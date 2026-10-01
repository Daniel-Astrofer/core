package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KfeMaintenanceClientTest {
    @Test void absentAuthIsUnknownAndFreshStatusWithBlockersCannotBeSafe() throws Exception {
        var config = new CellOperationsProperties(); var client = new KfeMaintenanceClient(config, mock(BankObservationClient.class));
        var now = Instant.parse("2026-10-01T12:00:00Z"); assertEquals(false, client.status(now).get("safeToUpdate"));
        var status = new ObjectMapper().readTree("""
                {"schema":"kerosene.kfe-maintenance/v1","mode":"DRAINED","changeId":"change-one","revision":1,
                 "observedAt":"2026-10-01T12:00:00Z","safeToUpdate":true,"blockers":{"inFlight":1}}
                """);
        assertEquals(false, client.validate(status, now).get("safeToUpdate"));
        assertThrows(Exception.class, () -> client.validate(status, now.plusSeconds(301)));
    }
}
