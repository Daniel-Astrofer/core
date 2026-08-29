# Estado atual

Última revisão: 28/08/2026.

| Área | Estado | Evidência e próxima ação |
| --- | --- | --- |
| Serviço Auth | Implementado | Código e testes estão em `auth-service/`. Manter APIs públicas e internas documentadas em `docs/backend/api/`. |
| Serviço KFE | Extraído, integração pendente | Código, testes e documentação financeira estão no irmão `kerosene-kfe`. Validar Vault Mesh e Node antes de produção. |
| Fronteira Auth/KFE | Separação física e de build concluída | O Core não depende da implementação KFE. Publicar versões imutáveis de Contracts e manter testes da comunicação remota. |
| Runtime compartilhado | Extraído | `kerosene-shared` possui utilitários Java neutros; a auditoria de propriedade continua pendente. |
| Documentação de API | Parcial | Há índices ativos; falta validar automaticamente divergências entre documentação e controllers. |
| Contratos entre repositórios | Extraídos | O Core consome o `kerosene-contracts` irmão por composição Gradle. Trocar `0.2.0-SNAPSHOT` por uma versão imutável antes da produção. |
| Adapters Python | Extraídos | `kerosene-rails` possui os dois adapters e o CI. O empacotamento no Deploy permanece pendente. |
| CLI administrativa | Extraída | `kerosene-admin` possui build, CI e release, preservando o executável `kerosene-jctl`. |
| Empacotamento de imagens | Extraído | Dockerfiles, healthcheck, Compose, Kubernetes e políticas de ambiente pertencem ao `kerosene-deploy`. |
| Produção | Bloqueada fora deste repositório | Deploy, secrets e gates de ambiente pertencem ao `kerosene-deploy`. |

Planos históricos não substituem este arquivo. Toda mudança de estado deve
incluir evidência verificável.
