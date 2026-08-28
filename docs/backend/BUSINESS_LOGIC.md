# Business Logic

## Overview

Finance is **KFE-only**. Money, balance, wallet, tx, receive, PSBT orchestration, tax events, reserves, reconcile live in `com.kerosene.kfe`.

No parallel finance modules outside KFE. New money behavior → KFE or `/api/admin/kfe/**`.

Treasury signing / share custody lives on the **vault mesh** (`kerosene-vault`): FROST shares, Intent/Receipt, Taproot PSBT, day-advance + reshare, governance rewards. KFE does not hold shares.

## KFE

Transactional core, rail exec, auditable money state.

- wallets + lifecycle
- spendable / locked / watched balances
- internal transfers
- on-chain withdraw
- Lightning withdraw
- tx quote/fee
- payment + receive requests
- cold/watch-only PSBT
- tax events from KFE txs
- admin reserve view
- audit, statement, reconcile
- finance outbox
- Intent submit to vault mesh (when vaultmesh enabled / mesh-only)

## Rules

1. No finance domain outside `com.kerosene.kfe`.
2. Public money routes: `/kfe/**`.
3. Admin money routes: `/api/admin/kfe/**`.
4. Removed finance modules blocked by `scripts/verify-kfe-only.sh`.
5. No flag to restore old finance backend.
6. No mpc-sidecar / HashiCorp Raft as go-live treasury signer.

## Removed

Old finance domains purged. Not SoT. Do not restore.

See `kerosene-kfe/docs/reference/KFE_ONLY_FINANCIAL_ARCHITECTURE.md` and `docs/backend/INFRASTRUCTURE.md`.
