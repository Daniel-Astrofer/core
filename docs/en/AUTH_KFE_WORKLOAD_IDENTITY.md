# Auth/KFE workload identity

Auth remains the only public API gateway for `/kfe/**`, `/api/public/kfe/**`
and `/api/admin/kfe/**`; the web edge never reaches KFE directly. KFE is reached
through its dedicated internal HTTPS port. Auth obtains its short-lived X.509-SVID from the SPIFFE
Workload API, proves `.../service/auth`, and accepts only the exact configured
`.../service/kfe` URI SAN from KFE. The same rule applies in reverse for
`/internal/kfe/**` callbacks.

Required production settings:

- `SPIFFE_ENDPOINT_SOCKET=unix:///spiffe-workload-api/spire-agent.sock`
- `KEROSENE_OWN_SPIFFE_ID=spiffe://<trust-domain>/service/auth`
- `KEROSENE_PEER_SPIFFE_ID=spiffe://<trust-domain>/service/kfe`
- `KEROSENE_INTERNAL_MTLS_PORT=8443`
- `KFE_INTERNAL_BASE_URL=https://kfe-service:8443`
- `KFE_REMOTE_BASE_URL=https://kfe-service:8443`

Production refuses clear-text URLs, a non-Unix Workload API endpoint, identical
public/internal ports, a missing identity, or a non-empty
`KFE_INTERNAL_SHARED_SECRET`. The public port cannot dispatch KFE callbacks
because the filter requires both the internal port and the exact client SVID.

The code and manifest checks do not prove cluster readiness. Before activation,
run the deploy preflight and an end-to-end Auth↔KFE handshake with missing,
wrong and expired SVID cases. Keep the rollout flag off until that evidence is
attached to the deploy issue.
