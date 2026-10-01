<!--
Kerosene documentation metadata
status: review-required
audience: internal
owner: core
source_of_truth: core
last_reviewed: 2026-09-03
-->

# Kerosene Core (Auth gateway)

Cell operator API: [routes, evidence proposal and runtime configuration](../reference/CELL_OPERATIONS_API.md).

Java/Spring identity, session, notification and public gateway service.

## Modules

- `auth-service`: Auth, sessions, notifications and public API.

Cross-repository contracts are consumed from the sibling
`kerosene-contracts` composite build. The administrative CLI lives in
`kerosene-admin`; Bitcoin and Lightning adapters live in `kerosene-rails`.
Image recipes and their healthcheck helper live in `kerosene-deploy`.
Financial execution lives in `kerosene-kfe`; neutral Java utilities live in
`kerosene-shared`.

Auth and KFE have separate source repositories and builds. Auth communicates
with KFE through remote clients and canonical contracts; the Core build no
longer consumes KFE implementation classes.

Documentation: [English](../en/README.md) ·
[Português (Brasil)](../pt-BR/README.md) ·
[documentation portal](docs/README.md) ·
[status](../STATUS.md) ·
[API catalog](../reference/API_CATALOG.md) ·
[quickstart](../QUICKSTART.md)

Module ownership and extraction gates:
[English](../en/MODULE_BOUNDARIES.md) ·
[Português (Brasil)](../pt-BR/FRONTEIRAS-DOS-MODULOS.md)

KFE-specific architecture and runbooks are owned by `kerosene-kfe`.

`web-admin-build/` is an empty packaging mount. Deploy generates the Flutter
bundle there before building the server image; placeholder HTML is not shipped.
