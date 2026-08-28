# Infrastructure integration

This document describes how Core integrates with the runtime. The canonical
Compose, Kubernetes and operations files are in the standalone
`kerosene-deploy` repository. Paths beginning with `infra/` below are relative
to that repository and commands must be executed from its root. Vault custody
implementation and architecture belong to the standalone `kerosene-vault`
repository.

## Sources

| Path | Role |
| --- | --- |
| `infra/docker/images.yaml` | image/tag/Dockerfile contract |
| `infra/docker/compose/local.compose.yaml` | local compose topology |
| `infra/docker/compose/vault-mesh-lab.compose.yaml` | lab vault mesh (testnet3) |
| `infra/docker/compose/vault-mesh-staging.compose.yaml` | staging mesh (mTLS) |
| `infra/kubernetes/overlays/local-full` | local-full Kind deploy |
| `infra/kubernetes/docs/LOCAL_FULL_RUNTIME.md` | local-full operator notes |
| `auth-service/src/main/resources/application*.properties` | Auth runtime profiles in this repository |
| `kerosene-kfe/src/main/resources/kfe-service-vaultmesh-*.properties` | KFE bridge profiles in this repository |
| standalone `kerosene-vault` repository | vault mesh (Rust) — treasury / FROST shares |
| Ops AES (`AES_SECRET`) | Core `VaultKeyProvider` loads ops AES from env only (no Java vault service) |
| `.../db/migration` | Flyway |

Do not call the stack “Hydra”. Use Kerosene backend / shards / vault mesh.

## Roles (current truth)

| Piece | Role |
| --- | --- |
| **`kfe-service` (`com.kerosene.kfe`)** | Bank: ledger, balances, rules, Intent emit / Receipt handle. No FROST shares. |
| **Vault mesh (`kerosene-vault`)** | Treasury/signing plane: FROST shares, DKG, day-advance + reshare, Taproot PSBT cosign, governance rewards. |
| **`auth-service` / Core** | Auth, product shell, admin health probes into mesh; ops AES from `AES_SECRET`. |
| **`mpc-sidecar`** | Removed from tree and local-full/local-ha deploy; do not re-wire as primary signer. |
| **HashiCorp Vault Raft** | Not treasury SoT. Removed from local-full and local-ha overlays; not custody health. |

Cutover is **clean**: mesh on, mpc signing off (`kfe.vaultmesh.mesh-only=true`, `kfe.mpc.signing-enabled=false`). No gradual HashiCorp→mesh treasury migration.

## Runtime topology

- Spring Boot shards + Postgres/Redis per shard (or local-full single DB/Redis)
- **Vault mesh** (`kerosene-vault*`) for treasury
- Tor/Vanguards where configured
- Bitcoin Core, indexer, LND as rails
- Prometheus / web admin for ops

### Local-full (deploy path)

```bash
bash infra/deploy.sh
# or: bash infra/kubernetes/deploy.sh local-full --wait
```

| Group | Services |
| --- | --- |
| App | `server` (Core), `kfe-service`, `web-page` |
| DB / cache | `local-postgres`, `local-redis` |
| Vault mesh | host `vault-mesh-lab` (vault-1/2/3) + K8s Endpoints bridge `vault-1` |
| Bitcoin / LN | `local-bitcoin`, `local-lnd*` |
| **Not deployed** | `mpc-sidecar`, HashiCorp `local-vault` wallet-arming |

Mesh lab: testnet3 + static token. Staging/go-live: mTLS via `vault-mesh-staging` + `kfe-service-vaultmesh-go-live.properties`. Same binary; config differs. See `LOCAL_FULL_RUNTIME.md`.

### Compose lab (mesh only)

```bash
docker compose -f infra/docker/compose/vault-mesh-lab.compose.yaml up --build
```

## Node tiers (domestic first-class)

Operators are mostly **domestic PCs** with TPM 2.0. SEV-SNP/SGX is a **preferred seating upgrade**, not a universal requirement. TPM ≠ SEV (TPM seals disk/identity; does not isolate share RAM like SEV). Detail: `VAULT_MESH_PLAN.md` §3.1.

Ceremony = real over-wire FROST DKG (prod and lab share the path; lab may use `dealer_lab` / `ATTESTATION_MODE=sim` for visualize only). Do not claim HW SEV on every node.

## Backend runtime

- Java 21 / Spring Boot in this repository
- KFE domain package: **`com.kerosene.kfe`** (not `source.kfe`)

| Area | Behavior |
| --- | --- |
| HTTP | Docker bind `0.0.0.0:8080` |
| Schema | Hibernate validate; SQL migrations |
| Flyway | off default; prod `FLYWAY_ENABLED=true` |
| Redis | local vs compose DNS by profile |
| Security | JWT, method security, paranoid + rate + JWT filters |
| Body | JSON mutators; default `2KB`, PSBT `64KB` |
| CORS | `APP_CORS_ALLOWED_ORIGINS`; prod wildcard invalid |
| Observability | actuator health/info/metrics/prometheus |

## Health

| Endpoint | Role |
| --- | --- |
| `GET /healthz` | compat health |
| `GET /health/live` | liveness |
| `GET /health/ready` | readiness + critical deps |
| `GET /api/admin/operations/vault-mesh` | admin snapshot of mesh health |
| **Vault mesh** `GET /v1/health` | custody plane (node_tier, attestation_mode, tee_available) |

Admin treasury health = **vault-mesh `/v1/health`**, not HashiCorp Vault Raft readiness.

## API

Domain docs: [api/README.md](api/README.md). Inventory: [API_REFERENCE.md](API_REFERENCE.md).

## Data

- Flyway under the Auth and KFE migration trees in this repository
- Covers: users/security factors; wallets/ledger/balances/payments/txs; Bitcoin/cold/PSBT/tax; treasury/PoR/audit; KFE core/idempotency/outbox/audit hash/UTXO…
- Redis: sessions/rate-limit/challenges/idempotency as configured

## Bitcoin / Lightning / KFE

- Bitcoin Core RPC in Docker/prod-hardened flows; optional local defaults
- ZMQ `rawtx` / `hashblock` when enabled
- Hot wallet / platform xpub config; cold PSBT flows
- LND TLS + macaroon when Lightning enabled
- KFE: wallets, addresses, UTXOs, cold PSBT, tx send/recover, receive capabilities, admin audit

Settlement signing for treasury Taproot PSBT goes through **vault mesh Intent/Receipt**, not mpc-sidecar.

## Vault mesh / custody

| Concern | Behavior |
| --- | --- |
| Shares | FROST shares only on mesh nodes (genesis DKG); Java never holds shares |
| Contract | kfe emits **Intent**, mesh returns **Receipt** |
| PSBT | Taproot signing on mesh |
| Epoch | day-advance + reshare policy |
| Rewards | governance rewards to active vault operators |
| Auth kfe↔mesh | lab: `X-Vault-Token`; go-live: mTLS (`kfe.vaultmesh.tls.*`) |
| Gaps (planned) | full SNP VCEK verification (fail-closed without HW); CHANNELS→LND inject (fail-closed stub, no fake capital); deposit xpub ≠ mesh `tb1p` (client USERS-only PSBT guard) — see Gap notes / `VAULT_MESH_PLAN.md` |

### Gaps (honest — not shipped)

| Gap | Status now | Notes |
| --- | --- | --- |
| **SNP VCEK / full HW attestation** | planned (fail-closed) | Staging stub / fail-closed without HW; **do not** claim production SNP quotes |
| **CHANNELS → LND inject** | landed (on-chain fund) | Soft-reserve CHANNELS → CHANNELS Taproot PSBT to LND address (key ≠ USERS omnibus) → `openChannel` → commit; pending-channels refuse; durable phase + stable Intent resume; commit-retry reconciler. Fail-closed without mesh fund txid. Lab: `kfe.channel.mesh-inject-fund-zero-conf-lab` |
| **Deposit xpub vs `tb1p`** | enforced (policy guard) | Mesh deposit is Taproot `GET /v1/bitcoin/deposit` (`tr()` / `tb1p…`); KFE platform xpub issuance is separate; vaultmesh client/tb1p-only receive address policy is enforced for `WATCH_ONLY` USERS flows |

## Security model

- Threshold FROST `2/3` + release allowlist + TPM seal/identity (domestic) + caps/fail-stop
- Production Gate refuses false SEV/SGX claims; honest domestic-only genesis is admitted
- Ops-secret vault bootstrap (AES master) ≠ treasury share custody

## Workers / build / secrets

### Scheduled workers (high level)

| Area | Responsibilities |
| --- | --- |
| Prices | BTC ticker |
| Accounting / audit | Merkle audit, history cleanup, reconcile / shadow balance |
| Security | time drift, attestation probes (legacy HashiCorp heartbeat removed) |
| Transactions | liquidity / inbound / pending / activation / finance reconcile / provider outbox |
| Treasury | integrity checks; mesh day-rotation when enabled |
| Bitcoin accounts | retention, receive/cold monitors, PSBT expiry |
| KFE | exec outbox, audit log/root, network monitor, inbound settlement |

### Build / ops

| Check | Command or endpoint |
| --- | --- |
| Backend liveness | `GET /health/live` |
| Backend readiness | `GET /health/ready` |
| Mesh health | `GET /v1/health` on vault node; admin `GET /api/admin/operations/vault-mesh` |
| KFE test slice | `./gradlew test --tests 'com.kerosene.kfe.*'` |
| Full backend tests | `./gradlew test` |
| Local-full validate | `bash infra/kubernetes/scripts/validate-local-full.sh` |

### Secrets / artifacts

- `.env`, certs, Tor keys, LND macaroons, DB/Redis/JWT secrets — never commit
- Mesh lab token / passphrase are lab-only; go-live uses mTLS
- Sensitive local runtime under `infra/runtime/local/**` when present
