# Kerosene Core documentation

Short technical index for maintainers, CI and service operators.

## Scope

- `auth-service`: identity, authentication, sessions, notifications and the
  public gateway.

Cross-repository protocol schemas are owned by `kerosene-contracts`. Deployment
manifests and environment orchestration are owned by `kerosene-deploy`.
Operator tooling lives in `kerosene-admin`; rail adapters live in
`kerosene-rails`. Financial execution lives in `kerosene-kfe`; neutral Java
runtime utilities live in `kerosene-shared`.

## Current documents

- [Status](STATUS.md)
- [Module boundaries and migration gates](MODULE_BOUNDARIES.md)
- [Repository boundary](../REPOSITORY_BOUNDARY.md)
- [API index](../backend/api/README.md)
- [API inventory](../backend/API_REFERENCE.md)
- [Business logic](../backend/BUSINESS_LOGIC.md)
- [Infrastructure integration](../backend/INFRASTRUCTURE.md)
- KFE financial ownership and runbooks: `kerosene-kfe/docs/reference/`
- [Troubleshooting](../backend/TROUBLESHOOTING.md)

Files marked **historical** or **deprecated** are evidence only and are not an
operational source of truth.

Human-readable Portuguese index: [Português (Brasil)](../pt-BR/README.md).
