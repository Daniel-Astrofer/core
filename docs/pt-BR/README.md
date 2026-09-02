# Documentação do Kerosene Core

Este é o índice para entender o Core sem precisar conhecer o antigo monorepo.

## Responsabilidade

- `auth-service`: identidade, autenticação, sessões, notificações e gateway
  público.

Os protocolos compartilhados entre repositórios pertencem ao
`kerosene-contracts`. A execução por ambiente pertence ao `kerosene-deploy`.
A CLI administrativa pertence ao `kerosene-admin`; os adapters pertencem ao
`kerosene-rails`. A execução financeira pertence ao `kerosene-kfe`; os
utilitários Java neutros pertencem ao `kerosene-shared`.

## Por onde começar

- [Estado atual e pendências](STATUS.md)
- [Fronteiras dos módulos e critérios de migração](FRONTEIRAS-DOS-MODULOS.md)
- [Identidade de workload Auth/KFE](IDENTIDADE-DE-WORKLOAD-AUTH-KFE.md)
- [Limites do repositório](../REPOSITORY_BOUNDARY.md)
- [Índice das APIs](../backend/api/README.md)
- [Inventário das APIs](../backend/API_REFERENCE.md)
- [Regras de negócio](../backend/BUSINESS_LOGIC.md)
- [Integração de infraestrutura](../backend/INFRASTRUCTURE.md)
- Responsabilidade financeira e runbooks do KFE: `kerosene-kfe/docs/reference/`
- [Solução de problemas](../backend/TROUBLESHOOTING.md)

Arquivos marcados como **históricos** ou **depreciados** servem apenas como
registro e não são a fonte operacional atual.

Índice técnico curto: [English](../en/README.md).
