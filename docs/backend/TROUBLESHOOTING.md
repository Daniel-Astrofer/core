# Troubleshooting

Fast diagnostics for Kerosene backend. Prefer read-only checks. `infra/scripts/local/control.sh start` may be disabled in orchestration — do not blind-start all containers.

## First Rule

## Fast Decision Flow
```bash
git status --short
```

- Startup/config: profiles, required env vars, Flyway safety, datasource, Redis, **vault mesh**, LND, or KFE-only assumptions.
- Runtime request failure: trace the request by `traceId` or `correlationId`.
- KFE financial behavior: verify `/kfe/**` or `/api/admin/kfe/**` only, then run focused KFE tests.
- Treasury / signing: probe vault mesh `GET /v1/health` and admin `GET /api/admin/operations/vault-mesh` — not HashiCorp Vault Raft readiness, not mpc-sidecar.
- Frontend/backend mismatch: run focused Flutter analysis/tests around the affected client code.
- Orchestration issue: use the MCP helper status/result tools before dispatching more work.
- If the process crashes before serving traffic, inspect startup diagnostics and startup logs first.
- If a request fails, search logs by `traceId`, `correlationId`, `event`, `domain`, and `operation`.
- If money state changed, inspect audit events and transaction/outbox state before retrying the action.
- If a legacy route appears, run the KFE-only verifier before editing API clients or docs.
```bash
git diff --check
```


## Crash Triage Without Blind Container Restarts
```bash
docker compose ps
docker compose logs --tail=200 <backend-service>
# mesh lab (host):
docker compose -f infra/docker/compose/vault-mesh-lab.compose.yaml ps
```

- `domain=startup`
- `event`
- `operation`
- `exceptionType`
- `correlationId`
- `traceId` when available
| Signal | Likely area | Next check |
| --- | --- | --- |
| Profile or production safety failure | configuration | active profiles and fail-closed production checks |
| Flyway error | database migration safety | migration order and datasource config |
| datasource connection failure | database | DB service status and credentials source, without printing secrets |
| Redis unavailable | cache/session/rate limit | Redis service status and profile expectation |
| Vault mesh health / attestation failure | treasury custody | mesh `GET /v1/health` (node_tier, attestation_mode, tee_available); admin vault-mesh snapshot; kfe `kfe.vaultmesh.*` |
| HashiCorp Raft / VAULT_RAFT_* errors | stale config | local-full sets Raft off; treasury is mesh-only — do not revive Raft as custody SoT |
| MPC signer unavailable | legacy path | mpc-sidecar is off on deploy; enable mesh + `kfe.mpc.signing-enabled=false`, not sidecar |
| LND or rail provider failure | KFE rail | provider config and retry/outbox state |
| legacy financial route/package hit | KFE-only regression | `scripts/verify-kfe-only.sh` (package `com.kerosene.kfe`) |

## Trace And Correlation IDs
```bash
rg 'correlationId=<id>|"correlationId":"<id>"' logs backend -g '*.log'
rg 'traceId=<id>|"traceId":"<id>"' logs backend -g '*.log'
```


## Structured Logs
- `runtime`
- `startup`
- `security`
- `auth`
- `kfe`
- `audit`
- `integration`
- `vault`
- `vaultmesh` / mesh Intent–Receipt
- `frontend-api`
- `access`
- `event`
- `domain`
- `operation`
- `message`
- `exceptionType`
- `correlationId`
- `traceId`
- sanitized identifiers such as wallet, transaction, audit, or user references

## Audit Events
- Auth/session: `AUTH_LOGIN_SUCCEEDED`, `AUTH_LOGIN_FAILED`, `AUTH_LOGOUT`, `JWT_SESSION_REVOKED`
- Admin access: `ADMIN_ACCESS_REQUESTED`, `ADMIN_ACCESS_APPROVED`, `ADMIN_ACCESS_REJECTED`, `ADMIN_ACCESS_REDEEMED`
- KFE: `KFE_WALLET_CREATED`, `KFE_TRANSACTION_SUBMITTED`, `KFE_IDEMPOTENCY_CONFLICT`, `KFE_OUTBOX_DISPATCHED`, `KFE_OUTBOX_RETRY`, …
- Vault mesh: mesh health / Intent–Receipt / day-advance events as emitted by current code; treat legacy `MPC_*` audit names as historical unless still logged

## KFE-Only Verification
```text
/kfe/**
/api/admin/kfe/**
```

```bash
scripts/verify-kfe-only.sh
```

```bash
STRICT_DOCS=1 scripts/verify-kfe-only.sh
```


## Focused Tests
```bash
cd /path/to/kerosene-core
./gradlew test --tests 'com.kerosene.kfe.*'
./gradlew test --tests '*Kfe*'
```

```bash
cd frontend
flutter analyze
flutter test test/path/to/focused_test.dart
```


## MCP Orchestration Helpers
- `kerosene_cycle_once`: run one controlled nightly orchestration cycle. It accepts `{"mode":"nightly"}`.
- `kerosene_git_status`: compact repository or agent worktree status.
- `kerosene_clean_worktree`: dirty tree inspection/cleanup flow without discarding unknown work.
- `kerosene_dispatch_next`: dispatch the next nightly queue item.
- `kerosene_collect_agent_result`: status, tail, and dirty-worktree info for a managed agent.
- `kerosene_commit_agent_output`: validate, stage enumerated files, commit, and cherry-pick finished agent output.
```text
fase-6/docs: update developer troubleshooting guide
```


## Minimal Closeout
- what failed;
- the `traceId` or `correlationId`, if available;
- the structured log `event`, `domain`, and `operation`;
- relevant audit event type, if money/security state changed;
- the focused validation command and result;
- whether broader validation was skipped and why.
