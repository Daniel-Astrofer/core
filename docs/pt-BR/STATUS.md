# Estado atual

Última revisão: 01/09/2026.

| Área | Estado | Evidência e próxima ação |
| --- | --- | --- |
| Serviço Auth | Implementado | Código e testes estão em `auth-service/`. Manter APIs públicas e internas documentadas em `docs/backend/api/`. |
| Serviço KFE | Extraído | Código, testes e documentação financeira estão no irmão `kerosene-kfe`. Vault Mesh e Node continuam como bloqueios separados de produção. |
| Fronteira Auth/KFE | mTLS SPIFFE nativo implementado; rollout no cluster pendente | Auth usa uma porta interna TLS 1.3 e fixa o SPIFFE ID exato do KFE. Consulte `IDENTIDADE-DE-WORKLOAD-AUTH-KFE.md`; ainda é obrigatório provar o handshake no cluster antes de ativar. |
| Runtime compartilhado | Extraído | `kerosene-shared` possui utilitários Java neutros; a auditoria de propriedade continua pendente. |
| Documentação de API | Parcial | Há índices ativos; falta validar automaticamente divergências entre documentação e controllers. |
| Contratos entre repositórios | Extraídos | O Core consome o `kerosene-contracts` irmão por composição Gradle. Trocar `0.2.0-SNAPSHOT` por uma versão imutável antes da produção. |
| Adapters Python | Extraídos | `kerosene-rails` possui os dois adapters e o CI. O empacotamento no Deploy permanece pendente. |
| CLI administrativa | Extraída | `kerosene-admin` possui build, CI e release, preservando o executável `kerosene-jctl`. |
| Empacotamento de imagens | Extraído | Dockerfiles, healthcheck, Compose, Kubernetes e políticas de ambiente pertencem ao `kerosene-deploy`. |
| Produção | Bloqueada fora deste repositório | Deploy, secrets e gates de ambiente pertencem ao `kerosene-deploy`. |

Planos históricos não substituem este arquivo. Toda mudança de estado deve
incluir evidência verificável.
