package com.kerosene.common.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseManifestServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void optionalAttestationDoesNotClaimAbsentSignatureValid() throws Exception {
        Material material = signedManifest("abc123", "sha256:img", "sha256:code", "sha256:cfg");
        var service = new ReleaseManifestService(objectMapper, new MockEnvironment(), "kerosene-backend", false,
                material.manifest().toString(), "", "", "abc123", "time", "sha256:img", "sha256:code", "sha256:cfg");
        assertFalse(service.snapshot().manifestSignatureValid());
        assertFalse(service.snapshot().authorized());
    }

    @Test
    void unknownRuntimeCannotMatchSignedExpectedValue() throws Exception {
        Material material = signedManifest("abc123", "sha256:img", "sha256:code", "sha256:cfg");
        assertFalse(service(material, true, "unknown", "sha256:img", "sha256:code", "sha256:cfg").snapshot().authorized());
    }

    @Test
    void authorizesRuntimeWhenSignedManifestMatches() throws Exception {
        Material material = signedManifest("abc123", "sha256:img", "sha256:code", "sha256:cfg");

        ReleaseManifestService service = service(material, true, "abc123", "sha256:img", "sha256:code", "sha256:cfg");

        assertTrue(service.snapshot().manifestSignatureValid());
        assertTrue(service.snapshot().authorized());
    }

    @Test
    void rejectsRuntimeWhenImageDigestDiffersFromManifest() throws Exception {
        Material material = signedManifest("abc123", "sha256:img", "sha256:code", "sha256:cfg");

        ReleaseManifestService service = service(material, true, "abc123", "sha256:other", "sha256:code", "sha256:cfg");

        assertTrue(service.snapshot().manifestSignatureValid());
        assertFalse(service.snapshot().authorized());
    }

    private ReleaseManifestService service(
            Material material,
            boolean required,
            String commit,
            String imageDigest,
            String codeHash,
            String configHash) {
        return new ReleaseManifestService(
                objectMapper,
                new MockEnvironment(),
                "kerosene-backend",
                required,
                material.manifest().toString(),
                material.signature().toString(),
                material.publicKey().toString(),
                commit,
                "2026-04-29T00:00:00Z",
                imageDigest,
                codeHash,
                configHash);
    }

    private Material signedManifest(String commit, String imageDigest, String codeHash, String configHash) throws Exception {
        var keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Path manifest = tempDir.resolve("release-manifest.json");
        Path signature = tempDir.resolve("release-manifest.json.sig");
        Path publicKey = tempDir.resolve("release-public-key.der.b64");

        String json = """
                {
                  "schema": "kerosene.release/v1",
                  "version": "test",
                  "services": {
                    "kerosene-backend": {
                      "gitCommit": "%s",
                      "imageDigest": "%s",
                      "codeHash": "%s",
                      "configHash": "%s"
                    }
                  }
                }
                """.formatted(commit, imageDigest, codeHash, configHash);
        byte[] manifestBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(manifest, json);

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(manifestBytes);
        Files.writeString(signature, Base64.getEncoder().encodeToString(signer.sign()));
        Files.writeString(publicKey, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        return new Material(manifest, signature, publicKey);
    }

    private record Material(Path manifest, Path signature, Path publicKey) {
    }
}
