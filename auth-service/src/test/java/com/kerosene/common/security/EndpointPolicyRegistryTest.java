package com.kerosene.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class EndpointPolicyRegistryTest {

    private static final List<Class<? extends Annotation>> METHOD_MAPPING_ANNOTATIONS = List.of(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class);

    private final EndpointPolicyRegistry registry = new EndpointPolicyRegistry();

    @Test
    void validatesDeclaredPolicyPatterns() {
        registry.validate();
    }

    @Test
    void deniesUndeclaredEndpointsByDefault() {
        assertTrue(registry.policyFor("/internal/new-unreviewed-endpoint").isEmpty());
    }
    @Test
    void bankReleaseReadIsCertificateOnlyNotAPublicOrJwtEndpoint() {
        assertEquals(EndpointPolicyRegistry.Policy.CERTIFICATE_ONLY,
                registry.policyFor("/v1/releases/observation").orElseThrow());
        assertEquals(EndpointPolicyRegistry.Policy.CERTIFICATE_ONLY,
                registry.policyFor("/v1/releases/observation/nested").orElseThrow());
        assertTrue(Arrays.stream(registry.publicEndpoints()).noneMatch(pattern -> pattern.contains("/v1/releases/observation")));
        assertTrue(Arrays.stream(registry.authenticatedEndpoints()).noneMatch(pattern -> pattern.contains("/v1/releases/observation")));
        assertTrue(Arrays.stream(registry.adminEndpoints()).noneMatch(pattern -> pattern.contains("/v1/releases/observation")));
    }

    @Test
    void kfeWalletEndpointsArePublic() {
        // KFE endpoints are public because the auth server proxies them
        // to kfe-service with X-KFE-Internal-Secret. JWT validation is
        // handled by the KfeGatewayController or kfe-service itself.
        assertEquals(
                EndpointPolicyRegistry.Policy.PUBLIC,
                registry.policyFor("/kfe/wallets").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.PUBLIC,
                registry.policyFor("/kfe/users/alice/receiving-capabilities").orElseThrow());
    }

    @Test
    void kfeAdminEndpointsRequireAdminPolicy() {
        assertEquals(
                EndpointPolicyRegistry.Policy.ADMIN,
                registry.policyFor("/api/admin/kfe/audit/latest").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.ADMIN,
                registry.policyFor("/api/admin/kfe/reserves/overview").orElseThrow());
    }

    @Test
    void legacyFinancialEndpointsAreNotDeclared() {
        assertTrue(registry.policyFor("/wallet/create").isEmpty());
        assertTrue(registry.policyFor("/v1/audit/config").isEmpty());
        assertTrue(registry.policyFor("/v1/audit/reserves/current").isEmpty());
        assertTrue(registry.policyFor("/v1/audit/siphon").isEmpty());
        assertTrue(registry.policyFor("/v1/audit/stats").isEmpty());
    }

    @Test
    void basePathControllerEndpointsHaveExplicitPolicies() {
        assertEquals(
                EndpointPolicyRegistry.Policy.AUTHENTICATED,
                registry.policyFor("/notifications").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.AUTHENTICATED,
                registry.policyFor("/auth/totp").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.ADMIN,
                registry.policyFor("/auth/admin/devices").orElseThrow());
    }

    @Test
    void deviceKeyLoginAndOnboardingEntrypointsArePublicButManagementRequiresAuthentication() {
        assertEquals(
                EndpointPolicyRegistry.Policy.PUBLIC,
                registry.policyFor("/auth/device-key/challenge").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.PUBLIC,
                registry.policyFor("/auth/device-key/onboarding/start").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.PUBLIC,
                registry.policyFor("/auth/device-key/onboarding/finish").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.PUBLIC,
                registry.policyFor("/auth/device-key/verify").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.AUTHENTICATED,
                registry.policyFor("/auth/device-key/register/start").orElseThrow());
        assertEquals(
                EndpointPolicyRegistry.Policy.AUTHENTICATED,
                registry.policyFor("/auth/device-key/devices").orElseThrow());
    }

    @Test
    void everyControllerEndpointHasDeclaredPolicy() throws Exception {
        Set<String> missingPolicies = new TreeSet<>();

        for (Class<?> controller : restControllers()) {
            List<String> basePaths = classMappingPaths(controller);
            for (Method method : controller.getDeclaredMethods()) {
                List<String> methodPaths = mappingPaths(method);
                if (methodPaths.isEmpty()) {
                    continue;
                }
                for (String basePath : basePaths) {
                    for (String methodPath : methodPaths) {
                        String endpoint = combine(basePath, methodPath);
                        if (!registry.hasDeclaredPolicy(endpoint)) {
                            missingPolicies.add(controller.getName() + "#" + method.getName() + " -> " + endpoint);
                        }
                    }
                }
            }
        }

        assertEquals(Set.of(), missingPolicies);
    }

    private List<Class<?>> restControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> controllers = new ArrayList<>();
        for (var beanDefinition : scanner.findCandidateComponents("com.kerosene")) {
            controllers.add(Class.forName(beanDefinition.getBeanClassName()));
        }
        return controllers;
    }

    private List<String> mappingPaths(Method method) {
        return METHOD_MAPPING_ANNOTATIONS.stream()
                .flatMap(annotationType -> annotationPaths(method, annotationType).stream())
                .distinct()
                .toList();
    }

    private List<String> classMappingPaths(Class<?> controller) {
        List<String> paths = annotationPaths(controller, RequestMapping.class);
        return paths.isEmpty() ? List.of("") : paths;
    }

    private List<String> annotationPaths(
            AnnotatedElement element,
            Class<? extends Annotation> annotationType) {
        MergedAnnotation<? extends Annotation> annotation = MergedAnnotations.from(element).get(annotationType);
        if (!annotation.isPresent()) {
            return List.of();
        }

        List<String> paths = new ArrayList<>();
        paths.addAll(Arrays.asList(annotation.getStringArray("value")));
        paths.addAll(Arrays.asList(annotation.getStringArray("path")));
        List<String> normalized = paths.stream()
                .filter(path -> path != null && !path.isBlank())
                .distinct()
                .toList();
        return normalized.isEmpty() ? List.of("") : normalized;
    }

    private String combine(String basePath, String methodPath) {
        String normalizedBase = normalize(basePath);
        String normalizedMethod = normalize(methodPath);
        if ("/".equals(normalizedBase)) {
            return normalizedMethod;
        }
        if ("/".equals(normalizedMethod)) {
            return normalizedBase;
        }
        return normalizedBase + normalizedMethod;
    }

    private String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
