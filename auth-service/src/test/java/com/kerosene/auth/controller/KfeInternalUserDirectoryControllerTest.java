package com.kerosene.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.kerosene.common.financial.FinancialUserDirectoryLookupRequest;
import com.kerosene.common.financial.FinancialUserDirectoryPort;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KfeInternalUserDirectoryControllerTest {

    private final FinancialUserDirectoryPort userDirectory = mock(FinancialUserDirectoryPort.class);
    private final KfeInternalUserDirectoryController controller =
            new KfeInternalUserDirectoryController(userDirectory);

    @Test
    void resolvesOnlyMinimalFinancialUserHandleByUsername() {
        FinancialUserDirectoryPort.FinancialUserHandle handle =
                new FinancialUserDirectoryPort.FinancialUserHandle(42L, "alice", true);
        when(userDirectory.findByUsername(" Alice ")).thenReturn(Optional.of(handle));

        var response = controller.lookup(
                FinancialUserDirectoryLookupRequest.byUsername(" Alice "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(handle, response.getBody().getData());
        verify(userDirectory).findByUsername(" Alice ");
    }

    @Test
    void resolvesByUserId() {
        FinancialUserDirectoryPort.FinancialUserHandle handle =
                new FinancialUserDirectoryPort.FinancialUserHandle(42L, "alice", false);
        when(userDirectory.findById(42L)).thenReturn(Optional.of(handle));

        var response = controller.lookup(
                FinancialUserDirectoryLookupRequest.byUserId(42L));

        assertEquals(handle, response.getBody().getData());
        verify(userDirectory).findById(42L);
    }

    @Test
    void returnsNotFoundWithoutExposingAdditionalUserData() {
        when(userDirectory.findByUsername("missing")).thenReturn(Optional.empty());

        var response = controller.lookup(
                FinancialUserDirectoryLookupRequest.byUsername("missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("AUTH_006", response.getBody().getErrorCode());
    }

    @Test
    void rejectsAmbiguousLookup() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.lookup(
                        new FinancialUserDirectoryLookupRequest("alice", 42L)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
