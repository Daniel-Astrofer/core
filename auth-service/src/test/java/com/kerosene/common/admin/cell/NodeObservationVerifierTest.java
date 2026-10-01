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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NodeObservationVerifierTest {
    final ObjectMapper json = new ObjectMapper();
    final Instant now = Instant.parse("2026-10-01T12:00:00Z");
    final String digest = "sha256:" + "a".repeat(64);
    KeyPair node, bank;
    CellOperationsProperties config;
    ObjectNode wrapper, read, bankRead;

    void sign(ObjectNode document, KeyPair key, String member) throws Exception {
        document.remove("signatures");
        var signing = Signature.getInstance("Ed25519"); signing.initSign(key.getPrivate());
        signing.update(NodeObservationVerifier.canonical(document));
        document.putArray("signatures").addObject().put("memberId", member)
                .put("publicKeyDerBase64", Base64.getEncoder().encodeToString(key.getPublic().getEncoded()))
                .put("signatureBase64", Base64.getEncoder().encodeToString(signing.sign()));
    }
    ObjectNode domain(String schema) {
        return json.createObjectNode().put("schema", schema).put("releaseId", "release-001")
                .put("networkId", "bank-main").put("targetSequence", 2).put("releaseLockCanonicalDigest", digest)
                .put("issuedAt", now.toString()).put("expiresAt", now.plusSeconds(30).toString());
    }
    void fixture() throws Exception {
        node = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        bank = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        config = new CellOperationsProperties(); config.networkId = "bank-main"; config.targetReleaseDigest = digest;
        config.observers = List.of("bank-one"); config.minimumVotes = 3;
        config.signerKeys = Map.of("node-one", Base64.getEncoder().encodeToString(node.getPublic().getEncoded()));
        config.bankKeys = Map.of("bank-one", Base64.getEncoder().encodeToString(bank.getPublic().getEncoded()));
        var observation = json.createObjectNode().put("observerId", "bank-one").put("status", "compatible")
                .put("observedSequence", 2).put("releaseDigest", digest).put("observedAt", now.toString());
        bankRead = domain("kerosene.bank-release-read/v1").put("source", "bank-runtime").put("challenge", "b".repeat(64));
        bankRead.set("observation", observation); sign(bankRead, bank, "bank-one");
        read = domain("kerosene.signed-release-observation/v1").put("commitCertificate", "unavailable");
        read.set("observation", observation); read.set("bankRead", bankRead); sign(read, node, "node-one");
        wrapper = json.createObjectNode().put("schema", "kerosene.release-observations/v1"); wrapper.putArray("observations").add(read);
    }
    Map<String, Object> verify() throws Exception { return new CellEvidenceVerifier(config).verify(json.writeValueAsBytes(wrapper), now); }
    @Test void normativeNodeWireVerifiesButNeverManufacturesDeploymentReadiness() throws Exception {
        fixture(); var result = verify(); assertEquals("VERIFIED", result.get("verification")); assertEquals(false, result.get("ready"));
        assertEquals(1, ((Map<?, ?>) result.get("quorum")).get("compatibleVotes"));
        assertTrue(((List<?>) result.get("blockers")).contains("ORDERED_CONSENSUS_EVIDENCE_MISSING"));
    }
    @Test void wrapperSignatureDoesNotReplaceIndependentBankSignature() throws Exception {
        fixture(); bankRead.put("challenge", "c".repeat(64)); sign(read, node, "node-one"); assertThrows(Exception.class, this::verify);
        fixture(); config.bankKeys = Map.of(); assertThrows(Exception.class, this::verify);
    }
    @Test void syntheticExpiredAndRepeatedBankReadsAreRejected() throws Exception {
        fixture(); bankRead.put("source", "synthetic"); sign(bankRead, bank, "bank-one"); sign(read, node, "node-one"); assertThrows(Exception.class, this::verify);
        fixture(); bankRead.put("expiresAt", now.toString()); sign(bankRead, bank, "bank-one"); sign(read, node, "node-one"); assertThrows(Exception.class, this::verify);
        fixture(); ((com.fasterxml.jackson.databind.node.ArrayNode) wrapper.path("observations")).add(read.deepCopy()); assertThrows(Exception.class, this::verify);
    }
    @Test void targetAndCanonicalizationAreStrict() throws Exception {
        fixture(); config.targetReleaseDigest = "sha256:" + "d".repeat(64); assertThrows(Exception.class, this::verify);
        assertThrows(Exception.class, () -> NodeObservationVerifier.canonical(json.readTree("{\"float\":1.5}")));
        assertThrows(Exception.class, () -> NodeObservationVerifier.canonical(json.readTree("{\"integer\":9007199254740992}")));
    }
    @Test void negativeObservationsRemainVisibleWithoutCountingAsVotes() throws Exception {
        for (String status : List.of("unknown", "incompatible")) {
            fixture();
            ((ObjectNode) bankRead.path("observation")).put("status", status).put("observedSequence", 0);
            sign(bankRead, bank, "bank-one"); sign(read, node, "node-one");
            var result = verify(); var quorum = (Map<?, ?>) result.get("quorum");
            assertEquals("VERIFIED", result.get("verification")); assertEquals(false, result.get("ready"));
            assertEquals(0, quorum.get("compatibleVotes"));
            assertEquals(status, ((Map<?, ?>) ((List<?>) quorum.get("votes")).getFirst()).get("status"));
        }
    }
}
