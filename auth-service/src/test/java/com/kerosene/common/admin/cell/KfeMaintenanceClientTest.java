package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KfeMaintenanceClientTest {
    @TempDir Path temporary;
    @Test void absentAuthIsUnknownAndFreshStatusWithBlockersCannotBeSafe() throws Exception {
        var config = new CellOperationsProperties(); var client = new KfeMaintenanceClient(config, mock(BankObservationClient.class));
        var now = Instant.parse("2026-10-01T12:00:00Z"); assertEquals(false, client.status(now).get("safeToUpdate"));
        var status = new ObjectMapper().readTree("""
                {"schema":"kerosene.kfe-maintenance/v1","mode":"DRAINING","changeId":"change-one","revision":1,
                 "observedAt":"2026-10-01T12:00:00Z","safeToUpdate":true,"blockers":{"inFlight":1}}
                """);
        assertEquals(false, client.validate(status, now).get("safeToUpdate"));
        assertThrows(Exception.class, () -> client.validate(status, now.plusSeconds(301)));
    }
    @Test void privateBoundedCredentialIsReadAgainForRotationAndUnsafeFilesNeverReachHttp() throws Exception {
        var config = new CellOperationsProperties(); config.kfeUrl = "https://kfe.example.invalid";
        Path token = temporary.resolve("token"); config.kfeAdminTokenFile = token.toString();
        Files.writeString(token, "synthetic-first"); Files.setPosixFilePermissions(token, PosixFilePermissions.fromString("rw-------"));
        var http = mock(BankObservationClient.class);
        byte[] response = """
                {"schema":"kerosene.kfe-maintenance/v1","mode":"DRAINING","changeId":"change-one","revision":1,
                 "observedAt":"2026-10-01T12:00:00Z","safeToUpdate":false,"blockers":{"coverageUnknown":1}}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(http.fetch(eq(config.kfeUrl), eq("/api/admin/kfe/maintenance/status"), anyString())).thenReturn(response);
        var client = new KfeMaintenanceClient(config, http); var now = Instant.parse("2026-10-01T12:00:00Z");
        assertEquals("AUTHENTICATED", client.status(now).get("verification"));
        verify(http).fetch(config.kfeUrl, "/api/admin/kfe/maintenance/status", "synthetic-first");
        Files.writeString(token, "synthetic-second"); assertEquals("AUTHENTICATED", client.status(now).get("verification"));
        verify(http).fetch(config.kfeUrl, "/api/admin/kfe/maintenance/status", "synthetic-second");
        clearInvocations(http);
        Files.writeString(token, "x".repeat(16385)); assertEquals("UNKNOWN", client.status(now).get("verification"));
        Files.writeString(token, "synthetic-second"); Files.setPosixFilePermissions(token, PosixFilePermissions.fromString("rw-r--r--"));
        assertEquals("UNKNOWN", client.status(now).get("verification"));
        Files.setPosixFilePermissions(token, PosixFilePermissions.fromString("rw-------"));
        Path linkedDirectory = temporary.resolve("linked-parent"); Files.createSymbolicLink(linkedDirectory, temporary);
        config.kfeAdminTokenFile = linkedDirectory.resolve("token").toString();
        assertEquals("UNKNOWN", client.status(now).get("verification"));
        verifyNoInteractions(http);
    }
    @Test void onlyActualDrainingModeWithAllExplicitCoverageCountsCanBeSafe() throws Exception {
        var config = new CellOperationsProperties(); var client = new KfeMaintenanceClient(config, mock(BankObservationClient.class));
        var now = Instant.parse("2026-10-01T12:00:00Z");
        var status = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree("""
                {"schema":"kerosene.kfe-maintenance/v1","mode":"DRAINING","changeId":"change-one","revision":1,
                 "observedAt":"2026-10-01T12:00:00Z","safeToUpdate":true,
                 "blockers":{"mutationCoverageUnknown":0,"callbackCoverageUnknown":0,"readSideEffectsUnknown":0}}
                """);
        assertEquals(true, client.validate(status, now).get("safeToUpdate"));
        status.put("mode", "ACTIVE"); assertEquals(false, client.validate(status, now).get("safeToUpdate"));
        status.put("mode", "DRAINED"); assertThrows(Exception.class, () -> client.validate(status, now));
        status.put("mode", "DRAINING"); ((com.fasterxml.jackson.databind.node.ObjectNode) status.path("blockers")).remove("callbackCoverageUnknown");
        var missing = client.validate(status, now); assertEquals(false, missing.get("safeToUpdate"));
        assertEquals(1, ((java.util.Map<?, ?>) missing.get("blockers")).get("KFE_COVERAGE_EVIDENCE_MISSING"));
    }
}
