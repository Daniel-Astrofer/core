# Agent rules

- Keep Auth and KFE deployable as separate processes and images.
- Do not introduce shared database ownership between services.
- Protocol changes must be backward-compatible and coordinated through
  `kerosene-contracts`.
- Never commit credentials, JWT secrets, macaroons, seeds or production data.
- Run Gradle verification before pushing.
- Do not copy KFE, Shared, Contracts, Admin or Rails source back into Core.
