package com.kerosene.common.release.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kerosene.common.release.ReleaseCanonicalJson;
import com.kerosene.common.release.ReleaseManifestService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BankReleaseServiceTest {
    @TempDir Path temporary;
    final ObjectMapper json = new ObjectMapper();
    final Instant now = Instant.parse("2026-10-01T12:00:00Z");
    final ReleaseManifestService runtime = mock(ReleaseManifestService.class);
    final MockEnvironment environment = new MockEnvironment().withProperty("server.ssl.enabled", "true")
            .withProperty("server.ssl.client-auth", "need");
    BankReleaseProperties config;
    KeyPair bank;
    String digest;
    Path target;
    ObjectNode lock;
    BankReleaseService fixture() throws Exception {
        bank = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        config = new BankReleaseProperties(); config.enabled = true; config.networkId = "bank-main"; config.observerId = "bank-one";
        config.clientSpkiDigests = List.of("sha256:" + "b".repeat(64));
        Path directory = Files.createDirectory(temporary.resolve("targets")); config.targetDirectory = directory.toString();
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
        Path key = temporary.resolve("bank-key.der"); Files.write(key, bank.getPrivate().getEncoded());
        Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------")); config.privateKeyFile = key.toString();
        config.publicKeyDerBase64 = Base64.getEncoder().encodeToString(bank.getPublic().getEncoded());
        lock = json.createObjectNode().put("schema", "kerosene.release-lock/v3").put("schemaVersion", 3)
                .put("releaseId", "release-one").put("sequence", 7);
        lock.putObject("network").put("id", "bank-main").put("plane", "bank");
        lock.put("fixtureDescription", "Only a catalog identity, not approval or a complete deploy lock");
        digest = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ReleaseCanonicalJson.bytes(lock)));
        target = directory.resolve(digest.substring(7) + ".json"); Files.write(target, json.writeValueAsBytes(lock));
        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-r--r--"));
        when(runtime.snapshot()).thenReturn(snapshot(false, true));
        var service = new BankReleaseService(config, runtime, environment, Clock.fixed(now, ZoneOffset.UTC)); service.initialize();
        return service;
    }
    ReleaseManifestService.ReleaseSnapshot snapshot(boolean signature, boolean authorized) {
        return new ReleaseManifestService.ReleaseSnapshot("core", "unknown", "unknown", "unknown", "unknown", "unknown", "unknown",
                "absent", signature, authorized, "test-runtime", "test-runtime", Map.of(), Map.of());
    }
    void verify(ObjectNode read) throws Exception {
        var payload = read.deepCopy(); var signature = payload.remove("signatures").get(0);
        var verifier = Signature.getInstance("Ed25519"); verifier.initVerify(bank.getPublic()); verifier.update(ReleaseCanonicalJson.bytes(payload));
        assertTrue(verifier.verify(Base64.getDecoder().decode(signature.path("signatureBase64").asText())));
        assertEquals(config.publicKeyDerBase64, signature.path("publicKeyDerBase64").asText());
        assertEquals(config.observerId, signature.path("memberId").asText());
    }
    @Test void exactNormativeReadBindsLocalTargetFreshChallengeAndIndependentBankKey() throws Exception {
        var service = fixture(); var read = service.observe(digest, "c".repeat(64)); verify(read);
        assertEquals("kerosene.bank-release-read/v1", read.path("schema").asText());
        assertEquals("bank-runtime", read.path("source").asText());
        assertEquals("release-one", read.path("releaseId").asText()); assertEquals(7, read.path("targetSequence").asLong());
        assertEquals(digest, read.path("releaseLockCanonicalDigest").asText());
        assertEquals("c".repeat(64), read.path("challenge").asText());
        assertEquals(now.plusSeconds(30).toString(), read.path("expiresAt").asText());
        assertEquals("unknown", read.path("observation").path("status").asText());
        assertEquals(0, read.path("observation").path("observedSequence").asLong());
        var other = service.observe(digest, "d".repeat(64)); verify(other);
        assertNotEquals(read.path("signatures"), other.path("signatures"));
        assertFalse(read.has("commitCertificate"));
    }
    @Test void optionalOrVerifiedRuntimeDoesNotImplyWholeCellCompatibility() throws Exception {
        var service = fixture();
        for (var snapshot : List.of(snapshot(false, false), snapshot(false, true), snapshot(true, true))) {
            when(runtime.snapshot()).thenReturn(snapshot);
            assertEquals("unknown", service.observe(digest, "c".repeat(64)).path("observation").path("status").asText());
        }
        when(runtime.snapshot()).thenReturn(snapshot(true, false));
        var read = service.observe(digest, "c".repeat(64)); verify(read);
        assertEquals("incompatible", read.path("observation").path("status").asText());
    }
    @Test void unknownTargetsAndInvalidQueriesHaveNoSignedOutput() throws Exception {
        var service = fixture();
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.observe("sha256:" + "e".repeat(64), "c".repeat(64))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.observe("../../other", "c".repeat(64))).getStatusCode());
        assertThrows(ResponseStatusException.class, () -> service.observe(digest, "C".repeat(64)));
        var query = new LinkedMultiValueMap<String, String>(); query.add("releaseDigest", digest); query.add("challenge", "c".repeat(64));
        query.add("status", "compatible");
        assertThrows(ResponseStatusException.class, () -> new BankReleaseController(service).observation(query));
        query.remove("status"); query.add("challenge", "d".repeat(64));
        assertThrows(ResponseStatusException.class, () -> new BankReleaseController(service).observation(query));
    }
    @Test void tamperedDuplicateTrailingOversizedAndSymlinkTargetsFailClosed() throws Exception {
        var service = fixture();
        for (String invalid : List.of("{\"releaseId\":\"other\"}", "{\"sequence\":1,\"sequence\":2}",
                json.writeValueAsString(lock) + " {}", " ".repeat(262145))) {
            Files.writeString(target, invalid);
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                    () -> service.observe(digest, "c".repeat(64))).getStatusCode());
        }
        Files.delete(target); Path linked = temporary.resolve("link-target.json"); Files.write(linked, json.writeValueAsBytes(lock));
        Files.createSymbolicLink(target, linked);
        assertThrows(ResponseStatusException.class, () -> service.observe(digest, "c".repeat(64)));
    }
    @Test void unsafeConfigurationFailsStartupInsteadOfDowngradingAuthentication() throws Exception {
        fixture();
        environment.setProperty("server.ssl.client-auth", "want");
        assertThrows(IllegalStateException.class, () -> new BankReleaseService(config, runtime, environment).initialize());
        environment.setProperty("server.ssl.client-auth", "need");
        config.publicKeyDerBase64 = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        assertThrows(IllegalStateException.class, () -> new BankReleaseService(config, runtime, environment).initialize());
        config.publicKeyDerBase64 = Base64.getEncoder().encodeToString(bank.getPublic().getEncoded());
        config.publicKeyDerBase64 = config.publicKeyDerBase64.replace("=", "");
        assertThrows(IllegalStateException.class, () -> new BankReleaseService(config, runtime, environment).initialize());
        config.publicKeyDerBase64 = Base64.getEncoder().encodeToString(bank.getPublic().getEncoded());
        Files.setPosixFilePermissions(Path.of(config.privateKeyFile), PosixFilePermissions.fromString("rw-r--r--"));
        assertThrows(IllegalStateException.class, () -> new BankReleaseService(config, runtime, environment).initialize());
        config.enabled = false; var disabled = new BankReleaseService(config, runtime, environment); disabled.initialize();
        assertThrows(ResponseStatusException.class, () -> disabled.observe(digest, "c".repeat(64)));
    }
    @Test void actualSignedRuntimeMismatchProducesIncompatibilityNotCallerClaim() throws Exception {
        fixture();
        var releaseKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Path manifest = temporary.resolve("current-runtime.json");
        Files.writeString(manifest, "{\"version\":\"test\",\"services\":{\"core\":{\"gitCommit\":\"approved-current-commit\"}}}");
        var signing = Signature.getInstance("Ed25519"); signing.initSign(releaseKey.getPrivate()); signing.update(Files.readAllBytes(manifest));
        Path signature = temporary.resolve("current.sig"), publicKey = temporary.resolve("release-public.txt");
        Files.writeString(signature, Base64.getEncoder().encodeToString(signing.sign()));
        Files.writeString(publicKey, Base64.getEncoder().encodeToString(releaseKey.getPublic().getEncoded()));
        var actual = new ReleaseManifestService(json, environment, "core", false, manifest.toString(), signature.toString(),
                publicKey.toString(), "different-running-commit", "unknown", "unknown", "unknown", "unknown");
        assertTrue(actual.snapshot().manifestSignatureValid()); assertFalse(actual.snapshot().authorized());
        var service = new BankReleaseService(config, actual, environment, Clock.fixed(now, ZoneOffset.UTC)); service.initialize();
        var read = service.observe(digest, "c".repeat(64)); verify(read);
        assertEquals("incompatible", read.path("observation").path("status").asText());
    }
}
