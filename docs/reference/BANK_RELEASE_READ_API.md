# Bank release read producer — Contracts release/v1

Core provides the Bank read consumed by Node:

`GET /v1/releases/observation?releaseDigest=sha256:<64 lower-case hex>&challenge=<64 lower-case hex>`

The response is exactly Contracts' `kerosene.bank-release-read/v1`. It binds a
locally staged target identity, configured Bank/network identity, fresh caller
challenge and local runtime observation under one independently pinned Ed25519
Bank signature. Its lifetime is 30 seconds. `source` is `bank-runtime`; there is
no synthetic fallback, aggregate report signature or commit certificate.

**This implementation cannot return `compatible`.** A valid signature on Core's
current runtime manifest is not proof that a new complete Cell release can run.
Missing/optional attestation and an authorized current runtime return `unknown`.
A cryptographically verified current-runtime manifest with a runtime mismatch
returns `incompatible`. Both carry `observedSequence:0`: no target compatibility
has been established. Independent target rebuild/compatibility, KFE coverage,
Vault attestations and ordered governance still need their own implementation.
No property, request field or catalog entry can override that boundary.

## Transport and identity

The dedicated Spring Security chain matches only the observation route and its
subpaths, before the ordinary JWT chain. Only GET is accepted. Enablement fails
startup unless the **direct Core listener** has `server.ssl.enabled=true` and
`server.ssl.client-auth=need`. Configure the existing server key/trust stores;
the container verifies certificate trust during TLS. The application additionally
requires a secure request, a nonexpired container-provided X.509 peer certificate
and an explicitly allowed SHA-256 digest of that leaf certificate's SPKI DER.

JWTs, ROLE_ADMIN, shared secrets, forwarded client-certificate headers and a CA
trusted but unpinned client do not authorize this route. TLS termination at an
ordinary HTTP reverse proxy is unsupported; pass TLS through to Core. Mandatory
client authentication on this listener also affects its other routes: deploy it
only where those callers have the required identities. A separately qualified
dedicated Bank listener remains deployment work; do not enable this on a public
browser endpoint just to satisfy observation configuration.

Keep the Bank signing key separate from Node observer and governance signing
keys. Node's configured `BankEndpoint.publicKeyDerBase64` pins this Bank SPKI;
TLS client pins authorize transport, not compatibility or release approval.

## Configuration and operator-staged catalog

Spring relaxed binding prefix: `release.bank-observer`.

| Property | Requirement |
| --- | --- |
| `enabled` | Default false; disabled requests return 503 without a signature |
| `observer-id` | Actual Bank ID, matching Node's source roster |
| `network-id` | Exact target Bank network |
| `client-spki-digests` | 1–64 distinct `sha256:<hex>` pins for authorized Node client identities |
| `private-key-file` | Absolute normalized path to Ed25519 PKCS8 DER, regular private file, at most 8 KiB |
| `public-key-der-base64` | Pinned Ed25519 X.509 SPKI DER; startup proves it matches the private key |
| `target-directory` | Absolute normalized existing directory, not group/world writable |

Never commit private keys, keystore passwords or production catalog contents.
Private key files must have owner-only permissions (normally 0400/0600).
Catalog entries must be regular, not group/world writable, and at most 256 KiB.
Symlinks, including any ancestor symlink, are refused. Kubernetes projected
Secret/ConfigMap links therefore need an operator-reviewed init copy into a
dedicated private volume, followed by appropriate ownership/modes and a read-only
Core mount. The application does not silently change permissions or copy secrets.
The filesystem owner/root and service configuration remain trusted local boundaries.

Stage each complete release-lock as `<canonical digest without sha256:>.json`.
Its canonical bytes use the shared Contracts-compatible JSON implementation:
recursive lexicographic keys, compact UTF-8, array order preserved, no floats or
integers outside ±(2^53−1). The reader rejects duplicate JSON keys, trailing JSON,
depth above 64, digest changes and wrong schema/version, release ID, sequence or
Bank network domain. Locks v1/v2/v3 identify a target; none grants compatibility.
This catalog is not a replacement for Deploy's full lock-schema, TUF, provenance,
quorum or v3 governance validation. Installing a file grants **no release authority**.

Catalog publication is operator-owned and must use atomic replacement; no catalog
write API exists. A catalog entry is reread and rehashed on every request. At most
four observations can run concurrently. There is no shell execution, deployment,
financial mutation, signer activation or local anti-replay reset through this API.

## Failures and audit

Responses are JSON `{ "code": "..." }`, with no signature on error:

| Status | Code / boundary |
| --- | --- |
| 400 | `query_invalid`: malformed, missing, repeated or extra parameters, including caller `status` |
| 403 | `client_certificate_required`: missing/unpinned/nonsecure application peer |
| 404 | `target_unknown`: digest absent from the local catalog |
| 405 | `method_not_allowed` |
| 429 | `observer_busy` |
| 503 | `observer_unconfigured` or `bank_evidence_unavailable` |

No client certificate fails at the TLS handshake before an HTTP response.
Application responses use `Cache-Control:no-store` and a bounded/sanitized
`X-Request-Id`. Application attempts emit the existing `AUDIT` marker with request
ID, verified peer SPKI or `unverified`, method, status and failure flag. They do
not log query/challenge contents, target files, signatures, private keys, bearer
tokens or raw exception details. TLS handshake failures belong to listener audit.

## Verification

`./gradlew --no-daemon -p verification test` includes the actual production
producer/controller/security chain. A disposable embedded Tomcat performs real
mTLS with independently generated test certificates and a separate Ed25519 key;
tests verify signed responses, challenge binding, trusted-but-unpinned denial,
TLS rejection without a certificate, cleartext/proxy-header denial, duplicate
query rejection and no caller-selected compatibility. A real signed runtime
manifest mismatch is also checked. These tests are not a full Core application,
Node-to-Core Cell deployment or successful new-release compatibility qualification.

The separately selected `nodeCoreWireTest` task requires the corresponding Node
branch's explicitly built `core_bank_probe` executable via
`KEROSENE_NODE_BANK_PROBE=/absolute/path/to/node/target/debug/examples/core_bank_probe`.
Run `./gradlew --no-daemon -p verification nodeCoreWireTest test`. It starts the
same disposable real-mTLS producer and exercises Node's actual transport,
independent Bank signature/binding validation, persisted negative observation
and refusal to sign an unknown target. Default tests exclude this tagged test;
the explicit task fails if its executable is missing. It is a bounded wire
qualification, not successful compatibility or a complete Cell deployment.
