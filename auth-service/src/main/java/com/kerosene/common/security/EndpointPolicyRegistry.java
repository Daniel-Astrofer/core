package com.kerosene.common.security;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class EndpointPolicyRegistry {

    public enum Policy {
        PUBLIC,
        ADMIN,
        AUTHENTICATED,
        CERTIFICATE_ONLY
    }

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    public static final String BANK_RELEASE_OBSERVATION = "/v1/releases/observation";
    private static final String[] CERTIFICATE_ONLY = { BANK_RELEASE_OBSERVATION, BANK_RELEASE_OBSERVATION + "/**" };

    private static final String[] AUTHENTICATED = {
            "/auth/activation-status",
            "/auth/activation-status/**",
            "/auth/backup-codes",
            "/auth/backup-codes/**",
            "/auth/device-key/**",
            "/auth/logout",
            "/auth/me",
            "/auth/passkey/**",
            "/auth/security-status",
            "/auth/security/**",
            "/auth/totp",
            "/auth/totp/**",
            "/api/economy/**",
            "/audit/**",
            "/health/dependencies",
            "/notifications",
            "/notifications/**",
            "/content",
            "/content/**",
            "/quorum/**",
            "/sovereignty/reattest",
            "/sovereignty/telemetry",
            "/users/*/receiving-capabilities"
    };

    private static final String[] ADMIN = {
            "/api/admin/**",
            "/auth/admin/access-attempts/**",
            "/auth/admin/devices",
            "/auth/admin/devices/**",
            "/auth/admin/key",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/configuration/security",
            "/configuration/ui",
            "/swagger-ui",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/webjars/**"
    };

    private static final String[] PUBLIC = {
            "/",
            "/bitcoin-banking",
            "/bitcoin-banking/**",
            "/admin",
            "/admin/**",
            "/download",
            "/status",
            "/index.html",
            "/favicon.png",
            "/manifest.json",
            "/version.json",
            "/flutter.js",
            "/flutter_bootstrap.js",
            "/flutter_service_worker.js",
            "/main.dart.js",
            "/assets/**",
            "/canvaskit/**",
            "/icons/**",
            "/healthz",
            "/health/live",
            "/health/ready",
            "/api/public/**",
            "/internal/kfe/**",
            "/internal/content/**",
            "/system/release",
            "/auth/signup",
            "/auth/signup/totp/verify",
            "/auth/login",
            "/auth/login/totp/verify",
            "/auth/admin/login",
            "/auth/admin/login/*",
            "/auth/passkey/challenge",
            "/auth/passkey/verify",
            "/auth/passkey/onboarding/start",
            "/auth/passkey/onboarding/finish",
            "/auth/device-key/challenge",
            "/auth/device-key/onboarding/start",
            "/auth/device-key/onboarding/finish",
            "/auth/device-key/verify",
            "/auth/recovery/emergency/start",
            "/auth/recovery/emergency/finish",
            "/auth/pow/challenge",
            "/integrations/btcpay/webhook/**",
            "/actuator/health",
            "/actuator/health/**",
            "/sovereignty/ping",
            "/sovereignty/status",
            "/kfe/**",
            "/error",
            "/ws/**"
    };

    @PostConstruct
    public void validate() {
        Map<String, Policy> seen = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        registerForValidation(seen, errors, Policy.PUBLIC, PUBLIC);
        registerForValidation(seen, errors, Policy.ADMIN, ADMIN);
        registerForValidation(seen, errors, Policy.AUTHENTICATED, AUTHENTICATED);
        registerForValidation(seen, errors, Policy.CERTIFICATE_ONLY, CERTIFICATE_ONLY);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid endpoint policy registry: " + String.join("; ", errors));
        }
    }

    public String[] authenticatedEndpoints() {
        return AUTHENTICATED.clone();
    }

    public String[] adminEndpoints() {
        return ADMIN.clone();
    }

    public String[] publicEndpoints() {
        return PUBLIC.clone();
    }

    public Optional<Policy> policyFor(String path) {
        String normalizedPath = normalize(path);
        for (String pattern : CERTIFICATE_ONLY) {
            if (PATH_MATCHER.match(pattern, normalizedPath)) return Optional.of(Policy.CERTIFICATE_ONLY);
        }
        for (String pattern : PUBLIC) {
            if (PATH_MATCHER.match(pattern, normalizedPath)) {
                return Optional.of(Policy.PUBLIC);
            }
        }
        for (String pattern : ADMIN) {
            if (PATH_MATCHER.match(pattern, normalizedPath)) {
                return Optional.of(Policy.ADMIN);
            }
        }
        for (String pattern : AUTHENTICATED) {
            if (PATH_MATCHER.match(pattern, normalizedPath)) {
                return Optional.of(Policy.AUTHENTICATED);
            }
        }
        return Optional.empty();
    }

    public boolean hasDeclaredPolicy(String path) {
        return policyFor(path).isPresent();
    }

    private void registerForValidation(
            Map<String, Policy> seen,
            List<String> errors,
            Policy policy,
            String[] patterns) {
        if (patterns.length == 0) {
            errors.add(policy + " policy has no endpoint patterns");
        }
        Arrays.stream(patterns).forEach(pattern -> validatePattern(seen, errors, policy, pattern));
    }

    private void validatePattern(Map<String, Policy> seen, List<String> errors, Policy policy, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            errors.add(policy + " policy contains a blank pattern");
            return;
        }
        if (!pattern.startsWith("/")) {
            errors.add(policy + " pattern must start with '/': " + pattern);
        }
        Policy previousPolicy = seen.putIfAbsent(pattern, policy);
        if (previousPolicy != null) {
            errors.add("duplicate pattern " + pattern + " in " + previousPolicy + " and " + policy);
        }
    }

    private String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
