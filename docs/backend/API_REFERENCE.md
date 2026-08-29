# Backend API Reference

HTTP endpoint inventory. Ops docs: [`api/`](api/README.md). Finance: KFE-only (`/kfe/**`, `/api/admin/kfe/**`) → [`api/KFE.md`](api/KFE.md).

## Domain docs

| Domain | Doc |
| --- | --- |
| Auth/session/step-up | [`api/AUTH.md`](api/AUTH.md) |
| KFE finance | [`api/KFE.md`](api/KFE.md) |
| KFE audit | [`api/AUDIT.md`](api/AUDIT.md) |
| Sovereignty/quorum | [`api/SOVEREIGNTY.md`](api/SOVEREIGNTY.md) |
| Notifications | [`api/NOTIFICATIONS.md`](api/NOTIFICATIONS.md) |
| Public/health/web | [`api/PUBLIC_HEALTH_WEB.md`](api/PUBLIC_HEALTH_WEB.md) |
| Integrations | [`api/INTEGRATIONS.md`](api/INTEGRATIONS.md) |
| DTO index | [`api/DTO_SCHEMA_INDEX.md`](api/DTO_SCHEMA_INDEX.md) |

Removed wallet, payments and mining APIs are not active contracts and no
longer appear as links in this index. Current financial routes are documented
under KFE.

## Scope

- ~91 HTTP sections (~90 unique method/path; `GET /` JSON+HTML).
- Error envelope: `ApiResponse` `{success,message,errorCode,data?,timestamp}`. Filters may return bare errors on `413`/`415`/some `401`/`403`.

## Global HTTP

- Body methods need `Content-Type: application/json` (except HTML).
- Paranoid body limit `2048` B; PSBT up to `64 KiB`.
- Optional `Digest: SHA-256=<base64>` must match body if set.
- `Authorization: Bearer <jwt>` on protected routes.
- Near-expiry JWT may renew via `X-New-Token`.
- CORS: `Authorization`, `Content-Type`, `Digest`, `X-Correlation-Id`, `X-Request-Id`, `X-Idempotency-Key`, `Idempotency-Key`, `X-Admin-Token`, `X-Owner-TOTP`, `X-Hardware-Signature`, release attestation headers, `X-Device-Hash`.

## Endpoints

### Public Web and Health

#### `GET /`
- `RootStatusController.root` · auth `Publico` · → `Map<String, Object>`
- src: `auth-service/src/main/java/com/kerosene/common/controller/RootStatusController.java#L21`
- headers: `Accept` (opt)
- resp keys: `status`, `service`, `region`, `timestamp`, `health`, `liveness`, `sovereignty`

#### `GET /`
- `WebAdminController.index` · auth `Publico` · → `text/html forward to /index.html`
- src: `auth-service/src/main/java/com/kerosene/common/controller/WebAdminController.java#L10`
- headers: `Accept` (opt)
- resp keys: `text/html`

#### `GET /admin`
- `WebAdminController.webRoutes` · auth `Publico` · → `text/html forward to /index.html`
- src: `auth-service/src/main/java/com/kerosene/common/controller/WebAdminController.java#L15`
- headers: `Accept` (opt)
- resp keys: `text/html`

#### `GET /admin/**`
- `WebAdminController.webRoutes` · auth `Publico` · → `text/html forward to /index.html`
- src: `auth-service/src/main/java/com/kerosene/common/controller/WebAdminController.java#L15`
- headers: `Accept` (opt)
- resp keys: `text/html`

#### `GET /bitcoin-banking`
- `WebAdminController.webRoutes` · auth `Publico` · → `text/html forward to /index.html`
- src: `auth-service/src/main/java/com/kerosene/common/controller/WebAdminController.java#L15`
- headers: `Accept` (opt)
- resp keys: `text/html`

#### `GET /bitcoin-banking/**`
- `WebAdminController.webRoutes` · auth `Publico` · → `text/html forward to /index.html`
- src: `auth-service/src/main/java/com/kerosene/common/controller/WebAdminController.java#L15`
- headers: `Accept` (opt)
- resp keys: `text/html`

#### `GET /download`
- `WebAdminController.webRoutes` · auth `Publico` · → `text/html forward to /index.html`
- src: `auth-service/src/main/java/com/kerosene/common/controller/WebAdminController.java#L15`
- headers: `Accept` (opt)
- resp keys: `text/html`

#### `GET /health/dependencies`
- `HealthController.dependencies` · auth `JWT` · → `ResponseEntity<OperationalHealthSnapshot>`
- src: `auth-service/src/main/java/com/kerosene/common/controller/HealthController.java#L29`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `status`, `service`, `region`, `timestamp`, `checks`

#### `GET /health/live`
- `HealthController.live` · auth `Publico` · → `ResponseEntity<OperationalHealthSnapshot>`
- src: `auth-service/src/main/java/com/kerosene/common/controller/HealthController.java#L19`
- headers: `Accept` (opt)
- resp keys: `status`, `service`, `region`, `timestamp`, `checks`

#### `GET /health/ready`
- `HealthController.ready` · auth `Publico` · → `ResponseEntity<OperationalHealthSnapshot>`
- src: `auth-service/src/main/java/com/kerosene/common/controller/HealthController.java#L24`
- headers: `Accept` (opt)
- resp keys: `status`, `service`, `region`, `timestamp`, `checks`

#### `GET /healthz`
- `RootStatusController.healthz` · auth `Publico` · → `Map<String, Object>`
- src: `auth-service/src/main/java/com/kerosene/common/controller/RootStatusController.java#L26`
- headers: `Accept` (opt)
- resp keys: `status`, `service`, `region`, `timestamp`, `health`, `liveness`, `sovereignty`

#### `GET /status`
- `WebAdminController.webRoutes` · auth `Publico` · → `text/html forward to /index.html`
- src: `auth-service/src/main/java/com/kerosene/common/controller/WebAdminController.java#L15`
- headers: `Accept` (opt)
- resp keys: `text/html`

#### `GET /system/release`
- `SystemReleaseController.release` · auth `Publico` · → `ReleaseManifestService.ReleaseSnapshot`
- src: `auth-service/src/main/java/com/kerosene/common/admin/SystemReleaseController.java#L16`
- headers: `Accept` (opt)
- resp keys: `service`, `version`, `gitCommit`, `buildTime`, `imageDigest`, `codeHash`, `configHash`, `manifestDigest`, `manifestSignatureValid`, `authorized`, `reason`, `message`, `runtime`, `manifestService`

### Public/Admin API

#### `GET /api/admin/operations/blockchain`
- `AdminOperationsController.blockchain` · auth `JWT com ROLE_ADMIN` · → `BitcoinBlockchainMonitorService.BlockchainMonitorSnapshot`
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java#L81`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `status`, `primarySource`, `network`, `indexer`, `localIndexerConfigured`, `checkedAt`, `chain`, `mempool`, `relevantTransactions`, `message`

#### `GET /api/admin/operations/health`
- `AdminOperationsController.health` · auth `JWT com ROLE_ADMIN` · → `OperationalHealthSnapshot`
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java#L76`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `status`, `service`, `region`, `timestamp`, `checks`

#### `GET /api/admin/operations/lightning`
- `AdminOperationsController.lightning` · auth `JWT com ROLE_ADMIN` · → `LightningNetworkMonitorService.LightningMonitorSnapshot`
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java#L86`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `status`, `primarySource`, `checkedAt`, `node`, `message`

#### `GET /api/admin/operations/logs`
- `AdminOperationsController.logs` · auth `JWT com ROLE_ADMIN` · → `List<Map<String, Object>>`
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java#L106`
- headers: `Accept` (opt), `Authorization` (req)
- query: 50 (int, false)
- resp keys: `array`

#### `GET /api/admin/operations/metrics`
- `AdminOperationsController.metrics` · auth `JWT com ROLE_ADMIN` · → `Map<String, Object>`
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java#L116`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `checkedAt`, `totalVolumeBtc`, `totalFeesBtc`, `totalTransactions`, `avgTicketBtc`, `confirmedTransactions`, `pendingTransactions`, `failedTransactions`, `transfers`, `paymentLinks`, `privacyBoundary`

#### `GET /api/admin/operations/mobile`
- `AdminOperationsController.mobile` · auth `JWT com ROLE_ADMIN` · → `MobileDownloadService.MobileReleaseInfo`
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java#L101`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `version`, `buildNumber`, `artifacts`, `changelog`, `generatedAt`, `integrityInstructions`

#### `GET /api/admin/operations/overview`
- `AdminOperationsController.overview` · auth `JWT com ROLE_ADMIN` · → `Map<String, Object>`
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java#L63`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `checkedAt`, `health`, `blockchain`, `lightning`, `vaultMesh`, `release`, `mobile`

#### `GET /api/admin/operations/release`
- `AdminOperationsController.release` · auth `JWT com ROLE_ADMIN` · → `ReleaseManifestService.ReleaseSnapshot`
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java#L96`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `service`, `version`, `gitCommit`, `buildTime`, `imageDigest`, `codeHash`, `configHash`, `manifestDigest`, `manifestSignatureValid`, `authorized`, `reason`, `message`, `runtime`, `manifestService`

#### `GET /api/admin/operations/vault-mesh`
- `AdminOperationsController.vaultMesh` · auth `JWT com ROLE_ADMIN` · → `Map` (vault mesh health snapshot)
- src: `auth-service/src/main/java/com/kerosene/common/admin/AdminOperationsController.java`
- headers: `Accept` (opt), `Authorization` (req)
- notes: probes kerosene-vault `GET /v1/health` (node_tier / attestation_mode / tee_available). HashiCorp Vault Raft admin route removed; treasury governance is vault-mesh only; mpc-sidecar not primary signer

#### `GET /api/public/mobile-download`
- `PublicSiteController.mobileDownload` · auth `Publico` · → `MobileDownloadService.MobileReleaseInfo`
- src: `auth-service/src/main/java/com/kerosene/common/admin/PublicSiteController.java#L17`
- headers: `Accept` (opt)
- resp keys: `version`, `buildNumber`, `artifacts`, `changelog`, `generatedAt`, `integrityInstructions`

### Auth and Account

#### `GET /auth/activation-status`
- `AccountActivationController.getStatus` · auth `JWT` · → `ResponseEntity<ApiResponse<AccountActivationStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AccountActivationController.java#L27`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/activation-status/deposit-link`
- `AccountActivationController.createDepositLink` · auth `JWT` · → `ResponseEntity<ApiResponse<AccountActivationStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AccountActivationController.java#L33`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/activation-status/{linkId}/confirm`
- `AccountActivationController.confirm` · auth `JWT` · → `ResponseEntity<ApiResponse<AccountActivationStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AccountActivationController.java#L42`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- path: `linkId (string, yes)`
- body keys: `txid`, `fromAddress`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/admin/access-attempts/pending`
- `AdminAccessController.pendingAttempts` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<List<AdminAccessAttemptDTO>>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L93`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/admin/access-attempts/{attemptId}/decision`
- `AdminAccessController.decide` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<AdminAccessAttemptDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L101`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- path: `attemptId (string, yes)`
- body keys: `decision`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/admin/devices`
- `AdminAccessController.devices` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<List<AdminAuthenticatedDeviceDTO>>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L113`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/admin/devices/{deviceId}/block`
- `AdminAccessController.blockDevice` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<AdminAuthenticatedDeviceDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L121`
- headers: `Accept` (opt), `Authorization` (req)
- path: `deviceId (string, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/admin/devices/{deviceId}/revoke`
- `AdminAccessController.revokeDevice` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<AdminAuthenticatedDeviceDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L132`
- headers: `Accept` (opt), `Authorization` (req)
- path: `deviceId (string, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `DELETE /auth/admin/key`
- `AdminAccessController.revokeKey` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<AdminKeyStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L85`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/admin/key`
- `AdminAccessController.keyStatus` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<AdminKeyStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L77`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/admin/key`
- `AdminAccessController.createOrRotateKey` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<AdminKeyStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L69`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- body keys: `keyMaterialHash`, `deviceInstallId`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/admin/login`
- `AdminAccessController.startLogin` · auth `Publico` · → `ResponseEntity<ApiResponse<AdminLoginResponseDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L40`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- body keys: `username`, `password`, `adminKeyProof`, `deviceId`, `deviceName`, `browser`, `userAgent`, `platform`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/admin/login/{attemptId}`
- `AdminAccessController.pollLogin` · auth `JWT` · → `ResponseEntity<ApiResponse<AdminLoginResponseDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AdminAccessController.java#L57`
- headers: `Accept` (opt), `Authorization` (req)
- path: `attemptId (string, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/backup-codes`
- `BackupCodesController.getStatus` · auth `JWT` · → `ResponseEntity<ApiResponse<BackupCodesStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/BackupCodesController.java#L23`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/backup-codes/regenerate`
- `BackupCodesController.regenerate` · auth `JWT` · → `ResponseEntity<ApiResponse<BackupCodesStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/BackupCodesController.java#L29`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/login`
- `UserController.login` · auth `Publico` · → `ResponseEntity<ApiResponse<String>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/UserController.java#L40`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- body keys: `username`, `password`, `challenge`, `nonce`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/login/totp/verify`
- `UserController.loginTotpVerify` · auth `Publico` · → `ResponseEntity<ApiResponse<String>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/UserController.java#L62`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- body keys: `preAuthToken`, `totpCode`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/me`
- `MeController.getCurrentUser` · auth `JWT` · → `ResponseEntity<ApiResponse<Map<String, Object>>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/MeController.java#L31`
- headers: `Accept` (opt), `Authorization` (req), `X-Device-Hash` (opt)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/passkey/challenge`
- `PasskeyController.getChallenge` · auth `Publico` · → `ResponseEntity<ApiResponse<String>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/PasskeyController.java#L68`
- headers: `Accept` (opt)
- query: username (String, true)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/passkey/devices`
- `PasskeyController.getRegisteredDevices` · auth `JWT` · → `ResponseEntity<ApiResponse<PasskeyInventoryDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/PasskeyController.java#L74`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/passkey/devices/{deviceInstallId}/block`
- `PasskeyController.blockDevice` · auth `JWT` · → `ResponseEntity<ApiResponse<PasskeyInventoryDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/PasskeyController.java#L183`
- headers: `Accept` (opt), `Authorization` (req)
- path: `deviceInstallId (string, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/passkey/devices/{deviceInstallId}/revoke`
- `PasskeyController.revokeDevice` · auth `JWT` · → `ResponseEntity<ApiResponse<PasskeyInventoryDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/PasskeyController.java#L188`
- headers: `Accept` (opt), `Authorization` (req)
- path: `deviceInstallId (string, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/passkey/onboarding/finish`
- `PasskeyController.finishOnboardingRegistration` · auth `Publico` · → `ResponseEntity<ApiResponse<String>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/PasskeyController.java#L363`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- query: sessionId (String, true)
- body keys: `publicKey`, `deviceName`, `signature`, `authData`, `clientDataJSON`, `credentialId`, `userHandle`, `publicKeyCose`, `brand`, `model`, `serialNumber`, `deviceInstallId`, `platform`, `browser`, `status`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/passkey/onboarding/start`
- `PasskeyController.startOnboardingRegistration` · auth `Publico` · → `ResponseEntity<ApiResponse<String>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/PasskeyController.java#L351`
- headers: `Accept` (opt)
- query: sessionId (String, true)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/passkey/register`
- `PasskeyController.registerPasskey` · auth `JWT` · → `ResponseEntity<ApiResponse<String>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/PasskeyController.java#L96`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- body keys: `publicKey`, `deviceName`, `signature`, `authData`, `clientDataJSON`, `credentialId`, `userHandle`, `publicKeyCose`, `brand`, `model`, `serialNumber`, `deviceInstallId`, `platform`, `browser`, `status`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/passkey/verify`
- `PasskeyController.verifyAndLogin` · auth `Publico` · → `ResponseEntity<ApiResponse<Object>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/PasskeyController.java#L196`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- body keys: `username`, `signature`, `authData`, `clientDataJSON`, `credentialId`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/pow/challenge`
- `UserController.getPowChallenge` · auth `Publico` · → `ResponseEntity<ApiResponse<Map<String, String>>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/UserController.java#L34`
- headers: `Accept` (opt)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/recovery/emergency/finish`
- `EmergencyRecoveryController.finish` · auth `Publico` · → `ResponseEntity<ApiResponse<EmergencyRecoveryFinishResponse>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/EmergencyRecoveryController.java#L52`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- body keys: `recoverySessionId`, `totpCode`, `publicKey`, `publicKeyCose`, `deviceName`, `signature`, `authData`, `clientDataJSON`, `credentialId`, `userHandle`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/recovery/emergency/start`
- `EmergencyRecoveryController.start` · auth `Publico` · → `ResponseEntity<ApiResponse<EmergencyRecoveryStartResponse>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/EmergencyRecoveryController.java#L28`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- body keys: `username`, `newPassphrase`, `recoveryCodes`, `challenge`, `nonce`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/security-status`
- `AccountSecurityStatusController.getStatus` · auth `JWT` · → `ResponseEntity<ApiResponse<AccountSecurityStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AccountSecurityStatusController.java#L22`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/security/app-pin`
- `AppPinController.getStatus` · auth `JWT` · → `ResponseEntity<ApiResponse<AppPinStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AppPinController.java#L28`
- headers: `Accept` (opt), `Authorization` (req), `X-Device-Hash` (opt)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `PUT /auth/security/app-pin`
- `AppPinController.configure` · auth `JWT` · → `ResponseEntity<ApiResponse<AppPinStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AppPinController.java#L36`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt), `X-Device-Hash` (opt)
- body keys: `enabled`, `pin`, `currentPin`, `totpCode`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/security/app-pin/verify`
- `AppPinController.verify` · auth `JWT` · → `ResponseEntity<ApiResponse<AppPinStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AppPinController.java#L45`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt), `X-Device-Hash` (opt)
- body keys: `pin`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /auth/security/profile`
- `AccountSecurityController.getProfile` · auth `JWT` · → `ResponseEntity<ApiResponse<AccountSecurityProfileDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AccountSecurityController.java#L46`
- headers: `Accept` (opt), `Authorization` (req), `X-Device-Hash` (opt)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `PUT /auth/security/profile`
- `AccountSecurityController.updateProfile` · auth `JWT` · → `ResponseEntity<ApiResponse<AccountSecurityProfileDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/AccountSecurityController.java#L61`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt), `X-Device-Hash` (opt)
- body keys: `accountSecurity`, `shamirTotalShares`, `shamirThreshold`, `multisigThreshold`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/signup`
- `UserController.signup` · auth `Publico` · → `ResponseEntity<ApiResponse<SignupResponseDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/UserController.java#L47`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- body keys: `username`, `password`, `challenge`, `nonce`, `accountSecurity`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/signup/totp/verify`
- `UserController.totpCodeVerify` · auth `Publico` · → `ResponseEntity<ApiResponse<String>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/UserController.java#L54`
- headers: `Accept` (opt), `Content-Type` (req), `Digest` (opt)
- body keys: `sessionId`, `totpCode`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `DELETE /auth/totp`
- `TotpController.disable` · auth `JWT` · → `ResponseEntity<ApiResponse<String>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/TotpController.java#L44`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/totp/setup`
- `TotpController.setup` · auth `JWT` · → `ResponseEntity<ApiResponse<TotpSetupResponseDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/TotpController.java#L27`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /auth/totp/verify`
- `TotpController.verify` · auth `JWT` · → `ResponseEntity<ApiResponse<BackupCodesStatusDTO>>`
- src: `auth-service/src/main/java/com/kerosene/auth/controller/TotpController.java#L34`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- body keys: `totpCode`
- resp keys: `success`, `message`, `data`, `timestamp`

### KFE

#### `GET /api/admin/kfe/audit/latest`
- `KfeAuditAdminController.latest` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<KfeAuditLatestResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeAuditAdminController.java:31`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /api/admin/kfe/audit/events`
- `KfeAuditAdminController.events` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<List<KfeAuditEventResponse>>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeAuditAdminController.java:36`
- headers: `Accept` (opt), `Authorization` (req)
- query: limit (int, no)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /api/admin/kfe/audit/transactions/{transactionId}`
- `KfeAuditAdminController.transactionEvents` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<List<KfeAuditEventResponse>>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeAuditAdminController.java:42`
- headers: `Accept` (opt), `Authorization` (req)
- path: `transactionId (UUID, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /api/admin/kfe/audit/root`
- `KfeAuditAdminController.root` · auth `JWT com ROLE_ADMIN` · → `ResponseEntity<ApiResponse<KfeAuditRootResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeAuditAdminController.java:50`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /kfe/dashboard`
- `KfeDashboardController.dashboard` · auth `JWT` · → `ResponseEntity<ApiResponse<KfeDashboardResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeDashboardController.java:22`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /kfe/users/{receiverIdentifier}/receiving-capabilities`
- `KfeReceivingController.capabilities` · auth `JWT` · → `ResponseEntity<ApiResponse<KfeReceivingCapabilitiesResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeReceivingController.java:22`
- headers: `Accept` (opt), `Authorization` (req)
- path: `receiverIdentifier (string, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /kfe/transactions`
- `KfeTransactionController.submit` · auth `JWT` · → `ResponseEntity<ApiResponse<KfeTransactionResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeTransactionController.java:38`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- body keys: `(see controller)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /kfe/transactions/{transactionId}`
- `KfeTransactionController.get` · auth `JWT` · → `ResponseEntity<ApiResponse<KfeTransactionResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeTransactionController.java:53`
- headers: `Accept` (opt), `Authorization` (req)
- path: `transactionId (UUID, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /kfe/wallets`
- `KfeWalletController.create` · auth `JWT` · → `ResponseEntity<ApiResponse<KfeWalletResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeWalletController.java:40`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- body keys: `(see controller)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /kfe/wallets`
- `KfeWalletController.list` · auth `JWT` · → `ResponseEntity<ApiResponse<List<KfeWalletResponse>>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeWalletController.java:49`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /kfe/wallets/{walletId}/addresses/rotate`
- `KfeWalletController.rotateAddress` · auth `JWT` · → `ResponseEntity<ApiResponse<KfeAddressResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeWalletController.java:56`
- headers: `Accept` (opt), `Authorization` (req)
- path: `walletId (UUID, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `GET /kfe/wallets/{walletId}/utxos`
- `KfeWalletController.listUtxos` · auth `JWT` · → `ResponseEntity<ApiResponse<List<KfeUtxoResponse>>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeWalletController.java:65`
- headers: `Accept` (opt), `Authorization` (req)
- path: `walletId (UUID, yes)`
- resp keys: `success`, `message`, `data`, `timestamp`

#### `POST /kfe/wallets/{walletId}/cold-wallet/psbt`
- `KfeWalletController.createColdWalletPsbt` · auth `JWT` · → `ResponseEntity<ApiResponse<KfeColdWalletPsbtResponse>>`
- src: `kerosene-kfe/src/main/java/com/kerosene/kfe/controller/KfeWalletController.java:74`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- path: `walletId (UUID, yes)`
- body keys: `(see controller)`
- resp keys: `success`, `message`, `data`, `timestamp`

### KFE Reserve Overview

#### `GET /api/admin/kfe/reserves/overview`
- `KfeReserveAdminController.overview` · auth `JWT com ROLE_ADMIN` · → `ApiResponse<KfeReserveOverviewResponse>`
- resp keys: `success`, `message`, `data`

### Mining (STALE — controller removed)

All Mining endpoints (`/mining/**`) and `MiningController` have been removed from the codebase. Do not implement new clients against this API.

### Notifications

#### `GET /notifications`
- `NotificationController.getNotifications` · auth `JWT` · → `ResponseEntity<List<NotificationEntity>>`
- src: `auth-service/src/main/java/com/kerosene/notification/controller/NotificationController.java#L34`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `array`

#### `GET /notifications/device-tokens`
- `NotificationController.activeDeviceTokens` · auth `JWT` · → `ResponseEntity<List<DeviceTokenResponse>>`
- src: `auth-service/src/main/java/com/kerosene/notification/controller/NotificationController.java#L52`
- headers: `Accept` (opt), `Authorization` (req)
- resp keys: `array`

#### `DELETE /notifications/device-tokens/{id}`
- `NotificationController.revokeToken` · auth `JWT` · → `ResponseEntity<Void>`
- src: `auth-service/src/main/java/com/kerosene/notification/controller/NotificationController.java#L59`
- headers: `Accept` (opt), `Authorization` (req)
- path: `id (string, yes)`

#### `POST /notifications/register-token`
- `NotificationController.registerToken` · auth `JWT` · → `ResponseEntity<DeviceTokenResponse>`
- src: `auth-service/src/main/java/com/kerosene/notification/controller/NotificationController.java#L45`
- headers: `Accept` (opt), `Authorization` (req), `Content-Type` (req), `Digest` (opt)
- body keys: `platform`, `token`, `deviceId`, `appVersion`
- resp keys: `id`, `platform`, `tokenRef`, `deviceRef`, `appVersion`, `createdAt`, `lastSeenAt`, `revokedAt`, `active`

#### `PUT /notifications/{id}/read`
- `NotificationController.markAsRead` · auth `JWT` · → `ResponseEntity<Void>`
- src: `auth-service/src/main/java/com/kerosene/notification/controller/NotificationController.java#L39`
- headers: `Accept` (opt), `Authorization` (req)
- path: `id (string, yes)`

### Sovereignty

#### `GET /sovereignty/ping`
- `SovereigntyStatusController.ping` · auth `Publico` · → `String`
- src: `auth-service/src/main/java/com/kerosene/security/SovereigntyStatusController.java#L199`
- headers: `Accept` (opt)
- resp keys: `text/html`

#### `POST /sovereignty/reattest`
- `SovereigntyStatusController.reAttestNode` · auth `JWT` · → `ResponseEntity<Map<String, String>>`
- src: `auth-service/src/main/java/com/kerosene/security/SovereigntyStatusController.java#L142`
- headers: `Accept` (opt), `Authorization` (req), `X-Admin-Token` (opt)
- resp keys: `message`

#### `GET /sovereignty/status`
- `SovereigntyStatusController.getSovereigntyStatus` · auth `Publico` · → `Map<String, Object>`
- src: `auth-service/src/main/java/com/kerosene/security/SovereigntyStatusController.java#L55`
- headers: `Accept` (opt)
- resp keys: `hardwareAttestation`, `networkConsensus`, `ledgerIntegrity`, `memoryProtection`, `serverUptimeSeconds`, `serverTimestamp`

#### `GET /sovereignty/telemetry`
- `SovereigntyStatusController.getTelemetry` · auth `JWT` · → `ResponseEntity<Map<String, Object>>`
- src: `auth-service/src/main/java/com/kerosene/security/SovereigntyStatusController.java#L173`
- headers: `Accept` (opt), `Authorization` (req), `X-Admin-Token` (opt)
- resp keys: `snapshotAt`, `storage`, `counters`, `recentEvents`

## WebSocket/STOMP

- | --- | --- | --- | --- |
- | `/ws/balance` | SockJS/STOMP para app autenticado | Header STOMP `Authorization: Bearer <jwt>` no `CONNECT` | `/user/queue/balance`, `/user/queue/notifications` |
- | `/ws/raw-balance` | WebSocket raw alternativo | configurado sem SockJS | uso interno/diagnostico |
- | `/ws/payment-request` | Eventos de payment request | Header STOMP `Authorization: Bearer <jwt>` no `CONNECT` | filas de usuario/autorizadas pelo interceptor |
- | `/ws/raw-payment-request` | Variante raw | configurado sem SockJS | uso interno/diagnostico |
- A autenticacao HTTP de `/ws/**` e liberada para permitir upgrade; a validacao real acontece no interceptor STOMP de `CONNECT`.

## DTOs

Field lists only. Full schemas: domain docs + `api/DTO_SCHEMA_INDEX.md`.

### `AccountActivationStatusDTO`
- `activated` `boolean`, `canReceiveInbound` `boolean`, `requiresActivationDeposit` `boolean`, `requiredAmountBtc` `BigDecimal`, `paymentLinkId` `String`, `depositAddress` `String`, `paymentStatus` `String`, `warningMessage` `String`, `activatedAt` `LocalDateTime`

### `AccountSecurityProfileDTO`
- `accountSecurity` `AccountSecurityType`, `shamirTotalShares` `Integer`, `shamirThreshold` `Integer`, `multisigThreshold` `Integer`, `passkeyAvailable` `boolean`, `passkeyEnabledForTransactions` `boolean`, `appPin` `AppPinStatusDTO`, `requiredFactors` `List<String>`, `passkeys` `PasskeyInventoryDTO`

### `AccountSecurityStatusDTO`
- `passwordConfigured` `boolean`, `passkeyRegistered` `boolean`, `totpEnabled` `boolean`, `backupCodesRemaining` `int`, `unprotected` `boolean`, `warningMessage` `String`, `accountActivated` `boolean`, `inboundEnabled` `boolean`, `passkeys` `PasskeyInventoryDTO`

### `AccountSecurityUpdateRequestDTO`
- `accountSecurity` `AccountSecurityType`, `shamirTotalShares` `Integer`, `shamirThreshold` `Integer`, `multisigThreshold` `Integer`

### `AdminAccessAttemptDTO`
- `attemptId` `UUID`, `status` `String`, `deviceId` `String`, `deviceName` `String`, `browser` `String`, `userAgent` `String`, `ipFingerprint` `String`, `requestedAt` `LocalDateTime`, `expiresAt` `LocalDateTime`

### `AdminAccessDecisionRequestDTO`
- `decision` `String`

### `AdminAuthenticatedDeviceDTO`
- `deviceId` `String`, `deviceName` `String`, `browser` `String`, `userAgent` `String`, `status` `String`, `firstAccessAt` `LocalDateTime`, `lastAccessAt` `LocalDateTime`

### `AdminDeviceSessionDTO`
- `id` `UUID`, `deviceId` `String`, `deviceName` `String`, `browser` `String`, `platform` `String`, `status` `String`, `firstAccessAt` `LocalDateTime`, `lastAccessAt` `LocalDateTime`

### `AdminKeyCreateRequestDTO`
- `keyMaterialHash` `String`, `deviceInstallId` `String`

### `AdminKeyStatusDTO`
- `configured` `boolean`, `status` `String`, `fingerprint` `String`, `createdAt` `LocalDateTime`, `revokedAt` `LocalDateTime`

### `AdminLoginRequestDTO`
- `username` `String`, `password` `char[]`, `adminKeyProof` `String`, `deviceId` `String`, `deviceName` `String`, `browser` `String`, `userAgent` `String`, `platform` `String`

### `AdminLoginResponseDTO`
- `status` `String`, `requiresMobileApproval` `boolean`, `attemptId` `UUID`, `expiresAt` `LocalDateTime`, `token` `String`, `message` `String`

### `AppPinStatusDTO`
- `enabled` `boolean`, `configured` `boolean`, `locked` `boolean`, `failedAttempts` `int`, `remainingAttempts` `int`, `maxAttempts` `int`, `minPinLength` `int`, `maxPinLength` `int`, `resettableWithTotp` `boolean`, `deviceScoped` `boolean`, `lockedUntil` `LocalDateTime`, `lastVerifiedAt` `LocalDateTime`, `updatedAt` `LocalDateTime`

### `Assets`
- `hotWalletBtc` `BigDecimal`, `treasuryXpubOnchainBtc` `BigDecimal`, `lightningBtc` `BigDecimal`, `totalOnchainBtc` `BigDecimal`, `totalAssetsBtc` `BigDecimal`

### `BackupCodesStatusDTO`
- `enabled` `boolean`, `remainingCodes` `int`, `newlyGeneratedCodes` `List<String>`

### `ChainState`
- `bitcoinNetwork` `String`, `bitcoinBlockHeight` `Long`, `bitcoinBestBlockHashRef` `String`, `lightningBlockHeight` `Long`, `lightningBlockHashRef` `String`

### `ConfigureAppPinRequestDTO`
- `enabled` `Boolean`, `pin` `String`, `currentPin` `String`, `totpCode` `String`

### `DeviceTokenRegisterRequest`
- `platform` `String`, `token` `String`, `deviceId` `String`, `appVersion` `String`

### `DeviceTokenResponse`
- `id` `Long`, `platform` `String`, `tokenRef` `String`, `deviceRef` `String`, `appVersion` `String`, `createdAt` `LocalDateTime`, `lastSeenAt` `LocalDateTime`, `revokedAt` `LocalDateTime`, `active` `boolean`

### `EmergencyRecoveryFinishRequest`
- `recoverySessionId` `String`, `totpCode` `String`, `publicKey` `String`, `publicKeyCose` `String`, `deviceName` `String`, `signature` `String`, `authData` `String`, `clientDataJSON` `String`, `credentialId` `String`, `userHandle` `String`

### `EmergencyRecoveryFinishResponse`
- `username` `String`, `newBackupCodes` `List<String>`

### `EmergencyRecoveryStartRequest`
- `username` `String`, `newPassphrase` `char[]`, `recoveryCodes` `List<String>`, `challenge` `String`, `nonce` `String`

### `EmergencyRecoveryStartResponse`
- `recoverySessionId` `String`, `otpUri` `String`, `passkeyChallenge` `String`, `expiresInSeconds` `long`, `requiredRecoveryCodes` `int`

### `Liabilities`
- `internalLedgerBtc` `BigDecimal`, `reservedOnchainBtc` `BigDecimal`, `reservedLightningBtc` `BigDecimal`, `totalOperationalExposureBtc` `BigDecimal`

### `MerkleProof`
- `merkleRoot` `String`, `ledgerCount` `Long`, `createdAt` `LocalDateTime`, `anchorTxidRef` `String`

<!-- Mining DTOs removed: MiningAllocationRequestDTO, MiningAllocationResponseDTO, MiningRigOfferDTO — controller and module removed from codebase -->

### `NotificationSendRequest`
- `userId` `String`, `title` `String`, `body` `String`, `kind` `String`, `severity` `String`, `deeplink` `String`, `entityType` `String`, `entityId` `String`, `metadata` `Map<String, String>`

### `PasskeyActionRequiredDTO`
- `action` `String`, `reason` `String`, `challenge` `String`, `totpFallbackAvailable` `boolean`, `linkNewPasskeyAllowed` `boolean`, `linkPasskeyPath` `String`, `guidance` `String`, `passkeys` `PasskeyInventoryDTO`

### `PasskeyDeviceDTO`
- `credentialRef` `String`, `deviceName` `String`, `brand` `String`, `model` `String`, `serialNumber` `String`, `deviceInstallId` `String`, `platform` `String`, `browser` `String`, `firstAccessAt` `LocalDateTime`, `lastAccessAt` `LocalDateTime`, `status` `String`, `relyingPartyId` `String`, `originHost` `String`, `compatibilityStatus` `String`, `compatibleWithCurrentLogin` `boolean`

### `PasskeyInventoryDTO`
- `passkeyRegistered` `boolean`, `compatibleForCurrentLogin` `boolean`, `legacyCredentialsPresent` `boolean`, `currentRelyingPartyId` `String`, `currentHost` `String`, `devices` `List<PasskeyDeviceDTO>`

### `PasskeyRegistrationRequest`
- `publicKey` `String`, `deviceName` `String`, `signature` `String`, `authData` `String`, `clientDataJSON` `String`, `credentialId` `String`, `userHandle` `String`, `publicKeyCose` `String`, `brand` `String`, `model` `String`, `serialNumber` `String`, `deviceInstallId` `String`, `platform` `String`, `browser` `String`, `status` `String`

### `PasskeyVerifyRequest`
- `username` `String`, `signature` `String`, `authData` `String`, `clientDataJSON` `String`, `credentialId` `String`

### `ProviderHealth`
- `provider` `String`, `status` `String`, `source` `String`, `message` `String`

### `ResponseError`
- `timestamp` `LocalDateTime`, `status` `HttpStatus`, `error` `String`, `message` `String`, `path` `String`

### `SignupResponseDTO`
- `sessionId` `String`, `otpUri` `String`, `backupCodes` `List<String>`, `totpOptional` `boolean`

### `TotpSetupResponseDTO`
- `otpUri` `String`, `secret` `String`

### `KfeReserveOverviewResponse`
- `totalOnchainBtc` `BigDecimal`, `lightningNodeBtc` `BigDecimal`, `inboundLiquidityBtc` `BigDecimal`, `outboundLiquidityBtc` `BigDecimal`, `reservedOnchainBtc` `BigDecimal`, `reservedLightningBtc` `BigDecimal`, `availableOnchainBtc` `BigDecimal`, `availableLightningBtc` `BigDecimal`, `lightningSendsAllowed` `boolean`, `liquidityState` `String`

### `VerifyAppPinRequestDTO`
- `pin` `String`
