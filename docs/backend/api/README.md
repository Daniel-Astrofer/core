# Backend API Docs

Per-domain ops docs for FE/mobile/QA. Auth, headers, body, responses, removed/replaced routes.

Inventory: `docs/backend/API_REFERENCE.md` (does not replace domain docs).

Auth source: controllers and DTOs under `auth-service/`, plus
`EndpointPolicyRegistry`, security configuration and `@PreAuthorize` rules.
KFE API documentation is owned by the `kerosene-kfe` repository.

## KFE-only

Active finance:

```text
/kfe/**
/api/admin/kfe/**
```

Old finance routes only as `STALE` / `CONTROLLER_ABSENT` / `REMOVED` / migrate notes — not live client contracts.

## Files

| Service | File | State |
| --- | --- | --- |
| Admin ops | — | Outside public repo |
| Auth | [AUTH.md](AUTH.md) | login, TOTP, passkey, PIN, device-key, recovery, admin |
| Integrations | [INTEGRATIONS.md](INTEGRATIONS.md) | BTCPay policy; no controller; stale |
| KFE | `kerosene-kfe/docs/reference/api/KFE.md` | wallet, dashboard, receive, tx, quote, PSBT, audit |
| Notifications | [NOTIFICATIONS.md](NOTIFICATIONS.md) | live |
| Public/health/web | [PUBLIC_HEALTH_WEB.md](PUBLIC_HEALTH_WEB.md) | public, health, web, actuator |
| Sovereignty | [SOVEREIGNTY.md](SOVEREIGNTY.md) | `7` live; HMAC + admin token |
| Treasury / vault mesh | [PUBLIC_HEALTH_WEB.md](PUBLIC_HEALTH_WEB.md), [INFRASTRUCTURE.md](../INFRASTRUCTURE.md) | no legacy treasury controller; custody = vault mesh (`/api/admin/operations/vault-mesh`, mesh `/v1/health`) |
| DTO index | [DTO_SCHEMA_INDEX.md](DTO_SCHEMA_INDEX.md) | aux only |

Bitcoin accounts, ledger, mining, payments, transactions and wallet documents
from the former monorepo are not present because those routes are not active
contracts. Use the KFE document for current financial APIs.

## Read rules

`STALE` / `CONTROLLER_ABSENT` / `DENIED_BY_DEFAULT` → do not call from clients until controller+service+policy exist.

Prefer:

```text
/auth/**
/kfe/**
/api/economy/**
/api/admin/kfe/audit/**
/mining/**
/notifications/**
/health/**
/sovereignty/**
/quorum/**
```

## Global

- `Security`: CORS, CSRF off, defensive headers, stateless session
- `EndpointPolicyRegistry`: `PUBLIC` / `ADMIN` / `AUTHENTICATED`
- Fallback `anyRequest().denyAll()`
- No policy → may never hit controller
- Filters before REST: `ParanoidSecurityFilter`, `RateLimitFilter`, `JwtAuthenticationFilter`
- `ReleaseAttestationFilter` may require attestation headers when enabled

## Notes

- `DTO_SCHEMA_INDEX.md` does not replace endpoint docs
- `Map<String,Object>` responses may be inferred
- Restoring legacy needs controller + policy + docs update
- Admin treasury health is vault-mesh only (HashiCorp Raft admin routes removed; mpc-sidecar not primary signer)
- Prefer `/api/admin/operations/vault-mesh` + mesh `GET /v1/health` over any Vault Raft readiness probe
