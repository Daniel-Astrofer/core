package com.kerosene.content.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.kerosene.common.dto.ApiResponse;
import com.kerosene.content.dto.HomeSurfaceResponseDTO;
import com.kerosene.content.dto.HomeUiPublishRequestDTO;
import com.kerosene.content.model.entity.HomeUiOverrideEntity;
import com.kerosene.content.service.HomeSurfaceComposer;
import com.kerosene.content.service.HomeUiOverrideService;
import com.kerosene.content.service.HomeUiPushService;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * Internal ops endpoint for publishing home UI overrides and live pushes.
 * The shared internal filter authenticates the calling KFE workload.
 */
@RestController
@RequestMapping("/internal/content/home-ui")
public class HomeUiPublishController {

    private final HomeUiOverrideService overrideService;
    private final HomeSurfaceComposer surfaceComposer;
    private final HomeUiPushService pushService;
    private final ObjectMapper objectMapper;

    public HomeUiPublishController(
            HomeUiOverrideService overrideService,
            HomeSurfaceComposer surfaceComposer,
            HomeUiPushService pushService,
            ObjectMapper objectMapper) {
        this.overrideService = overrideService;
        this.surfaceComposer = surfaceComposer;
        this.pushService = pushService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publish(
            @RequestBody HomeUiPublishRequestDTO request) {
        require(request != null, "request is required");
        String action = request.action() == null ? "" : request.action().trim().toUpperCase(Locale.ROOT);

        return switch (action) {
            case "UPSERT_OVERRIDE" -> upsertOverride(request);
            case "PUSH_SNAPSHOT" -> pushSnapshot(request);
            case "PUSH_PATCH" -> pushPatch(request);
            case "PUSH_GREETING" -> pushGreeting(request);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "action must be UPSERT_OVERRIDE | PUSH_SNAPSHOT | PUSH_PATCH | PUSH_GREETING");
        };
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> upsertOverride(HomeUiPublishRequestDTO request) {
        require(request.payloadJson() != null && !request.payloadJson().isBlank(), "payloadJson is required");
        String scope = request.scope() == null ? "GLOBAL" : request.scope().trim().toUpperCase(Locale.ROOT);
        if (!scope.equals("GLOBAL") && !scope.equals("USER") && !scope.equals("SEGMENT")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid scope");
        }
        if (scope.equals("USER")) {
            require(request.userId() != null, "userId required for USER scope");
        }
        if (scope.equals("SEGMENT")) {
            require(request.segmentKey() != null && !request.segmentKey().isBlank(), "segmentKey required");
        }
        try {
            JsonNode node = objectMapper.readTree(request.payloadJson());
            if (node == null || !node.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payloadJson must be a JSON object");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payloadJson is not valid JSON");
        }

        HomeUiOverrideEntity entity = new HomeUiOverrideEntity();
        entity.setScope(scope);
        entity.setUserId(request.userId());
        entity.setSegmentKey(request.segmentKey());
        entity.setPriority(request.priority() == null ? 0 : request.priority());
        entity.setActive(request.active() == null || request.active());
        entity.setStartsAt(parseInstant(request.startsAt()));
        entity.setEndsAt(parseInstant(request.endsAt()));
        entity.setPayload(request.payloadJson().trim());
        HomeUiOverrideEntity saved = overrideService.save(entity);

        if (request.userId() != null && (request.active() == null || request.active())) {
            HomeSurfaceResponseDTO surface = surfaceComposer.compose(
                    request.userId(),
                    firstNonBlank(request.balanceView(), "TOTAL"),
                    firstNonBlank(request.locale(), "pt"),
                    firstNonBlank(request.timeZone(), "UTC"));
            pushService.pushSnapshot(request.userId(), surface);
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Home UI override saved.",
                Map.of("id", saved.getId(), "scope", saved.getScope(), "priority", saved.getPriority())));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> pushSnapshot(HomeUiPublishRequestDTO request) {
        require(request.userId() != null, "userId is required");
        HomeSurfaceResponseDTO surface = surfaceComposer.compose(
                request.userId(),
                firstNonBlank(request.balanceView(), "TOTAL"),
                firstNonBlank(request.locale(), "pt"),
                firstNonBlank(request.timeZone(), "UTC"));
        pushService.pushSnapshot(request.userId(), surface);
        return ResponseEntity.ok(ApiResponse.success(
                "Home UI snapshot pushed.",
                Map.of("userId", request.userId(), "version", surface.version())));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> pushPatch(HomeUiPublishRequestDTO request) {
        require(request.userId() != null, "userId is required");
        require(request.payloadJson() != null && !request.payloadJson().isBlank(), "payloadJson is required");
        try {
            JsonNode patch = objectMapper.readTree(request.payloadJson());
            pushService.pushPatch(request.userId(), patch, Instant.now().toString());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payloadJson is not valid JSON");
        }
        return ResponseEntity.ok(ApiResponse.success(
                "Home UI patch pushed.",
                Map.of("userId", request.userId())));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> pushGreeting(HomeUiPublishRequestDTO request) {
        require(request.userId() != null, "userId is required");
        require(request.payloadJson() != null && !request.payloadJson().isBlank(), "payloadJson is required");
        try {
            JsonNode greeting = objectMapper.readTree(request.payloadJson());
            pushService.pushGreeting(request.userId(), greeting, Instant.now().toString());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payloadJson is not valid JSON");
        }
        return ResponseEntity.ok(ApiResponse.success(
                "Home UI greeting pushed.",
                Map.of("userId", request.userId())));
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instant: " + raw);
        }
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
