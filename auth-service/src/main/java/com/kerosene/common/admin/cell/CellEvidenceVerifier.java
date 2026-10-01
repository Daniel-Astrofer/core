package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CellEvidenceVerifier {
    public static final String ENVELOPE = "kerosene.bank-release-observations/v1";
    private final ObjectReader mapper = com.kerosene.config.ReleaseJsonConfig.reader();
    private final CellOperationsProperties config;
    public CellEvidenceVerifier(CellOperationsProperties config) { this.config = config; }

    public Map<String, Object> verify(byte[] envelopeBytes, Instant now) throws Exception {
        if (envelopeBytes.length > BankObservationClient.MAX_BYTES) throw new IllegalArgumentException("oversize");
        JsonNode envelope = mapper.readTree(envelopeBytes);
        if ("kerosene.release-observations/v1".equals(envelope.path("schema").asText())) {
            return new NodeObservationVerifier(config).verify(envelope, now);
        }
        if (!ENVELOPE.equals(envelope.path("schema").asText())) throw new IllegalArgumentException("envelope version");
        byte[] payload = Base64.getDecoder().decode(envelope.path("payloadBase64").asText());
        var trusted = new HashSet<String>();
        var distinctKeys = new HashSet<String>();
        if (!envelope.path("signatures").isArray()) throw new IllegalArgumentException("signatures missing");
        for (JsonNode signature : envelope.path("signatures")) {
            String id = signature.path("keyId").asText();
            String key = config.signerKeys.get(id);
            if (key == null || !"Ed25519".equals(signature.path("algorithm").asText())) continue;
            var verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(KeyFactory.getInstance("Ed25519").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(key))));
            verifier.update(payload);
            if (verifier.verify(Base64.getDecoder().decode(signature.path("signatureBase64").asText()))) {
                trusted.add(id); distinctKeys.add(key);
            }
        }
        if (config.minimumSignatures < 1 || distinctKeys.size() < config.minimumSignatures) {
            throw new IllegalArgumentException("signature threshold");
        }
        JsonNode operations = mapper.readTree(payload);
        JsonNode report = operations.path("observerReport");
        if (!"kerosene.bank-cell-observations/v1".equals(operations.path("schema").asText())) throw new IllegalArgumentException("payload version");
        if (!"kerosene.bank-observer-report/v2".equals(report.path("schema").asText())
                || config.cellId.isBlank() || config.networkId.isBlank()
                || !config.cellId.equals(operations.path("cellId").asText())
                || !config.networkId.equals(report.path("networkId").asText())) {
            throw new IllegalArgumentException("report identity");
        }
        validateReport(report);
        var blockers = new ArrayList<String>();
        Instant issued = Instant.parse(report.path("issuedAt").asText());
        Instant expires = Instant.parse(report.path("expiresAt").asText());
        boolean fresh = fresh(issued, now) && expires.isAfter(now) && expires.isAfter(issued);
        if (!fresh) blockers.add("OBSERVATIONS_STALE");
        JsonNode current = operations.path("currentRelease"), target = operations.path("targetRelease");
        if (!validRelease(current)) blockers.add("CURRENT_RELEASE_MISSING");
        if (!validRelease(target) || !target.path("releaseId").asText().equals(report.path("releaseId").asText())
                || target.path("sequence").asLong() != report.path("targetSequence").asLong()
                || !target.path("digest").asText().equals(report.path("releaseLockCanonicalDigest").asText())) {
            blockers.add("TARGET_RELEASE_MISMATCH");
        }
        if (!digest(target.path("deploymentManifestDigest").asText()) || !digest(target.path("packageManifestDigest").asText())) {
            blockers.add("PACKAGE_OR_DEPLOYMENT_MANIFEST_BINDING_MISSING");
        }
        var votes = new ArrayList<Map<String, Object>>();
        var seen = new HashSet<String>();
        int compatible = 0;
        if (!report.path("observations").isArray()) blockers.add("OBSERVATIONS_MISSING");
        for (JsonNode observation : report.path("observations")) {
            String observer = observation.path("observerId").asText();
            boolean known = config.observers.contains(observer) && seen.add(observer);
            boolean voteFresh = fresh && freshTime(observation.path("observedAt").asText(), now);
            boolean match = validRelease(target)
                    && observation.path("observedSequence").asLong(-1) == target.path("sequence").asLong()
                    && observation.path("releaseDigest").asText().equals(target.path("digest").asText());
            boolean accepted = known && voteFresh && match && "compatible".equals(observation.path("status").asText());
            if (accepted) compatible++;
            if (!known) blockers.add("UNKNOWN_OR_DUPLICATE_OBSERVER");
            votes.add(Map.of("observerId", observer, "status", observation.path("status").asText("unknown"),
                    "observedAt", observation.path("observedAt").asText(), "fresh", voteFresh,
                    "observedSequence", observation.path("observedSequence").asLong(-1),
                    "releaseDigest", observation.path("releaseDigest").asText(), "accepted", accepted));
        }
        if (config.minimumVotes < 1 || config.minimumVotes > new HashSet<>(config.observers).size()
                || compatible < config.minimumVotes) blockers.add("QUORUM_INSUFFICIENT");
        var backups = new ArrayList<Map<String, Object>>();
        boolean restoreReady = false;
        for (JsonNode backup : operations.path("backups")) {
            JsonNode restore = backup.path("restoreEvidence");
            boolean accepted = fresh && validRelease(current) && digest(backup.path("objectDigest").asText())
                    && backup.path("releaseDigest").asText().equals(current.path("digest").asText())
                    && freshTime(backup.path("createdAt").asText(), now, config.maximumBackupAgeSeconds)
                    && "PASSED".equals(restore.path("status").asText())
                    && restore.path("objectDigest").asText().equals(backup.path("objectDigest").asText())
                    && freshTime(restore.path("verifiedAt").asText(), now, config.maximumRestoreAgeSeconds)
                    && !backup.path("backupId").asText().isBlank();
            restoreReady |= accepted;
            backups.add(Map.of("backupId", backup.path("backupId").asText(), "createdAt", backup.path("createdAt").asText(),
                    "objectDigest", backup.path("objectDigest").asText(), "releaseDigest", backup.path("releaseDigest").asText(),
                    "restoreEvidence", safe(restore), "accepted", accepted));
        }
        if (!restoreReady) blockers.add("RESTORE_EVIDENCE_MISSING_OR_STALE");
        JsonNode update = operations.path("update");
        String phase = update.path("phase").asText("UNKNOWN");
        if (!List.of("IDLE", "PREPARING", "APPLYING", "VERIFYING", "COMPLETED", "FAILED", "ROLLING_BACK").contains(phase)
                || !update.path("history").isArray()) blockers.add("UPDATE_HISTORY_MISSING");
        if (List.of("FAILED", "ROLLING_BACK", "PREPARING", "APPLYING", "VERIFYING").contains(phase)) blockers.add("UPDATE_NOT_IDLE");
        // Legacy independently invented envelopes do not prove the normative
        // Node/Bank contract, ordered consensus or signed restore evidence.
        blockers.add("LEGACY_NON_NORMATIVE_OPERATIONAL_ENVELOPE");
        var result = new LinkedHashMap<String, Object>();
        result.put("verification", "VERIFIED");
        result.put("evidenceDigest", "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload)));
        result.put("signers", trusted.stream().sorted().toList());
        result.put("issuedAt", issued.toString()); result.put("expiresAt", expires.toString());
        result.put("fresh", fresh); result.put("currentRelease", safe(current)); result.put("targetRelease", safe(target));
        result.put("quorum", Map.of("requiredVotes", config.minimumVotes, "compatibleVotes", compatible, "votes", votes));
        result.put("backups", backups); result.put("update", Map.of("phase", phase, "history", safe(update.path("history"))));
        result.put("blockers", blockers.stream().distinct().toList());
        result.put("ready", blockers.isEmpty());
        return result;
    }

    private Object safe(JsonNode node) throws java.io.IOException {
        return node.isMissingNode() || node.isNull() ? Map.of() : mapper.forType(Object.class).readValue(node);
    }
    private boolean freshTime(String value, Instant now) {
        return freshTime(value, now, config.maximumAgeSeconds);
    }
    private boolean freshTime(String value, Instant now, long ageSeconds) {
        try {
            Instant parsed = Instant.parse(value);
            return ageSeconds > 0 && !parsed.isAfter(now) && !parsed.isBefore(now.minusSeconds(ageSeconds));
        } catch (Exception e) { return false; }
    }
    private boolean fresh(Instant value, Instant now) {
        return config.maximumAgeSeconds > 0 && !value.isAfter(now) && !value.isBefore(now.minusSeconds(config.maximumAgeSeconds));
    }
    private boolean validRelease(JsonNode node) {
        return node.isObject() && node.path("releaseId").asText().matches("[a-z0-9][a-z0-9._-]{2,127}")
                && node.path("sequence").isIntegralNumber() && node.path("sequence").canConvertToLong() && node.path("sequence").asLong() > 0
                && digest(node.path("digest").asText());
    }
    private boolean digest(String value) { return value.matches("sha256:[0-9a-f]{64}"); }
    private void validateReport(JsonNode report) {
        var fields = java.util.Set.of("schema", "releaseId", "networkId", "targetSequence", "releaseLockCanonicalDigest",
                "issuedAt", "expiresAt", "observations", "signatures");
        if (!report.isObject() || report.size() != fields.size() || !report.properties().stream().allMatch(e -> fields.contains(e.getKey()))
                || !report.path("releaseId").asText().matches("[a-z0-9][a-z0-9._-]{2,127}")
                || !report.path("networkId").asText().matches("[a-z0-9][a-z0-9._-]{2,127}")
                || !report.path("targetSequence").isIntegralNumber() || !report.path("targetSequence").canConvertToLong() || report.path("targetSequence").asLong() < 1
                || !digest(report.path("releaseLockCanonicalDigest").asText())
                || !report.path("observations").isArray() || report.path("observations").isEmpty()
                || !report.path("signatures").isArray() || report.path("signatures").isEmpty()) throw new IllegalArgumentException("report schema");
        var observationFields = java.util.Set.of("observerId", "status", "observedSequence", "releaseDigest", "observedAt");
        for (JsonNode observation : report.path("observations")) {
            if (!observation.isObject() || observation.size() != observationFields.size()
                    || !observation.properties().stream().allMatch(e -> observationFields.contains(e.getKey()))
                    || !observation.path("observerId").asText().matches("[a-z0-9][a-z0-9._-]{2,127}")
                    || !List.of("compatible", "incompatible", "unknown").contains(observation.path("status").asText())
                    || !observation.path("observedSequence").isIntegralNumber() || !observation.path("observedSequence").canConvertToLong() || observation.path("observedSequence").asLong(-1) < 0
                    || !digest(observation.path("releaseDigest").asText()) || !observation.path("observedAt").isTextual()) throw new IllegalArgumentException("observation schema");
        }
    }
}
