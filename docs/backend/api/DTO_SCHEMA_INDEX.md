# DTO Schema Index

Índice auxiliar dos DTOs usados pelos endpoints documentados.

Este arquivo não é a documentação operacional de API. Para entender um endpoint,
consulte primeiro [`docs/backend/api/`](README.md). Fluxos financeiros pertencem
a `kerosene-kfe/docs/reference/api/KFE.md`.

Fonte técnica: DTOs Java em `auth-service/src/main/java/com/kerosene/**` e `kerosene-kfe/src/main/java/com/kerosene/kfe/**`, controllers ativos, `EndpointPolicyRegistry`, configuração de segurança e anotações `@PreAuthorize`.

Regra KFE-only: DTOs financeiros legados removidos não devem aparecer neste índice como schemas ativos.

## `AccountActivationStatusDTO`

Fonte: [AccountActivationStatusDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AccountActivationStatusDTO.java)

- `boolean activated`
- `boolean canReceiveInbound`
- `boolean requiresActivationDeposit`
- `BigDecimal requiredAmountBtc`
- `String paymentLinkId`
- `String depositAddress`
- `String paymentStatus`
- `String warningMessage`
- `LocalDateTime activatedAt`

## `AccountSecurityProfileDTO`

Fonte: [AccountSecurityProfileDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AccountSecurityProfileDTO.java)

- `AccountSecurityType accountSecurity`
- `Integer shamirTotalShares`
- `Integer shamirThreshold`
- `Integer multisigThreshold`
- `boolean passkeyAvailable`
- `boolean passkeyEnabledForTransactions`
- `AppPinStatusDTO appPin`
- `List<String> requiredFactors`
- `PasskeyInventoryDTO passkeys`

## `AccountSecurityStatusDTO`

Fonte: [AccountSecurityStatusDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AccountSecurityStatusDTO.java)

- `boolean passwordConfigured`
- `boolean passkeyRegistered`
- `boolean totpEnabled`
- `int backupCodesRemaining`
- `boolean unprotected`
- `String warningMessage`
- `boolean accountActivated`
- `boolean inboundEnabled`
- `PasskeyInventoryDTO passkeys`

## `AccountSecurityUpdateRequestDTO`

Fonte: [AccountSecurityUpdateRequestDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AccountSecurityUpdateRequestDTO.java)

- `accountSecurity: AccountSecurityType`
- `shamirTotalShares: Integer`
- `shamirThreshold: Integer`
- `multisigThreshold: Integer`

## `AdminAccessAttemptDTO`

Fonte: [AdminAccessAttemptDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AdminAccessAttemptDTO.java)

- `UUID attemptId`
- `String status`
- `String deviceId`
- `String deviceName`
- `String browser`
- `String userAgent`
- `String ipFingerprint`
- `LocalDateTime requestedAt`
- `LocalDateTime expiresAt`

## `AdminAccessDecisionRequestDTO`

Fonte: [AdminAccessDecisionRequestDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AdminAccessDecisionRequestDTO.java)

- `decision: String`

## `AdminAuthenticatedDeviceDTO`

Fonte: [AdminAuthenticatedDeviceDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AdminAuthenticatedDeviceDTO.java)

- `String deviceId`
- `String deviceName`
- `String browser`
- `String userAgent`
- `String status`
- `LocalDateTime firstAccessAt`
- `LocalDateTime lastAccessAt`

## `AdminKeyCreateRequestDTO`

Fonte: [AdminKeyCreateRequestDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AdminKeyCreateRequestDTO.java)

- `keyMaterialHash: String`
- `deviceInstallId: String`

## `AdminKeyStatusDTO`

Fonte: [AdminKeyStatusDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AdminKeyStatusDTO.java)

- `boolean configured`
- `String status`
- `String fingerprint`
- `LocalDateTime createdAt`
- `LocalDateTime revokedAt`

## `AdminLoginRequestDTO`

Fonte: [AdminLoginRequestDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AdminLoginRequestDTO.java)

- `username: String`
- `adminKeyProof: String`
- `deviceId: String`
- `deviceName: String`
- `browser: String`
- `userAgent: String`
- `platform: String`

## `AdminLoginResponseDTO`

Fonte: [AdminLoginResponseDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AdminLoginResponseDTO.java)

- `String status`
- `boolean requiresMobileApproval`
- `UUID attemptId`
- `LocalDateTime expiresAt`
- `String token`
- `String message`

## `AppPinStatusDTO`

Fonte: [AppPinStatusDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/AppPinStatusDTO.java)

- `boolean enabled`
- `boolean configured`
- `boolean locked`
- `int failedAttempts`
- `int remainingAttempts`
- `int maxAttempts`
- `int minPinLength`
- `int maxPinLength`
- `boolean resettableWithTotp`
- `boolean deviceScoped`
- `LocalDateTime lockedUntil`
- `LocalDateTime lastVerifiedAt`
- `LocalDateTime updatedAt`

## `BackupCodesStatusDTO`

Fonte: [BackupCodesStatusDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/BackupCodesStatusDTO.java)

- `boolean enabled`
- `int remainingCodes`
- `List<String> newlyGeneratedCodes`

## `ConfigureAppPinRequestDTO`

Fonte: [ConfigureAppPinRequestDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/ConfigureAppPinRequestDTO.java)

- `enabled: Boolean`
- `pin: String`
- `currentPin: String`
- `totpCode: String`

## `DeviceKeyChallengeResponse`

Fonte: [DeviceKeyChallengeResponse.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/devicekey/DeviceKeyChallengeResponse.java)

- `String challengeId`
- `String challenge`
- `long expiresInSeconds`
- `String onionServiceId`
- `String algorithm`
- `String canonicalization`

## `DeviceKeyDeviceDTO`

Fonte: [DeviceKeyDeviceDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/devicekey/DeviceKeyDeviceDTO.java)

- `String credentialId`
- `String deviceName`
- `String deviceInstallId`
- `String keyStorage`
- `String platform`
- `String browser`
- `String onionServiceId`
- `String status`
- `long counter`
- `LocalDateTime createdAt`
- `LocalDateTime lastUsedAt`
- `LocalDateTime revokedAt`
- `int protocolVersion`

## `DeviceKeyRegistrationRequest`

Fonte: [DeviceKeyRegistrationRequest.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/devicekey/DeviceKeyRegistrationRequest.java)

- `publicKey: String`
- `publicKeySha256: String`
- `credentialId: String`
- `userHandle: String`
- `deviceName: String`
- `deviceInstallId: String`
- `keyStorage: String`
- `platform: String`
- `browser: String`
- `brand: String`
- `model: String`
- `serialNumber: String`
- `signedPayload: String`
- `signature: String`

## `DeviceKeyVerifyRequest`

Fonte: [DeviceKeyVerifyRequest.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/devicekey/DeviceKeyVerifyRequest.java)

- `username: String`
- `credentialId: String`
- `deviceInstallId: String`
- `signedPayload: String`
- `signature: String`

## `DeviceTokenRegisterRequest`

Fonte: [DeviceTokenRegisterRequest.java](../../../auth-service/src/main/java/com/kerosene/notification/dto/DeviceTokenRegisterRequest.java)

- `String platform`
- `String token`
- `String deviceId`
- `String appVersion`

## `DeviceTokenResponse`

Fonte: [DeviceTokenResponse.java](../../../auth-service/src/main/java/com/kerosene/notification/dto/DeviceTokenResponse.java)

- `Long id`
- `String platform`
- `String tokenRef`
- `String deviceRef`
- `String appVersion`
- `LocalDateTime createdAt`
- `LocalDateTime lastSeenAt`
- `LocalDateTime revokedAt`
- `boolean active`

## `EmergencyRecoveryFinishRequest`

Fonte: [EmergencyRecoveryFinishRequest.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/EmergencyRecoveryFinishRequest.java)

- `recoverySessionId: String`
- `totpCode: String`
- `publicKey: String`
- `publicKeyCose: String`
- `deviceName: String`
- `signature: String`
- `authData: String`
- `clientDataJSON: String`
- `credentialId: String`
- `userHandle: String`

## `EmergencyRecoveryFinishResponse`

Fonte: [EmergencyRecoveryFinishResponse.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/EmergencyRecoveryFinishResponse.java)

- `username: String`
- `newBackupCodes: List<String>`

## `EmergencyRecoveryStartRequest`

Fonte: [EmergencyRecoveryStartRequest.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/EmergencyRecoveryStartRequest.java)

- `username: String`
- `recoveryCodes: List<String>`
- `challenge: String`
- `nonce: String`

## `EmergencyRecoveryStartResponse`

Fonte: [EmergencyRecoveryStartResponse.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/EmergencyRecoveryStartResponse.java)

- `recoverySessionId: String`
- `otpUri: String`
- `passkeyChallenge: String`
- `expiresInSeconds: long`
- `requiredRecoveryCodes: int`

## `KfeAddressResponse`

Fonte: [KfeAddressResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeAddressResponse.java)

- `UUID id`
- `UUID walletId`
- `String address`
- `KfeWalletAddressRole role`
- `KfeWalletAddressStatus status`
- `String derivationPath`
- `Integer derivationIndex`
- `String providerReference`
- `LocalDateTime createdAt`
- `LocalDateTime retiredAt`

## `KfeAuditEventResponse`

Fonte: [KfeAuditEventResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeAuditEventResponse.java)

- `Long sequenceNumber`
- `UUID id`
- `UUID transactionId`
- `UUID walletId`
- `String eventType`
- `String fromStatus`
- `String toStatus`
- `String payloadHash`
- `String previousHash`
- `String eventHash`
- `LocalDateTime createdAt`

## `KfeAuditLatestResponse`

Fonte: [KfeAuditLatestResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeAuditLatestResponse.java)

- `KfeAuditEventResponse latestEvent`
- `KfeAuditRootResponse root`

## `KfeAuditRootResponse`

Fonte: [KfeAuditRootResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeAuditRootResponse.java)

- `String merkleRoot`
- `long eventCount`
- `Long fromSequence`
- `Long toSequence`
- `LocalDateTime generatedAt`

## `KfeColdWalletPsbtRequest`

Fonte: [KfeColdWalletPsbtRequest.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeColdWalletPsbtRequest.java)

- `@NotBlank @Size(max = 128) String destinationAddress`
- `@Min(546) long amountSats`
- `@Min(1) Integer confirmationTarget`
- `@Min(1) Long feeRateSatsPerVbyte`
- `@Valid List<Input> inputs`

## `KfeColdWalletPsbtResponse`

Fonte: [KfeColdWalletPsbtResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeColdWalletPsbtResponse.java)

- `String psbt`
- `String psbtHash`
- `long feeSats`
- `long amountSats`
- `String destinationAddress`
- `List<KfeColdWalletPsbtRequest.Input> inputs`

## `KfeCreateWalletRequest`

Fonte: [KfeCreateWalletRequest.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeCreateWalletRequest.java)

- `@NotNull KfeWalletKind kind`
- `KfeWalletName name`
- `@Size(max = 96) String label`
- `String xpub`
- `String descriptor`
- `String fingerprint`
- `String derivationPath`
- `String initialAddress`
- `String initialAddressDerivationPath`
- `Integer initialAddressDerivationIndex`
- `String initialAddressProviderReference`
- `Boolean issueInitialAddress`

## `KfeDashboardResponse`

Fonte: [KfeDashboardResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeDashboardResponse.java)

- `List<KfeDashboardWallet> wallets`
- `List<KfeStatementItem> recentStatement`
- `long totalSpendableSats`
- `long totalObservedSats`
- `long totalVisibleSats`

## `KfeReceivingCapabilitiesResponse`

Fonte: [KfeReceivingCapabilitiesResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeReceivingCapabilitiesResponse.java)

- `boolean canReceiveInternal`
- `boolean canReceiveLightning`
- `boolean canReceiveOnchain`
- `String preferredRail`
- `List<String> missingRequirements`
- `String receiverDisplayName`
- `UUID internalWalletId`
- `List<String> availableRails`
- `Limits limits`

## `KfeSubmitTransactionRequest`

Fonte: [KfeSubmitTransactionRequest.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeSubmitTransactionRequest.java)

- `@NotBlank String idempotencyKey`
- `@NotNull KfeRail rail`
- `@NotNull KfeDirection direction`
- `UUID sourceWalletId`
- `UUID destinationWalletId`
- `@Min(1) long amountSats`
- `@Min(0) long networkFeeSats`
- `String externalReference`
- `String memo`
- `String totpCode`
- `String passkeyAssertionJson`
- `String confirmationPassphrase`

## `KfeTransactionResponse`

Fonte: [KfeTransactionResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeTransactionResponse.java)

- `UUID id`
- `KfeTransactionStatus status`
- `KfeRail rail`
- `KfeDirection direction`
- `UUID sourceWalletId`
- `UUID destinationWalletId`
- `long grossAmountSats`
- `long receiverAmountSats`
- `long networkFeeSats`
- `long keroseneFeeSats`
- `long totalDebitSats`
- `String quorumProposalHash`
- `int quorumAckCount`
- `String providerReference`
- `String blockchainTxid`
- `String failureCode`
- `String failureMessage`
- `LocalDateTime createdAt`
- `LocalDateTime updatedAt`

## `KfeUtxoResponse`

Fonte: [KfeUtxoResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeUtxoResponse.java)

- `String txid`
- `int vout`
- `long valueSats`
- `String scriptPubKey`
- `String address`

## `KfeWalletNameOption`

Fonte: [KfeWalletNameOption.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeWalletNameOption.java)

- `KfeWalletName name`
- `String label`

## `KfeWalletResponse`

Fonte: [KfeWalletResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeWalletResponse.java)

- `UUID id`
- `KfeWalletKind kind`
- `KfeWalletStatus status`
- `String label`
- `String walletName`
- `String walletTypeDescription`
- `String asset`
- `boolean spendable`
- `boolean xpubConfigured`
- `boolean mpcKeyConfigured`
- `String activeAddress`
- `LocalDateTime createdAt`
- `LocalDateTime updatedAt`

<!-- Mining DTOs removed: MiningAllocationRequestDTO, MiningAllocationResponseDTO, MiningRigOfferDTO — controller and module removed from codebase -->
- `BigDecimal availableHashrate`
- `BigDecimal pricePerUnitDayBtc`
- `BigDecimal projectedBtcYieldPerUnitDay`
- `Integer minRentalHours`
- `Integer maxRentalHours`
- `String provider`

## `PasskeyInventoryDTO`

Fonte: [PasskeyInventoryDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/PasskeyInventoryDTO.java)

- `boolean passkeyRegistered`
- `boolean compatibleForCurrentLogin`
- `boolean legacyCredentialsPresent`
- `String currentRelyingPartyId`
- `String currentHost`
- `List<PasskeyDeviceDTO> devices`

## `PasskeyRegistrationRequest`

Fonte: [PasskeyRegistrationRequest.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/passkey/PasskeyRegistrationRequest.java)

- `publicKey: String`
- `deviceName: String`
- `signature: String`
- `authData: String`
- `clientDataJSON: String`
- `credentialId: String`
- `userHandle: String`
- `publicKeyCose: String`
- `brand: String`
- `model: String`
- `serialNumber: String`
- `deviceInstallId: String`
- `platform: String`
- `browser: String`
- `status: String`

## `PasskeyVerifyRequest`

Fonte: [PasskeyVerifyRequest.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/passkey/PasskeyVerifyRequest.java)

- `username: String`
- `signature: String`
- `authData: String`
- `clientDataJSON: String`
- `credentialId: String`

## `SignupResponseDTO`

Fonte: [SignupResponseDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/SignupResponseDTO.java)

- `sessionId: String`
- `otpUri: String`
- `backupCodes: List<String>`
- `totpOptional: boolean`

## `SignupTotpVerifyRequestDTO`

Fonte: [SignupTotpVerifyRequestDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/SignupTotpVerifyRequestDTO.java)

- `sessionId: String`
- `totpCode: String`

## `TotpSetupResponseDTO`

Fonte: [TotpSetupResponseDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/TotpSetupResponseDTO.java)

- `String otpUri`
- `String secret`

## `KfeReserveOverviewResponse`

Fonte: [KfeReserveOverviewResponse.java](https://github.com/Daniel-Astrofer/kerosene-kfe/blob/main/src/main/java/com/kerosene/kfe/dto/KfeReserveOverviewResponse.java)

- `BigDecimal totalOnchainBtc`
- `BigDecimal lightningNodeBtc`
- `BigDecimal inboundLiquidityBtc`
- `BigDecimal outboundLiquidityBtc`
- `BigDecimal reservedOnchainBtc`
- `BigDecimal reservedLightningBtc`
- `BigDecimal availableOnchainBtc`
- `BigDecimal availableLightningBtc`
- `boolean lightningSendsAllowed`
- `String liquidityState`

## `UserDTO`

Fonte: [UserDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/UserDTO.java)

- `username: String`
- `totpSecret: String`
- `totpCode: String`
- `voucherCode: String`
- `challenge: String`
- `nonce: String`
- `preAuthToken: String`
- `sessionId: String`
- `accountSecurity: AccountSecurityType`
- `shamirTotalShares: Integer`
- `shamirThreshold: Integer`
- `multisigThreshold: Integer`
- `backupCodes: java.util.List<String>`

## `VerifyAppPinRequestDTO`

Fonte: [VerifyAppPinRequestDTO.java](../../../auth-service/src/main/java/com/kerosene/auth/dto/VerifyAppPinRequestDTO.java)

- `pin: String`
