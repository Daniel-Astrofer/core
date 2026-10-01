package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Maintenance is ROLE_ADMIN; the legacy internal shared secret cannot authorize this endpoint. */
@Component
public class KfeMaintenanceClient {
    private final CellOperationsProperties config;
    private final BankObservationClient http;
    private final ObjectMapper mapper = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    public KfeMaintenanceClient(CellOperationsProperties config, BankObservationClient http) { this.config = config; this.http = http; }
    public Map<String, Object> status(Instant now) {
        try {
            if (config.kfeUrl.isBlank() || config.kfeAdminTokenFile.isBlank()) throw new IllegalStateException();
            Path credential = Path.of(config.kfeAdminTokenFile);
            if (!Files.isRegularFile(credential) || Files.isSymbolicLink(credential) || Files.size(credential) > 16384) throw new IllegalStateException();
            var permissions = Files.getPosixFilePermissions(credential);
            if (permissions.stream().anyMatch(p -> p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_"))) throw new IllegalStateException();
            String token = Files.readString(credential).trim();
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
                || !status.path("revision").canConvertToLong() || status.path("revision").asLong(-1) < 0 || !status.path("blockers").isObject()
                || !status.path("mode").isTextual() || status.path("mode").asText().isBlank()
                || "UNKNOWN".equalsIgnoreCase(status.path("mode").asText()) || !status.has("changeId")) throw new IllegalArgumentException();
        Instant observed = Instant.parse(status.path("observedAt").asText());
        if (observed.isAfter(now) || config.maximumAgeSeconds < 1 || observed.isBefore(now.minusSeconds(config.maximumAgeSeconds))) throw new IllegalArgumentException();
        boolean blocked = false;
        for (JsonNode count : status.path("blockers")) {
            if (!count.isIntegralNumber() || !count.canConvertToLong() || count.asLong(-1) < 0) throw new IllegalArgumentException();
            blocked |= count.asLong() > 0;
        }
        var result = mapper.convertValue(status, java.util.LinkedHashMap.class);
        result.put("verification", "AUTHENTICATED");
        result.put("safeToUpdate", status.path("safeToUpdate").asBoolean() && !blocked);
        return result;
    }
}
