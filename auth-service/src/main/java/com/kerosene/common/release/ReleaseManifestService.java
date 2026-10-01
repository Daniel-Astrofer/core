package com.kerosene.common.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReleaseManifestService {

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final String serviceName;
    private final boolean attestationRequired;
    private final String manifestPath;
    private final String manifestSignaturePath;
    private final String publicKeyPath;
    private final String gitCommit;
    private final String buildTime;
    private final String imageDigest;
    private final String codeHash;
    private final String configHash;

    private volatile ReleaseSnapshot cachedSnapshot;

    public ReleaseManifestService(
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${release.service-name:${spring.application.name:kerosene-backend}}") String serviceName,
            @Value("${release.attestation.required:false}") boolean attestationRequired,
            @Value("${release.manifest.path:}") String manifestPath,
            @Value("${release.manifest.signature-path:}") String manifestSignaturePath,
            @Value("${release.manifest.public-key-path:}") String publicKeyPath,
            @Value("${release.git-commit:${GIT_COMMIT:unknown}}") String gitCommit,
            @Value("${release.build-time:${BUILD_TIME:unknown}}") String buildTime,
            @Value("${release.image-digest:${IMAGE_DIGEST:unknown}}") String imageDigest,
            @Value("${release.code-hash:${CODE_HASH:unknown}}") String codeHash,
            @Value("${release.config-hash:${CONFIG_HASH:unknown}}") String configHash) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.serviceName = trimOrDefault(serviceName, "kerosene-backend");
        this.attestationRequired = attestationRequired;
        this.manifestPath = trim(manifestPath);
        this.manifestSignaturePath = trim(manifestSignaturePath);
        this.publicKeyPath = trim(publicKeyPath);
        this.gitCommit = trimOrDefault(gitCommit, "unknown");
        this.buildTime = trimOrDefault(buildTime, "unknown");
        this.imageDigest = trimOrDefault(imageDigest, "unknown");
        this.codeHash = trimOrDefault(codeHash, "unknown");
        this.configHash = trimOrDefault(configHash, "unknown");
    }

    @PostConstruct
    public void validateAtStartup() {
        ReleaseSnapshot snapshot = snapshot();
        if (attestationRequired && !snapshot.authorized()) {
            throw new IllegalStateException("Release attestation failed: " + snapshot.reason());
        }
    }

    public ReleaseSnapshot snapshot() {
        ReleaseSnapshot local = cachedSnapshot;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cachedSnapshot == null) {
                cachedSnapshot = loadSnapshot();
            }
            return cachedSnapshot;
        }
    }

    public String manifestDigest() {
        return snapshot().manifestDigest();
    }

    public boolean isAuthorized() {
        return snapshot().authorized();
    }

    private ReleaseSnapshot loadSnapshot() {
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("service", serviceName);
        runtime.put("gitCommit", gitCommit);
        runtime.put("buildTime", buildTime);
        runtime.put("imageDigest", imageDigest);
        runtime.put("codeHash", codeHash);
        runtime.put("configHash", configHash);
        runtime.put("profiles", String.join(",", environment.getActiveProfiles()));

        if (manifestPath.isBlank()) {
            boolean authorized = !attestationRequired;
            return new ReleaseSnapshot(
                    serviceName,
                    "UNCONFIGURED",
                    gitCommit,
                    buildTime,
                    imageDigest,
                    codeHash,
                    configHash,
                    "absent",
                    false,
                    authorized,
                    authorized ? "ATTESTATION_OPTIONAL" : "ATTESTATION_REQUIRED_BUT_MANIFEST_MISSING",
                    "release.manifest.path is not configured",
                    runtime,
                    Map.of());
        }

        try {
            Path manifest = Path.of(manifestPath);
            byte[] manifestBytes = Files.readAllBytes(manifest);
            String digest = "sha256:" + sha256Hex(manifestBytes);
            JsonNode root = objectMapper.readTree(manifestBytes);
            JsonNode services = root.path("services");
            JsonNode service = services.path(serviceName);
            if (service.isMissingNode()) {
                return failed(digest, "SERVICE_NOT_IN_MANIFEST", "Manifest does not authorize " + serviceName, runtime, root);
            }

            boolean signatureValid = verifySignature(manifestBytes);
            String expectedCommit = text(service, "gitCommit");
            String expectedDigest = text(service, "imageDigest");
            String expectedCodeHash = text(service, "codeHash");
            String expectedConfigHash = text(service, "configHash");

            String mismatch = firstMismatch(
                    expectedCommit, gitCommit, "gitCommit",
                    expectedDigest, imageDigest, "imageDigest",
                    expectedCodeHash, codeHash, "codeHash",
                    expectedConfigHash, configHash, "configHash");
            boolean authorized = signatureValid && mismatch == null;
            String reason = authorized ? "AUTHORIZED" : (signatureValid ? mismatch : "INVALID_MANIFEST_SIGNATURE");

            return new ReleaseSnapshot(
                    serviceName,
                    root.path("version").asText("unknown"),
                    gitCommit,
                    buildTime,
                    imageDigest,
                    codeHash,
                    configHash,
                    digest,
                    signatureValid,
                    authorized,
                    reason,
                    authorized ? "Runtime release matches signed manifest" : "Runtime release does not match signed manifest",
                    runtime,
                    objectMapper.convertValue(service, Map.class));
        } catch (Exception exception) {
            boolean authorized = !attestationRequired;
            return new ReleaseSnapshot(
                    serviceName,
                    "ERROR",
                    gitCommit,
                    buildTime,
                    imageDigest,
                    codeHash,
                    configHash,
                    "unreadable",
                    false,
                    authorized,
                    authorized ? "ATTESTATION_OPTIONAL_MANIFEST_ERROR" : "MANIFEST_ERROR",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    runtime,
                    Map.of());
        }
    }

    private ReleaseSnapshot failed(String digest, String reason, String message, Map<String, Object> runtime, JsonNode root) {
        return new ReleaseSnapshot(
                serviceName,
                root.path("version").asText("unknown"),
                gitCommit,
                buildTime,
                imageDigest,
                codeHash,
                configHash,
                digest,
                false,
                false,
                reason,
                message,
                runtime,
                Map.of());
    }

    private boolean verifySignature(byte[] manifestBytes) throws Exception {
        if (manifestSignaturePath.isBlank() || publicKeyPath.isBlank()) {
            return false;
        }
        byte[] signatureBytes = Base64.getMimeDecoder().decode(Files.readString(Path.of(manifestSignaturePath)).trim());
        byte[] publicKeyBytes = Base64.getMimeDecoder().decode(Files.readString(Path.of(publicKeyPath)).trim());
        PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(publicKey);
        verifier.update(manifestBytes);
        return verifier.verify(signatureBytes);
    }

    private String firstMismatch(String... values) {
        for (int i = 0; i + 2 < values.length; i += 3) {
            String expected = values[i];
            String actual = values[i + 1];
            String field = values[i + 2];
            if (expected != null && !expected.isBlank() && !expected.equals(actual)) {
                return "MISMATCH_" + field;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String sha256Hex(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimOrDefault(String value, String fallback) {
        String trimmed = trim(value);
        return trimmed.isBlank() ? fallback : trimmed;
    }

    public record ReleaseSnapshot(
            String service,
            String version,
            String gitCommit,
            String buildTime,
            String imageDigest,
            String codeHash,
            String configHash,
            String manifestDigest,
            boolean manifestSignatureValid,
            boolean authorized,
            String reason,
            String message,
            Map<String, Object> runtime,
            Map<String, Object> manifestService) {
        public Instant checkedAt() {
            return Instant.now();
        }
    }
}
