# Repository boundary

This repository is the canonical source for the Auth/public gateway domain:

- `auth-service`: authentication, sessions, notifications and the public API.

Contracts are owned by the sibling `kerosene-contracts` repository and are
consumed through a Gradle composite build. Core no longer contains a copied
contracts module. Operator clients are owned by `kerosene-admin`; Python rail
processes are owned by `kerosene-rails`.
Image recipes and healthcheck packaging source are owned by `kerosene-deploy`.
Financial execution is owned by `kerosene-kfe`; neutral Java runtime utilities
are owned by `kerosene-shared`.

`auth-service` is physically and compile-time independent from KFE. It consumes
canonical contracts and communicates with KFE through remote clients at
runtime; no KFE implementation artifact is present on the Core classpath.

Core must not read source files from the archived monorepo, Clients, Vault,
Node, Admin, Rails, KFE, Shared or Deploy source files; Gradle composite builds
are dependency substitution for local development, not source ownership.

See [the module boundary register](en/MODULE_BOUNDARIES.md) for ownership,
current coupling and removal gates.
