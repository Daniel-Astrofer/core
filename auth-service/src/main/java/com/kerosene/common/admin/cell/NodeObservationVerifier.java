package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.databind.JsonNode;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Consumes Contracts' release-observations/v1; wrapper votes are not Bank votes. */
final class NodeObservationVerifier {
    private final CellOperationsProperties config;
    NodeObservationVerifier(CellOperationsProperties config) { this.config = config; }

    static byte[] canonical(JsonNode node) throws Exception {
        return com.kerosene.common.release.ReleaseCanonicalJson.bytes(node);
    }
    private static void fields(JsonNode node, String... fields) {
        var expected = Set.of(fields);
        if (!node.isObject() || node.size() != expected.size() || !node.properties().stream().allMatch(e -> expected.contains(e.getKey()))) throw new IllegalArgumentException("contract fields");
    }
    private static String text(JsonNode value, String field, String pattern) {
        JsonNode node = value.path(field);
        if (!node.isTextual() || !node.asText().matches(pattern)) throw new IllegalArgumentException("invalid " + field);
        return node.asText();
    }
    private static void lifetime(JsonNode value, Instant now) {
        Instant issue = Instant.parse(value.path("issuedAt").asText());
        Instant expiry = Instant.parse(value.path("expiresAt").asText());
        if (issue.isAfter(now.plusSeconds(5)) || issue.isBefore(now.minusSeconds(60)) || !expiry.isAfter(now) || !expiry.isAfter(issue) || expiry.isAfter(issue.plusSeconds(60))) throw new IllegalArgumentException("stale signed read");
    }
    private static void signed(JsonNode value, Map<String, String> trusted, String requiredMember) throws Exception {
        JsonNode signatures = value.path("signatures");
        if (!signatures.isArray() || signatures.size() != 1) throw new IllegalArgumentException("one independently pinned signer required");
        JsonNode signature = signatures.get(0);
        fields(signature, "memberId", "publicKeyDerBase64", "signatureBase64");
        String member = signature.path("memberId").asText();
        String key = trusted.get(member);
        if (key == null || (requiredMember != null && !requiredMember.equals(member)) || !key.equals(signature.path("publicKeyDerBase64").asText())) throw new IllegalArgumentException("untrusted signer");
        var payload = value.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) payload).remove("signatures");
        var publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
        if (!key.equals(Base64.getEncoder().encodeToString(publicKey.getEncoded()))) throw new IllegalArgumentException("noncanonical pinned key");
        var verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(publicKey);
        verifier.update(canonical(payload));
        if (!verifier.verify(Base64.getDecoder().decode(signature.path("signatureBase64").asText()))) throw new IllegalArgumentException("signature mismatch");
    }
    Map<String, Object> verify(JsonNode wrapper, Instant now) throws Exception {
        fields(wrapper, "schema", "observations");
        JsonNode reads = wrapper.path("observations");
        if (!reads.isArray() || reads.isEmpty() || reads.size() > 64 || !config.targetReleaseDigest.matches("sha256:[0-9a-f]{64}")) throw new IllegalArgumentException("bounded observations and configured target required");
        var votes = new ArrayList<Map<String, Object>>(); var seen = new HashSet<String>(); var distinctKeys = new HashSet<String>();
        String releaseId = null; long sequence = -1; String expiry = null;
        int compatible = 0;
        for (JsonNode read : reads) {
            fields(read, "schema", "releaseId", "networkId", "targetSequence", "releaseLockCanonicalDigest", "issuedAt", "expiresAt", "observation", "bankRead", "commitCertificate", "signatures");
            if (!"kerosene.signed-release-observation/v1".equals(read.path("schema").asText()) || !"unavailable".equals(read.path("commitCertificate").asText())) throw new IllegalArgumentException("unsupported observation contract");
            signed(read, config.signerKeys, null); lifetime(read, now);
            String id = text(read, "releaseId", "[a-z0-9][a-z0-9._-]{2,127}");
            long target = read.path("targetSequence").asLong(-1);
            if (!read.path("targetSequence").isIntegralNumber() || target < 1 || target > 9007199254740991L || !config.networkId.equals(read.path("networkId").asText()) || !config.targetReleaseDigest.equals(read.path("releaseLockCanonicalDigest").asText()) || (releaseId != null && (!releaseId.equals(id) || sequence != target))) throw new IllegalArgumentException("mixed release domain");
            releaseId = id; sequence = target;
            Instant readExpiry = Instant.parse(read.path("expiresAt").asText());
            JsonNode bank = read.path("bankRead");
            fields(bank, "schema", "releaseId", "networkId", "targetSequence", "releaseLockCanonicalDigest", "issuedAt", "expiresAt", "challenge", "source", "observation", "signatures");
            if (!"kerosene.bank-release-read/v1".equals(bank.path("schema").asText()) || !"bank-runtime".equals(bank.path("source").asText()) || !bank.path("observation").equals(read.path("observation"))) throw new IllegalArgumentException("synthetic or mismatched Bank read");
            text(bank, "challenge", "[0-9a-f]{64}");
            for (String field : List.of("releaseId", "networkId", "targetSequence", "releaseLockCanonicalDigest")) if (!bank.path(field).equals(read.path(field))) throw new IllegalArgumentException("Bank target mismatch");
            lifetime(bank, now);
            Instant bankExpiry = Instant.parse(bank.path("expiresAt").asText());
            Instant effectiveExpiry = readExpiry.isBefore(bankExpiry) ? readExpiry : bankExpiry;
            if (expiry == null || effectiveExpiry.isBefore(Instant.parse(expiry))) expiry = effectiveExpiry.toString();
            JsonNode observation = bank.path("observation");
            fields(observation, "observerId", "status", "observedSequence", "releaseDigest", "observedAt");
            String member = text(observation, "observerId", "[a-z0-9][a-z0-9._-]{2,127}");
            signed(bank, config.bankKeys, member);
            if (config.signerKeys.containsValue(config.bankKeys.get(member))) throw new IllegalArgumentException("Bank and Node signing identities must be independent");
            if (!config.observers.contains(member) || !seen.add(member) || !distinctKeys.add(config.bankKeys.get(member))) throw new IllegalArgumentException("unknown/duplicate Bank vote identity");
            Instant observed = Instant.parse(observation.path("observedAt").asText());
            Instant issued = Instant.parse(bank.path("issuedAt").asText());
            if (observed.isAfter(issued.plusSeconds(5)) || observed.isBefore(issued.minusSeconds(900))) throw new IllegalArgumentException("observation age");
            String status = observation.path("status").asText();
            if (!List.of("compatible", "incompatible", "unknown").contains(status)) throw new IllegalArgumentException("unknown status");
            if (!observation.path("observedSequence").isIntegralNumber() || !observation.path("observedSequence").canConvertToLong()
                    || observation.path("observedSequence").longValue() < 0 || observation.path("observedSequence").longValue() > 9007199254740991L)
                throw new IllegalArgumentException("invalid observed sequence");
            text(observation, "releaseDigest", "sha256:[0-9a-f]{64}");
            boolean accepted = "compatible".equals(status) && observation.path("observedSequence").isIntegralNumber() && observation.path("observedSequence").asLong(-1) == sequence && config.targetReleaseDigest.equals(observation.path("releaseDigest").asText());
            if (accepted) compatible++;
            votes.add(Map.of("observerId", member, "status", status, "observedAt", observed.toString(), "fresh", true, "accepted", accepted, "observedSequence", observation.path("observedSequence").asLong(-1), "releaseDigest", observation.path("releaseDigest").asText()));
        }
        var blockers = new ArrayList<>(List.of("CURRENT_RELEASE_MISSING", "ORDERED_CONSENSUS_EVIDENCE_MISSING", "RESTORE_EVIDENCE_MISSING", "UPDATE_HISTORY_MISSING", "PACKAGE_OR_DEPLOYMENT_MANIFEST_BINDING_MISSING"));
        if (config.minimumVotes < 1 || compatible < config.minimumVotes) blockers.add("QUORUM_INSUFFICIENT");
        var result = new LinkedHashMap<String, Object>();
        result.put("verification", "VERIFIED"); result.put("fresh", true); result.put("ready", false);
        result.put("targetRelease", Map.of("releaseId", releaseId, "sequence", sequence, "digest", config.targetReleaseDigest)); result.put("currentRelease", Map.of());
        result.put("quorum", Map.of("requiredVotes", config.minimumVotes, "compatibleVotes", compatible, "votes", votes, "commitCertificate", "unavailable"));
        result.put("backups", List.of()); result.put("update", Map.of("phase", "UNKNOWN", "history", List.of()));
        result.put("blockers", blockers); result.put("issuedAt", now.toString()); result.put("expiresAt", expiry);
        result.put("evidenceDigest", "sha256:" + java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(canonical(wrapper))));
        return result;
    }
}
