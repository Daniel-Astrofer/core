# Status

Last reviewed: 2026-09-01.

| Area | State | Evidence / next action |
| --- | --- | --- |
| Auth service | Implemented | Source and tests are under `auth-service/`. Keep its public and internal APIs documented in `docs/backend/api/`. |
| KFE service | Extracted | Source, tests and financial docs are in sibling `kerosene-kfe`. Vault Mesh and Node integration remain separate production blockers. |
| Auth/KFE boundary | Native SPIFFE mTLS implemented; cluster rollout pending | Auth uses a dedicated TLS 1.3 internal connector and pins the exact KFE SPIFFE ID. See `AUTH_KFE_WORKLOAD_IDENTITY.md`; an in-cluster handshake is still required before activation. |
| Shared runtime | Extracted | `kerosene-shared` owns neutral Java utilities; class ownership audit remains pending. |
| API documentation | Partial | Active indexes exist; endpoint inventories require automated drift checks against controllers. |
| Cross-repository contracts | Extracted | Core consumes canonical sibling `kerosene-contracts` through Gradle composite substitution. Replace `0.2.0-SNAPSHOT` with an immutable production release. |
| Python rail adapters | Extracted | `kerosene-rails` owns both adapters and their CI. Deploy packaging remains pending. |
| Administrative CLI | Extracted | `kerosene-admin` owns the build, CI and release while preserving the `kerosene-jctl` executable name. |
| Image packaging | Extracted | Dockerfiles, healthcheck source, Compose, Kubernetes and environment policy belong to `kerosene-deploy`. |
| Production readiness | Blocked outside this repository | Deployment, secrets and environment gates are owned by `kerosene-deploy`. |

Historical roadmaps do not override this file. Update this table with evidence
when a state changes.
