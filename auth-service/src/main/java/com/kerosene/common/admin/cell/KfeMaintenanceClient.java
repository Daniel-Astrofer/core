package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Maintenance is ROLE_ADMIN; the legacy internal shared secret cannot authorize this endpoint. */
@Component
public class KfeMaintenanceClient {
    private final CellOperationsProperties config;
    private final BankObservationClient http;
    private final ObjectReader mapper = com.kerosene.config.ReleaseJsonConfig.reader();
    public KfeMaintenanceClient(CellOperationsProperties config, BankObservationClient http) { this.config = config; this.http = http; }
    public Map<String, Object> status(Instant now) {
        try {
            if (config.kfeUrl.isBlank() || config.kfeAdminTokenFile.isBlank()) throw new IllegalStateException();
            Path credential = Path.of(config.kfeAdminTokenFile);
            byte[] credentialBytes = com.kerosene.common.release.ReleaseEvidenceFiles.read(credential, 16384, true);
            String token;
            try { token = new String(credentialBytes, java.nio.charset.StandardCharsets.UTF_8).trim(); }
            finally { java.util.Arrays.fill(credentialBytes, (byte) 0); }
            if (token.isBlank()) throw new IllegalStateException();
            JsonNode status = mapper.readTree(http.fetch(config.kfeUrl, "/api/admin/kfe/maintenance/status", token));
            return validate(status, now);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return Map.of("verification", "UNKNOWN", "mode", "UNKNOWN", "safeToUpdate", false,
                    "blockers", Map.of("KFE_MAINTENANCE_UNAVAILABLE", 1));
        }
    }
    Map<String, Object> validate(JsonNode status, Instant now) throws Exception {
        if (!"kerosene.kfe-maintenance/v1".equals(status.path("schema").asText())
                || !status.path("safeToUpdate").isBoolean() || !status.path("revision").isIntegralNumber()
                || !status.path("revision").canConvertToLong() || status.path("revision").asLong(-1) < 0
                || status.path("revision").asLong() > 9007199254740991L || !status.path("blockers").isObject()
                || !status.path("mode").isTextual() || !java.util.List.of("ACTIVE", "DRAINING").contains(status.path("mode").asText())
                || !status.has("changeId")) throw new IllegalArgumentException();
        boolean draining = "DRAINING".equals(status.path("mode").asText());
        if (draining && (!status.path("changeId").isTextual()
                || !status.path("changeId").asText().matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}")
                || status.path("revision").asLong() < 1)) throw new IllegalArgumentException();
        Instant observed = Instant.parse(status.path("observedAt").asText());
        if (observed.isAfter(now) || config.maximumAgeSeconds < 1 || observed.isBefore(now.minusSeconds(config.maximumAgeSeconds))) throw new IllegalArgumentException();
        boolean blocked = false;
        for (JsonNode count : status.path("blockers")) {
            if (!count.isIntegralNumber() || !count.canConvertToLong() || count.asLong(-1) < 0 || count.asLong() > 9007199254740991L) throw new IllegalArgumentException();
            blocked |= count.asLong() > 0;
        }
        var result = mapper.forType(java.util.LinkedHashMap.class).<java.util.LinkedHashMap<String, Object>>readValue(status);
        boolean coveragePresent = java.util.List.of("mutationCoverageUnknown", "callbackCoverageUnknown", "readSideEffectsUnknown")
                .stream().allMatch(status.path("blockers")::has);
        if (!coveragePresent) {
            var blockers = mapper.forType(java.util.LinkedHashMap.class).<java.util.LinkedHashMap<String, Object>>readValue(status.path("blockers"));
            blockers.put("KFE_COVERAGE_EVIDENCE_MISSING", 1); result.put("blockers", blockers);
        }
        result.put("verification", "AUTHENTICATED");
        result.put("safeToUpdate", status.path("safeToUpdate").asBoolean() && !blocked && draining && coveragePresent);
        return result;
    }
}
