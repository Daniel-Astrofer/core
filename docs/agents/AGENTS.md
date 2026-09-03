# Agent guide — Kerosene Core

## Scope

Core owns Auth, sessions, notifications and gateway policy. KFE, Rails, Deploy,
Admin, Shared and Contracts remain independently owned repositories.

## Documentation

- Start at `docs/README.md`.
- Put boundaries and architecture in `architecture/`, API facts in `reference/`
  and diagnostics/testnet guidance in `operations/`.
- Link to external owners rather than recreating their APIs or runbooks locally.

## Safety and integration

- Keep Auth and KFE deployable as separate processes and images.
- Do not introduce shared database ownership between services.
- Protocol changes must be backward compatible and coordinated through
  `kerosene-contracts`.
- Never commit credentials, JWT secrets, macaroons, seeds or production data.

## Verification

Run Gradle verification before pushing and update API/reference documentation
for a public behavior change.
