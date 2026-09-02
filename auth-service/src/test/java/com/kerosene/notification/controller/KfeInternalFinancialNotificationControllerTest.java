package com.kerosene.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.kerosene.common.financial.FinancialDepositConfirmedNotificationRequest;
import com.kerosene.common.financial.FinancialNotificationPort;
import com.kerosene.common.financial.FinancialPaymentRequestDepositConfirmedNotificationRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KfeInternalFinancialNotificationControllerTest {

    private final FinancialNotificationPort notificationPort = mock(FinancialNotificationPort.class);
    private final KfeInternalFinancialNotificationController controller =
            new KfeInternalFinancialNotificationController(notificationPort);

    @Test
    void forwardsDepositConfirmedNotificationWhenCredentialMatches() {
        UUID transactionId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        controller.notifyDepositConfirmed(
                new FinancialDepositConfirmedNotificationRequest(
                        42L,
                        transactionId,
                        walletId,
                        "ONCHAIN",
                        1500L,
                        3));

        verify(notificationPort).notifyDepositConfirmed(42L, transactionId, walletId, "ONCHAIN", 1500L, 3);
    }

    @Test
    void forwardsDepositDetectedNotificationWhenCredentialMatches() {
        UUID transactionId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        controller.notifyDepositDetected(
                new FinancialDepositConfirmedNotificationRequest(
                        42L,
                        transactionId,
                        walletId,
                        "ONCHAIN",
                        1500L,
                        1));

        verify(notificationPort).notifyDepositDetected(42L, transactionId, walletId, "ONCHAIN", 1500L, 1);
    }

    @Test
    void forwardsDepositConfirmationProgressNotificationWhenCredentialMatches() {
        UUID transactionId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        controller.notifyPaymentRequestDepositConfirmed(
                new FinancialPaymentRequestDepositConfirmedNotificationRequest(
                        42L,
                        transactionId,
                        UUID.randomUUID(),
                        "public-id",
                        walletId,
                        "ONCHAIN",
                        1500L));

        controller.notifyDepositConfirmationProgress(
                new FinancialDepositConfirmedNotificationRequest(
                        42L,
                        transactionId,
                        walletId,
                        "ONCHAIN",
                        1500L,
                        2));

        verify(notificationPort).notifyDepositConfirmationProgress(42L, transactionId, walletId, "ONCHAIN", 1500L, 2);
    }

    @Test
    void forwardsPaymentRequestDepositConfirmedNotificationWhenCredentialMatches() {
        UUID transactionId = UUID.randomUUID();
        UUID paymentRequestId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        controller.notifyPaymentRequestDepositConfirmed(
                new FinancialPaymentRequestDepositConfirmedNotificationRequest(
                        42L,
                        transactionId,
                        paymentRequestId,
                        "public-id",
                        walletId,
                        "LIGHTNING",
                        2500L));

        verify(notificationPort).notifyPaymentRequestDepositConfirmed(
                42L,
                transactionId,
                paymentRequestId,
                "public-id",
                walletId,
                "LIGHTNING",
                2500L);
    }

    @Test
    void rejectsMissingUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.notifyDepositConfirmed(
                        new FinancialDepositConfirmedNotificationRequest(
                                null,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "ONCHAIN",
                                1500L,
                                3)));
    }
}
