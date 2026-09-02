package com.kerosene.notification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.kerosene.common.dto.ApiResponse;
import com.kerosene.common.financial.FinancialDepositConfirmedNotificationRequest;
import com.kerosene.common.financial.FinancialNotificationPort;
import com.kerosene.common.financial.FinancialOutboundNotificationRequest;
import com.kerosene.common.financial.FinancialPaymentRequestDepositConfirmedNotificationRequest;
import com.kerosene.common.financial.FinancialInternalTransferNotificationRequest;
import com.kerosene.common.financial.FinancialExternalPaymentNotificationRequest;

@RestController
@RequestMapping("/internal/kfe/notifications")
public class KfeInternalFinancialNotificationController {

    private final FinancialNotificationPort notificationPort;

    public KfeInternalFinancialNotificationController(FinancialNotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    @PostMapping("/deposit-confirmed")
    public ResponseEntity<ApiResponse<Void>> notifyDepositConfirmed(
            @RequestBody FinancialDepositConfirmedNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyDepositConfirmed(
                request.userId(),
                request.transactionId(),
                request.walletId(),
                request.rail(),
                request.creditedSats(),
                request.confirmations());
        return ResponseEntity.ok(ApiResponse.success("KFE deposit notification accepted.", null));
    }

    @PostMapping("/deposit-detected")
    public ResponseEntity<ApiResponse<Void>> notifyDepositDetected(
            @RequestBody FinancialDepositConfirmedNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyDepositDetected(
                request.userId(),
                request.transactionId(),
                request.walletId(),
                request.rail(),
                request.creditedSats(),
                request.confirmations());
        return ResponseEntity.ok(ApiResponse.success("KFE deposit detected notification accepted.", null));
    }

    @PostMapping("/deposit-progress")
    public ResponseEntity<ApiResponse<Void>> notifyDepositConfirmationProgress(
            @RequestBody FinancialDepositConfirmedNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyDepositConfirmationProgress(
                request.userId(),
                request.transactionId(),
                request.walletId(),
                request.rail(),
                request.creditedSats(),
                request.confirmations());
        return ResponseEntity.ok(ApiResponse.success("KFE deposit progress notification accepted.", null));
    }

    @PostMapping("/payment-request-deposit-confirmed")
    public ResponseEntity<ApiResponse<Void>> notifyPaymentRequestDepositConfirmed(
            @RequestBody FinancialPaymentRequestDepositConfirmedNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.paymentRequestId() != null, "paymentRequestId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyPaymentRequestDepositConfirmed(
                request.userId(),
                request.transactionId(),
                request.paymentRequestId(),
                request.publicId(),
                request.walletId(),
                request.rail(),
                request.creditedSats());
        return ResponseEntity.ok(ApiResponse.success("KFE payment request notification accepted.", null));
    }

    @PostMapping("/outbound-detected")
    public ResponseEntity<ApiResponse<Void>> notifyOutboundDetected(
            @RequestBody FinancialOutboundNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyOutboundDetected(
                request.userId(),
                request.transactionId(),
                request.walletId(),
                request.rail(),
                request.amountSats(),
                request.confirmations(),
                request.destinationHint());
        return ResponseEntity.ok(ApiResponse.success("KFE outbound detected notification accepted.", null));
    }

    @PostMapping("/outbound-confirmed")
    public ResponseEntity<ApiResponse<Void>> notifyOutboundConfirmed(
            @RequestBody FinancialOutboundNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyOutboundConfirmed(
                request.userId(),
                request.transactionId(),
                request.walletId(),
                request.rail(),
                request.amountSats(),
                request.confirmations());
        return ResponseEntity.ok(ApiResponse.success("KFE outbound confirmed notification accepted.", null));
    }

    @PostMapping("/internal-transfer-received")
    public ResponseEntity<ApiResponse<Void>> notifyInternalTransferReceived(
            @RequestBody FinancialInternalTransferNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyInternalTransferReceived(
                request.userId(),
                request.transactionId(),
                request.walletId(),
                request.amountSats());
        return ResponseEntity.ok(ApiResponse.success("KFE internal transfer received notification accepted.", null));
    }

    @PostMapping("/internal-transfer-sent")
    public ResponseEntity<ApiResponse<Void>> notifyInternalTransferSent(
            @RequestBody FinancialInternalTransferNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyInternalTransferSent(
                request.userId(),
                request.transactionId(),
                request.walletId(),
                request.amountSats());
        return ResponseEntity.ok(ApiResponse.success("KFE internal transfer sent notification accepted.", null));
    }

    @PostMapping("/external-payment-sent")
    public ResponseEntity<ApiResponse<Void>> notifyExternalPaymentSent(
            @RequestBody FinancialExternalPaymentNotificationRequest request) {
        require(request != null, "request is required");
        require(request.userId() != null, "userId is required");
        require(request.transactionId() != null, "transactionId is required");
        require(request.walletId() != null, "walletId is required");

        notificationPort.notifyExternalPaymentSent(
                request.userId(),
                request.transactionId(),
                request.walletId(),
                request.rail(),
                request.amountSats());
        return ResponseEntity.ok(ApiResponse.success("KFE external payment sent notification accepted.", null));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

}
