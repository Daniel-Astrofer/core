package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellEvidenceVerifierTest {
    final ObjectMapper mapper = new ObjectMapper();
    final Instant now = Instant.parse("2026-10-01T12:00:00Z");
    final String digest = "sha256:" + "a".repeat(64);
    CellOperationsProperties config;
    KeyPair keys;
    ObjectNode report;
    @BeforeEach void setup() throws Exception {
        keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        config = new CellOperationsProperties(); config.cellId = "cell-test"; config.networkId = "testnet";
        config.observers = List.of("bank-one", "bank-two"); config.minimumVotes = 2;
        config.signerKeys = Map.of("bank-key", Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));
        report = mapper.createObjectNode(); report.put("schema", "kerosene.bank-observer-report/v2");
        report.put("cellId", config.cellId); report.put("networkId", config.networkId); report.put("releaseId", "release-new");
        report.put("targetSequence", 2); report.put("releaseLockCanonicalDigest", digest);
        report.put("issuedAt", now.minusSeconds(5).toString()); report.put("expiresAt", now.plusSeconds(60).toString());
        report.putObject("currentRelease").put("releaseId", "release-old").put("sequence", 1).put("digest", digest);
        report.putObject("targetRelease").put("releaseId", "release-new").put("sequence", 2).put("digest", digest)
                .put("deploymentManifestDigest", digest).put("packageManifestDigest", digest);
        var observations = report.putArray("observations");
        for (String observer : config.observers) observations.addObject().put("observerId", observer).put("status", "compatible")
                .put("observedSequence", 2).put("releaseDigest", digest).put("observedAt", now.minusSeconds(2).toString());
        var backup = report.putArray("backups").addObject().put("backupId", "backup-one").put("releaseDigest", digest)
                .put("objectDigest", digest).put("createdAt", now.minusSeconds(10).toString());
        backup.putObject("restoreEvidence").put("status", "PASSED").put("objectDigest", digest).put("verifiedAt", now.minusSeconds(1).toString());
        report.putObject("update").put("phase", "IDLE").putArray("history");
    }
    byte[] signed() throws Exception {
        var operations = mapper.createObjectNode().put("schema", "kerosene.bank-cell-observations/v1").put("cellId", config.cellId);
        for (String field : List.of("currentRelease", "targetRelease", "backups", "update")) if (report.has(field)) operations.set(field, report.get(field));
        ObjectNode observerReport = report.deepCopy();
        observerReport.remove(List.of("cellId", "currentRelease", "targetRelease", "backups", "update"));
        observerReport.putArray("signatures").addObject().put("keyId", "bank-key");
        operations.set("observerReport", observerReport);
        byte[] bytes = mapper.writeValueAsBytes(operations);
        Signature signature = Signature.getInstance("Ed25519"); signature.initSign(keys.getPrivate()); signature.update(bytes);
        var envelope = mapper.createObjectNode().put("schema", CellEvidenceVerifier.ENVELOPE)
                .put("payloadBase64", Base64.getEncoder().encodeToString(bytes));
        envelope.putArray("signatures").addObject().put("keyId", "bank-key").put("algorithm", "Ed25519")
                .put("signatureBase64", Base64.getEncoder().encodeToString(signature.sign()));
        return mapper.writeValueAsBytes(envelope);
    }
    Map<String, Object> verify() throws Exception { return new CellEvidenceVerifier(config).verify(signed(), now); }
    @Test void legacySignedEnvelopeNeverAuthorizesReadiness() throws Exception {
        assertEquals(false, verify().get("ready"));
        assertTrue(((List<?>) verify().get("blockers")).contains("LEGACY_NON_NORMATIVE_OPERATIONAL_ENVELOPE"));
    }
    @Test void expiresAndFutureObservationsDoNotVote() throws Exception {
        report.put("expiresAt", now.toString()); assertEquals(false, verify().get("ready"));
        report.put("expiresAt", now.plusSeconds(10).toString());
        ((ObjectNode) report.path("observations").get(0)).put("observedAt", now.plusSeconds(1).toString());
        assertEquals(false, verify().get("ready"));
    }
    @Test void duplicateAndWrongDigestDoNotCreateQuorum() throws Exception {
        ((ObjectNode) report.path("observations").get(1)).put("observerId", "bank-one"); assertEquals(false, verify().get("ready"));
        ((ObjectNode) report.path("observations").get(1)).put("observerId", "bank-two").put("releaseDigest", "sha256:" + "b".repeat(64));
        assertEquals(false, verify().get("ready"));
    }
    @Test void missingAndMismatchedRestoreEvidenceBlocks() throws Exception {
        ((ObjectNode) report.path("backups").get(0).path("restoreEvidence")).put("objectDigest", "sha256:" + "b".repeat(64));
        assertEquals(false, verify().get("ready")); report.remove("backups"); assertEquals(false, verify().get("ready"));
    }
    @Test void untrustedKeyAndTamperingAreRejected() throws Exception {
        byte[] signed = signed(); config.signerKeys = Map.of();
        assertThrows(Exception.class, () -> new CellEvidenceVerifier(config).verify(signed, now));
        setup(); var envelope = (ObjectNode) mapper.readTree(signed()); envelope.put("payloadBase64", Base64.getEncoder().encodeToString("{}".getBytes()));
        assertThrows(Exception.class, () -> new CellEvidenceVerifier(config).verify(mapper.writeValueAsBytes(envelope), now));
    }
    @Test void wrongNetworkAndUnknownVersionReject() throws Exception {
        report.put("networkId", "mainnet"); assertThrows(Exception.class, this::verify);
        report.put("networkId", config.networkId).put("schema", "v3"); assertThrows(Exception.class, this::verify);
    }
    @Test void repeatedSignaturesCannotMeetThreshold() throws Exception {
        config.minimumSignatures = 2; var envelope = (ObjectNode) mapper.readTree(signed());
        ((com.fasterxml.jackson.databind.node.ArrayNode) envelope.path("signatures")).add(envelope.path("signatures").get(0).deepCopy());
        assertThrows(Exception.class, () -> new CellEvidenceVerifier(config).verify(mapper.writeValueAsBytes(envelope), now));
    }
    @Test void staleBackupAndInProgressUpdateBlock() throws Exception {
        ((ObjectNode) report.path("backups").get(0)).put("createdAt", now.minusSeconds(86401).toString());
        assertEquals(false, verify().get("ready")); setup(); ((ObjectNode) report.path("update")).put("phase", "APPLYING");
        assertEquals(false, verify().get("ready"));
    }
}
