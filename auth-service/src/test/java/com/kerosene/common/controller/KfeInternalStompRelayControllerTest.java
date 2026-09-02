package com.kerosene.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.kerosene.common.financial.StompUserPublishRequest;
import com.kerosene.common.service.StompUserRelayService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class KfeInternalStompRelayControllerTest {

    private final StompUserRelayService relayService = mock(StompUserRelayService.class);
    private final KfeInternalStompRelayController controller =
            new KfeInternalStompRelayController(relayService);

    @Test
    void forwardsAllowlistedPublishWhenCredentialMatches() {
        controller.publish(
                new StompUserPublishRequest(42L, "/queue/transactions", Map.of("id", "tx-1")));

        verify(relayService).publishToUser(42L, "/queue/transactions", Map.of("id", "tx-1"));
    }

    @Test
    void rejectsBlankDestination() {
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.publish(
                        new StompUserPublishRequest(42L, "  ", Map.of("id", "1"))));
        verifyNoInteractions(relayService);
    }
}
