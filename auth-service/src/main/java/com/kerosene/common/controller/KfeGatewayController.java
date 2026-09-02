package com.kerosene.common.controller;

import com.kerosene.common.security.workload.InternalServiceRestTemplateFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;

/**
 * Proxies the public KFE API namespaces to the independently deployed KFE service.
 *
 * The auth server validates the JWT (Spring Security filter chain),
 * The internal HTTP client enforces either the local-only compatibility
 * credential or the production SPIFFE mTLS identity.
 */
@RestController
@RequestMapping({"/kfe", "/api/public/kfe", "/api/admin/kfe"})
public class KfeGatewayController {

    private static final Logger log = LoggerFactory.getLogger(KfeGatewayController.class);
    private static final String PUBLIC_KFE_HEALTH_PATH = "/kfe/health/ready";
    private static final String INTERNAL_KFE_HEALTH_PATH = "/health/ready";
    private final RestTemplate restTemplate;
    private final String kfeBaseUrl;
    private static final Set<String> FORWARDED_REQUEST_HEADERS = Set.of(
            "authorization",
            "content-type",
            "digest",
            "x-correlation-id",
            "x-request-id",
            "x-idempotency-key",
            "idempotency-key",
            "x-tx-hash",
            "x-device-hash");

    public KfeGatewayController(
            InternalServiceRestTemplateFactory restTemplateFactory,
            @Value("${kfe.internal.base-url:http://kfe-service:8080}") String kfeBaseUrl,
            @Value("${kfe.gateway.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${kfe.gateway.read-timeout-ms:30000}") int readTimeoutMs) {
        InternalServiceRestTemplateFactory.ConfiguredClient client = restTemplateFactory.create(
                kfeBaseUrl,
                "http://kfe-service:8080",
                connectTimeoutMs,
                readTimeoutMs);
        this.kfeBaseUrl = client.baseUrl();
        this.restTemplate = client.restTemplate();
    }

    KfeGatewayController(RestTemplate restTemplate, String kfeBaseUrl) {
        this.restTemplate = restTemplate;
        this.kfeBaseUrl = kfeBaseUrl;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetPath = PUBLIC_KFE_HEALTH_PATH.equals(path) ? INTERNAL_KFE_HEALTH_PATH : path;
        String targetUrl = kfeBaseUrl + targetPath + (query != null ? "?" + query : "");

        try {
            HttpMethod method = HttpMethod.valueOf(request.getMethod());

            HttpHeaders forwardHeaders = new HttpHeaders();
            forwardHeaders.setContentType(MediaType.APPLICATION_JSON);

            // Copy client headers including Authorization (KFE validates JWT)
            for (String name : Collections.list(request.getHeaderNames())) {
                String lower = name.toLowerCase();
                if (!FORWARDED_REQUEST_HEADERS.contains(lower)) continue;
                String value = request.getHeader(name);
                if (value != null && !value.isEmpty()) {
                    forwardHeaders.set(name, value);
                }
            }

            byte[] body = request.getInputStream().readAllBytes();
            HttpEntity<byte[]> entity = new HttpEntity<>(body.length > 0 ? body : null, forwardHeaders);

            ResponseEntity<String> kfeResponse = restTemplate.exchange(
                    targetUrl, method, entity, String.class);

            ResponseEntity.BodyBuilder response = ResponseEntity.status(kfeResponse.getStatusCode());
            if (kfeResponse.getHeaders().getContentType() != null) {
                response.contentType(kfeResponse.getHeaders().getContentType());
            }
            copyResponseHeader(kfeResponse.getHeaders(), response, "X-Correlation-Id");
            copyResponseHeader(kfeResponse.getHeaders(), response, "X-Request-Id");
            return response.body(kfeResponse.getBody());

        } catch (Exception e) {
            boolean isTimeout = e instanceof org.springframework.web.client.ResourceAccessException
                    && e.getCause() instanceof SocketTimeoutException;
            HttpStatus status = isTimeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
            String errorCode = isTimeout ? "SYS_504" : "SYS_502";
            log.error("[KFE Gateway] {} {} failed ({}, {})",
                    request.getMethod(), path, errorCode, e.getClass().getSimpleName());
            return ResponseEntity.status(status)
                    .body("{\"success\":false,\"message\":\"KFE gateway error\",\"errorCode\":\"" + errorCode + "\"}");
        }
    }

    private static void copyResponseHeader(HttpHeaders source, ResponseEntity.BodyBuilder target, String name) {
        String value = source.getFirst(name);
        if (value != null && !value.isBlank()) {
            target.header(name, value);
        }
    }
}
