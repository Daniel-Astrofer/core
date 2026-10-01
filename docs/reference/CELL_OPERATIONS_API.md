# Cell operations API

Core owns the operator gateway. jctl and Flutter call Core only. Core reads Bank
over HTTPS with an explicit PKCS12 client identity and trust store. It never
executes shell deployment commands, restores a database, or changes Bank state.
The existing `/system/release` runtime snapshot remains compatible. Optional
attestation no longer labels a missing signature valid, and unknown runtime
values cannot satisfy a signed expected value.

## Authenticated, audited routes

Every route below requires `ROLE_ADMIN` or `ROLE_OPERATOR`. `ROLE_AUDITOR` and
ordinary users are denied. Attempts, including denials and failures, emit the
`AUDIT` marker with operation, method, sanitized actor, status and request ID.
`X-Request-Id` is returned; caller IDs must be 1–128 ASCII letters/digits/._-.
Bodies, upstream credentials and raw transport exceptions are not logged.
Production must retain the existing AUDIT log stream in its operator log sink.

| Method | `/api/admin/operations/cell` suffix | Result |
| --- | --- | --- |
| GET | (none) | Full `kerosene.cell-operations/v1` evidence snapshot |
| GET | `/releases` | Current Cell / target / Core runtime |
| GET | `/quorum` | Required and accepted votes, freshness per observer |
| GET | `/blockers` | Readiness and explicit blockers |
| GET | `/backups` | Backup digests and restore test evidence |
| GET | `/updates` | Observed phase/history plus recorded plans |
| GET | `/updates/plans` | Up to 100 stored plans |
| GET | `/updates/plans/{uuid}` | Persisted plan intent |
| POST | `/updates/plans` | Recheck evidence and atomically store intent |

Plan body: `targetReleaseId`, positive `targetSequence`, `targetDigest`,
`deploymentManifestDigest`, `packageManifestDigest`. Digests are `sha256:` plus
64 lower-case hex characters. The exact five-field target must match verified
Bank evidence (409 if changed or absent). Missing/invalid input gives 400;
unconfigured storage gives 503. Readiness blockers result in a `BLOCKED` plan;
otherwise status is `PLANNED`. Both always carry `deploymentExecuted:false`.
Recorded readiness is historical and must be rechecked before any owner-side
execution. No execute/deploy/restore route exists. UI creates intent only;
jctl verifies the local package before submitting intent. Neither proves that
deployment has run. Plan JSON contains operator identity and must be retained
as operational audit data in the configured Core-owned volume.

## Normative Node observation integration

Core now consumes Contracts' `kerosene.release-observations/v1` at
`GET /v1/releases/observations?releaseDigest=sha256:<target>` on the configured
Node HTTPS/mTLS origin. `operations.cell.target-release-digest` must be set
explicitly; the target never comes from an operator HTTP query. The unsigned
retrieval wrapper contains individually signed Node observations, each with an
independently signed `kerosene.bank-release-read/v1`. Configure distinct pinned
Node `signer-keys` and Bank `bank-keys`, plus an allowlist of Bank observer IDs.

Signatures cover recursively sorted, compact UTF-8 JSON excluding only their
own `signatures` field, as specified in Contracts, not arbitrary base64 payload
bytes. Core verifies both layers, exact release/network/sequence/digest binding,
freshness and distinct Bank identities/keys. A serving Node wrapping multiple
Bank reads is not itself an additional Bank vote. Synthetic sources, duplicate
votes, expired reads, unknown fields and noninteroperable numeric values fail
closed. Verified `incompatible` and `unknown` observations remain visible to
operators but never count as compatible votes.

The actual observation contract does **not** contain current Cell deployment,
ordered consensus proof, package/configuration bindings, backup evidence or
execution history. Core explicitly reports these missing and `ready:false`;
it does not infer them from signatures or runtime health. The current plan API
cannot accept a target with absent package/configuration bindings. The actual
Core Bank producer `/v1/releases/observation` remains to be integrated with
independent compatibility checks; the outbound reader is not that producer.

## Quarantined legacy operational envelope

The earlier prototype below is retained as a legacy decoder, not a normative
Bank/Node contract and not an authorization mechanism. It always adds
`LEGACY_NON_NORMATIVE_OPERATIONAL_ENVELOPE` and cannot make readiness true. Do not
implement a new Bank endpoint based on this envelope. Its historical fields are
documented here solely to identify existing fixtures during migration.

GET `/v1/releases/observations` on the configured HTTPS origin returns:

```json
{
  "schema": "kerosene.bank-release-observations/v1",
  "payloadBase64": "BASE64_OF_EXACT_UTF8_JSON_BYTES",
  "signatures": [{"keyId":"bank","algorithm":"Ed25519","signatureBase64":"BASE64_SIGNATURE"}]
}
```

The signature covers **the decoded payload bytes exactly**, including the
payload schema. Keys are pinned by configuration, never taken from the envelope.
Distinct configured key IDs count toward `minimum-signatures`. Duplicate JSON
keys, unknown versions, invalid signatures and wrong Cell/network identity fail
closed. Transport/verification failures produce `UNVERIFIED`, `ready:false`,
zero accepted votes and missing-evidence blockers; they never turn into an empty
successful quorum. Responses are limited to 1 MiB; redirects are refused.

Payload `schema` is `kerosene.bank-cell-observations/v1`, with these fields:

- `cellId`: exact configured identity.
- `observerReport`: existing `kerosene.bank-observer-report/v2`, retaining its
  closed deploy schema unchanged (releaseId/networkId/targetSequence/
  releaseLockCanonicalDigest/issuedAt/expiresAt/observations/signatures).
  Its inner signatures remain Bank-owned report material; Core verifies the
  encompassing envelope signature, not a new canonicalization of that report.
- `currentRelease`: `{releaseId,sequence,digest}`.
- `targetRelease`: `{releaseId,sequence,digest,deploymentManifestDigest,packageManifestDigest}`;
  release ID, sequence and digest must match the observer report. The deployment
  digest binds the **exact JSON bytes** of service configuration; package digest
  binds the exact signed package manifest bytes used by jctl.
- `backups`: `{backupId,releaseDigest,objectDigest,createdAt,restoreEvidence}`
  entries; `restoreEvidence` is `{status:"PASSED",objectDigest,verifiedAt}`.
  Accepted evidence binds the current release and the same backup object digest;
  backup creation and restore verification must both be fresh.
- `update`: `{phase,history}`. Phases are IDLE, PREPARING, APPLYING, VERIFYING,
  COMPLETED, FAILED, ROLLING_BACK. History is an array of Bank-owned execution
  evidence objects. Plans do not insert execution history. Failed/in-progress/
  rollback phases block another update; missing/unknown phase/history blocks too.

Only unique configured observers whose observations are fresh and `compatible`
at the exact target sequence/digest count. Unknown/duplicate observers are
blockers. Report issuance cannot be in the future; report must be unexpired.
Every observation must be within `maximum-age-seconds`; backup creation and
restore tests use their separate maximum ages (default 24 hours);
future timestamps are rejected. Missing backup/restore evidence, package or
configuration bindings and insufficient votes block readiness. Core's own
runtime must also have a verified signature and authorized runtime values.
These historical fields are not provided by the normative Node contract and
must not be manufactured by a compatibility adapter.

## KFE maintenance contract

When a runtime short-lived ROLE_ADMIN credential is provisioned, Core reads
`GET /api/admin/kfe/maintenance/status` over the configured HTTPS/mTLS trust.
The old internal shared secret does not authorize this admin endpoint and is
never substituted. The credential file is reread on every request for rotation,
must be regular, private, non-symlink, <=16 KiB, and is never returned or logged.

The expected schema is `kerosene.kfe-maintenance/v1`, with mode/changeId/revision/
observedAt/safeToUpdate/blockers (named nonnegative integer counts). Core requires
fresh observedAt, a known nonempty mode, a nonnegative integer revision and zero
blocker counts as well as `safeToUpdate:true`. Missing auth, unavailable endpoint,
unknown schema/status or stale evidence becomes UNKNOWN and a readiness blocker.
Snapshot/plans expose this evidence. KFE drain/resume remain KFE-owned audited
ROLE_ADMIN operations with `{changeId,reason,expectedRevision}`; Core's operator
surface does not impersonate an administrator or issue those mutations.

## Runtime configuration

Spring relaxed binding prefix: `operations.cell`. Secrets are runtime mounts or
environment references, not committed files. Blank defaults keep readiness blocked.

| Property | Purpose |
| --- | --- |
| `bank-url` | HTTPS origin only; no credentials, path, query or fragment |
| `cell-id`, `network-id` | Expected signed identities |
| `observers` | Comma-separated allowlist of observer IDs |
| `signer-keys.<keyId>` | Pinned Ed25519 X509 DER public key, base64 |
| `bank-keys.<observerId>` | Separately pinned Bank Ed25519 SPKI public key, base64 |
| `target-release-digest` | Explicit full canonical release-lock digest to retrieve |
| `minimum-signatures` | Distinct trusted envelope signers (default 1) |
| `minimum-votes` | Compatible observer votes (default 1) |
| `maximum-age-seconds` | Freshness budget for evidence (default 300) |
| `maximum-backup-age-seconds` | Backup creation freshness (default 86400) |
| `maximum-restore-age-seconds` | Restore verification freshness (default 86400) |
| `key-store`, `key-store-password` | Core outbound PKCS12 mTLS identity |
| `trust-store`, `trust-store-password` | Explicit CA trust for Bank/KFE |
| `kfe-url` | KFE HTTPS origin, optional authenticated maintenance client |
| `kfe-admin-token-file` | Private rotating short-lived ROLE_ADMIN token mount |
| `plan-directory` | Dedicated persistent Core-owned plan volume |

Both outbound origins use this explicit mTLS identity/trust bundle; it must be
authorized by both services. Configure this using approved service configuration
and bind its exact bytes with Deploy's `--deployment-manifest` flow. The plan
directory must be private (POSIX mode 0700) and writable only by Core; supervise disk capacity and retain plans
with operator audit policy. Storage is node-local unless the mounted volume is
shared, and plan history is separate from owner-side execution history.

## Verification without Contracts Gradle

`./gradlew -p verification test --no-daemon --max-workers=1` compiles the actual
changed production sources and tests signatures, freshness, quorum, restore
binding, optional runtime attestation, KFE failure, plan persistence and HTTP
authorization/audit. This independent Gradle project uses no composite build and
does not invoke Contracts or Shared tasks. It is focused verification, not a
claim that the complete legacy Auth module or service startup was verified.
