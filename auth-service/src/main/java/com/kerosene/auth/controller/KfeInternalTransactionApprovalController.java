package com.kerosene.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.kerosene.common.dto.ApiResponse;
import com.kerosene.common.financial.FinancialColdWalletPsbtApprovalRequest;
import com.kerosene.common.financial.FinancialCustodyTransferApprovalRequest;
import com.kerosene.common.financial.FinancialLocalFactorApprovalRequest;
import com.kerosene.common.financial.FinancialTransactionApprovalPort;
import com.kerosene.common.financial.FinancialWalletOutboundApprovalRequest;

@RestController
@RequestMapping("/internal/kfe/transaction-approval")
public class KfeInternalTransactionApprovalController {

    private final FinancialTransactionApprovalPort approvalPort;

    public KfeInternalTransactionApprovalController(FinancialTransactionApprovalPort approvalPort) {
        this.approvalPort = approvalPort;
    }

    @PostMapping("/local-factor")
    public ResponseEntity<ApiResponse<Void>> approveLocalFactor(
            @RequestBody FinancialLocalFactorApprovalRequest request) {
        require(request != null && request.userId() != null, "userId is required");
        approvalPort.approveLocalFactor(request.userId(), request.deviceRef(), request.factor());
        return ResponseEntity.ok(ApiResponse.success("KFE local factor approved.", null));
    }

    @PostMapping("/custody-transfer")
    public ResponseEntity<ApiResponse<Void>> approveCustodyTransfer(
            @RequestBody FinancialCustodyTransferApprovalRequest request) {
        require(request != null && request.userId() != null, "userId is required");
        approvalPort.approveCustodyTransfer(request.userId(), request.assertion());
        return ResponseEntity.ok(ApiResponse.success("KFE custody transfer approved.", null));
    }

    @PostMapping("/wallet-outbound")
    public ResponseEntity<ApiResponse<Void>> approveWalletOutbound(
            @RequestBody FinancialWalletOutboundApprovalRequest request) {
        require(request != null && request.actorUserId() != null, "actorUserId is required");
        require(request.ownerUserId() != null, "ownerUserId is required");
        approvalPort.approveWalletOutbound(
                request.actorUserId(),
                request.ownerUserId(),
                request.passkeyAssertion(),
                request.recoveryApproval(),
                request.deviceProof());
        return ResponseEntity.ok(ApiResponse.success("KFE wallet outbound approved.", null));
    }

    @PostMapping("/cold-wallet-psbt")
    public ResponseEntity<ApiResponse<Void>> approveColdWalletPsbt(
            @RequestBody FinancialColdWalletPsbtApprovalRequest request) {
        require(request != null && request.userId() != null, "userId is required");
        approvalPort.approveColdWalletPsbt(request.userId(), request.factor());
        return ResponseEntity.ok(ApiResponse.success("KFE cold wallet PSBT approved.", null));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

}
