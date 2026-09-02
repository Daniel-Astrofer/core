package com.kerosene.common.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.kerosene.common.dto.ApiResponse;
import com.kerosene.common.financial.StompUserPublishRequest;
import com.kerosene.common.service.StompUserRelayService;

import java.util.Map;

/**
 * KFE standalone → Core STOMP bridge.
 *
 * <p>The shared internal filter authenticates the KFE workload before dispatch.
 */
@RestController
@RequestMapping("/internal/kfe/stomp")
public class KfeInternalStompRelayController {

    private final StompUserRelayService relayService;

    public KfeInternalStompRelayController(StompUserRelayService relayService) {
        this.relayService = relayService;
    }

    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publish(
            @RequestBody StompUserPublishRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.destination() != null && !request.destination().isBlank(), "destination is required");
        require(request.payload() != null && !request.payload().isEmpty(), "payload is required");

        try {
            relayService.publishToUser(request.userId(), request.destination(), request.payload());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(
                "STOMP user publish accepted.",
                Map.of(
                        "userId", request.userId(),
                        "destination", StompUserRelayService.normalizeDestination(request.destination()))));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
