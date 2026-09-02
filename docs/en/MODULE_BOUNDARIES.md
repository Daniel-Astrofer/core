# Module boundaries

This register describes the current source tree. It is not a future architecture
promise and does not authorize protocol or behavior changes.

| Path | Current responsibility | Boundary state | Safe extraction gate |
| --- | --- | --- | --- |
| `auth-service/` | Identity, authentication, sessions, notifications and public gateway | Sole implementation module in Core; no KFE implementation dependency | Publish immutable contract versions and maintain remote contract tests. |
| sibling `kerosene-kfe` | Ledger, wallets, reconciliation and financial execution | Extracted with independent build and CI | Publish immutable artifacts and contract tests. |
| sibling `kerosene-shared` | Neutral Java runtime utilities | Extracted with independent build and CI | Audit class ownership and publish an immutable artifact. |
| sibling `kerosene-contracts` | Canonical cross-repository Java contracts | Extracted; consumed through Gradle composite substitution | Publish immutable release versions and replace the snapshot coordinate for production. |
| sibling `kerosene-admin` | Read-only administrative CLI | Extracted with independent build, CI and release | Create the GitHub repository/remote and preserve command compatibility. |
| sibling `kerosene-rails` | Bitcoin Core and LND HTTP facades | Extracted with independent Python CI | Add production image ownership in Deploy without copying adapter source. |
| sibling `kerosene-deploy` | Image recipes, healthcheck helper and environment orchestration | Extracted | Keep service source outside Deploy and consume immutable images in production. |

## Dependency direction observed today

```text
Core/Auth -- SPIFFE mTLS/HTTPS ---> sibling kerosene-kfe (runtime boundary)
Core/Auth + KFE ------> sibling kerosene-shared
Core/Auth/KFE/Shared -> sibling kerosene-contracts
kerosene-admin          independent repository
kerosene-rails          independent repository
kerosene-deploy         independent packaging/orchestration repository
```

Core and KFE no longer consume each other's implementation artifacts. Their
remaining shared surface is the versioned Contracts API and the runtime HTTP
boundary.

## Rules for this reorganization phase

- Do not add features, transports, consensus or security protocols.
- Do not restore KFE, Shared, Contracts, Admin or Rails source to Core.
- Do not place Compose, Kubernetes, secrets or environment policy in Core.
- Preserve service behavior while documenting and testing each future boundary.
