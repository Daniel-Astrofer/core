package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kerosene.common.release.ReleaseManifestService;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CellOperationsServiceTest {
    @TempDir Path temp;
    @Test void unavailableEvidenceAndOptionalRuntimeNeverBecomeReady() throws Exception {
        var config = new CellOperationsProperties(); var bank = mock(BankObservationClient.class);
        when(bank.fetch()).thenThrow(new IllegalStateException());
        var release = mock(ReleaseManifestService.class); when(release.snapshot()).thenReturn(runtime(false));
        var kfe = mock(KfeMaintenanceClient.class); when(kfe.status(any())).thenReturn(Map.of("safeToUpdate", false));
        var service = new CellOperationsService(bank, new CellEvidenceVerifier(config), release, config, new ObjectMapper(), kfe);
        var snapshot = service.snapshot(); assertEquals(false, snapshot.get("ready")); assertEquals("UNVERIFIED", snapshot.get("verification"));
        assertTrue(((List<?>) snapshot.get("blockers")).contains("KFE_MAINTENANCE_NOT_SAFE"));
    }
    @Test void plansPersistIntentAndRejectChangedTarget() throws Exception {
        var config = new CellOperationsProperties(); config.planDirectory = temp.toString();
        var verifier = mock(CellEvidenceVerifier.class); var bank = mock(BankObservationClient.class); when(bank.fetch()).thenReturn(new byte[0]);
        var release = mock(ReleaseManifestService.class); when(release.snapshot()).thenReturn(runtime(true));
        var kfe = mock(KfeMaintenanceClient.class); when(kfe.status(any())).thenReturn(Map.of("safeToUpdate", true));
        String digest = "sha256:" + "a".repeat(64);
        when(verifier.verify(any(), any())).thenAnswer(call -> new LinkedHashMap<>(Map.of("ready", true, "blockers", List.of(),
                "targetRelease", Map.of("releaseId", "release-new", "sequence", 2, "digest", digest, "deploymentManifestDigest", digest, "packageManifestDigest", digest),
                "evidenceDigest", digest)));
        var service = new CellOperationsService(bank, verifier, release, config, new ObjectMapper(), kfe);
        var plan = service.plan(new CellOperationsService.PlanRequest("release-new", 2, digest, digest, digest), "operator", "req-one");
        assertEquals("PLANNED", plan.get("status")); assertEquals(false, plan.get("deploymentExecuted"));
        assertEquals(1, service.plans().size());
        assertEquals(new ObjectMapper().writeValueAsString(plan), new ObjectMapper().writeValueAsString(service.plan((String) plan.get("planId"))));
        assertThrows(ResponseStatusException.class, () -> service.plan(new CellOperationsService.PlanRequest("release-old", 2, digest, digest, digest), "operator", "req-two"));
        when(kfe.status(any())).thenReturn(Map.of("safeToUpdate", false));
        assertEquals("BLOCKED", service.plan(new CellOperationsService.PlanRequest("release-new", 2, digest, digest, digest), "operator", "req-three").get("status"));
    }
    static ReleaseManifestService.ReleaseSnapshot runtime(boolean signed) {
        return new ReleaseManifestService.ReleaseSnapshot("core", "test", "commit", "time", "digest", "code", "config", "digest", signed, true, "reason", "message", Map.of(), Map.of());
    }
    @Test void linkedAndNonregularPlanStorageIsRejected() throws Exception {
        var config = new CellOperationsProperties(); config.planDirectory = temp.toString();
        var service = new CellOperationsService(mock(BankObservationClient.class), mock(CellEvidenceVerifier.class),
                mock(ReleaseManifestService.class), config, new ObjectMapper(), mock(KfeMaintenanceClient.class));
        String id = "11111111-1111-1111-1111-111111111111";
        Path outside = temp.resolve("outside.json"); java.nio.file.Files.writeString(outside, "{\"secret\":\"must-not-be-read\"}");
        java.nio.file.Files.createSymbolicLink(temp.resolve(id + ".json"), outside);
        assertThrows(ResponseStatusException.class, () -> service.plan(id));
        String directoryId = "22222222-2222-2222-2222-222222222222";
        java.nio.file.Files.createDirectory(temp.resolve(directoryId + ".json"));
        assertThrows(ResponseStatusException.class, () -> service.plan(directoryId));
        Path linkedParent = temp.resolve("linked-parent"); java.nio.file.Files.createSymbolicLink(linkedParent, temp);
        config.planDirectory = linkedParent.resolve("child").toString();
        assertThrows(ResponseStatusException.class, service::plans);
        assertFalse(java.nio.file.Files.exists(temp.resolve("child")));
    }
}
