package com.kerosene.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.kerosene.common.financial.FinancialColdWalletPsbtApprovalRequest;
import com.kerosene.common.financial.FinancialCustodyTransferApprovalRequest;
import com.kerosene.common.financial.FinancialLocalFactorApprovalRequest;
import com.kerosene.common.financial.FinancialTransactionApprovalPort;
import com.kerosene.common.financial.FinancialWalletOutboundApprovalRequest;
import com.kerosene.common.financial.DeviceProof;
import com.kerosene.common.financial.PasskeyAssertion;
import com.kerosene.common.financial.RecoveryApproval;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KfeInternalTransactionApprovalControllerTest {

    private static final DeviceProof DEVICE_PROOF =
            new DeviceProof("device", "proof", "challenge", Instant.parse("2026-07-27T12:00:00Z"));
    private static final PasskeyAssertion PASSKEY =
            new PasskeyAssertion("credential", "client-data", "auth-data", "signature", "user");
    private static final RecoveryApproval RECOVERY =
            new RecoveryApproval("recovery-proof", "challenge", Instant.parse("2026-07-27T12:00:00Z"));

    private final FinancialTransactionApprovalPort approvalPort = mock(FinancialTransactionApprovalPort.class);
    private final KfeInternalTransactionApprovalController controller =
            new KfeInternalTransactionApprovalController(approvalPort);

    @Test
    void approvesLocalFactorWhenCredentialMatches() {
        controller.approveLocalFactor(
                new FinancialLocalFactorApprovalRequest(42L, "device", DEVICE_PROOF));

        verify(approvalPort).approveLocalFactor(42L, "device", DEVICE_PROOF);
    }

    @Test
    void approvesCustodyTransferWhenCredentialMatches() {
        controller.approveCustodyTransfer(
                new FinancialCustodyTransferApprovalRequest(42L, PASSKEY));

        verify(approvalPort).approveCustodyTransfer(42L, PASSKEY);
    }

    @Test
    void approvesWalletOutboundWhenCredentialMatches() {
        controller.approveWalletOutbound(
                new FinancialWalletOutboundApprovalRequest(41L, 42L, PASSKEY, RECOVERY, DEVICE_PROOF));

        verify(approvalPort).approveWalletOutbound(41L, 42L, PASSKEY, RECOVERY, DEVICE_PROOF);
    }

    @Test
    void approvesColdWalletPsbtWhenCredentialMatches() {
        controller.approveColdWalletPsbt(
                new FinancialColdWalletPsbtApprovalRequest(42L, DEVICE_PROOF));

        verify(approvalPort).approveColdWalletPsbt(42L, DEVICE_PROOF);
    }

    @Test
    void rejectsMissingUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.approveLocalFactor(
                        new FinancialLocalFactorApprovalRequest(null, "device", DEVICE_PROOF)));
    }
}
