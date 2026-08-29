# Fronteiras dos módulos

Este registro descreve a árvore de código atual. Ele não promete uma arquitetura
futura e não autoriza mudanças de protocolo ou comportamento.

| Caminho | Responsabilidade atual | Estado da fronteira | Critério seguro para extração |
| --- | --- | --- | --- |
| `auth-service/` | Identidade, autenticação, sessões, notificações e gateway público | Único módulo de implementação do Core; sem dependência da implementação KFE | Publicar versões imutáveis dos contratos e manter testes da comunicação remota. |
| repositório irmão `kerosene-kfe` | Ledger, carteiras, reconciliação e execução financeira | Extraído com build e CI próprios | Publicar artefatos imutáveis e testes de contrato. |
| repositório irmão `kerosene-shared` | Utilitários Java neutros de runtime | Extraído com build e CI próprios | Revisar propriedade das classes e publicar artefato imutável. |
| repositório irmão `kerosene-contracts` | Contratos Java canônicos entre repositórios | Extraído e consumido por composição Gradle | Publicar versões imutáveis e substituir o snapshot antes da produção. |
| repositório irmão `kerosene-admin` | CLI administrativa somente de leitura | Extraído com build, CI e release próprios | Criar o repositório/remoto no GitHub e preservar os comandos. |
| repositório irmão `kerosene-rails` | Fachadas HTTP para Bitcoin Core e LND | Extraído com CI Python próprio | Definir imagens de produção no Deploy sem copiar o código dos adapters. |
| repositório irmão `kerosene-deploy` | Receitas de imagem, healthcheck e orquestração | Extraído | Manter código dos serviços fora do Deploy e usar imagens imutáveis em produção. |

## Dependências observadas hoje

```text
Core/Auth -- HTTP ---> kerosene-kfe irmão (fronteira de runtime)
Core/Auth + KFE ------> kerosene-shared irmão
Core/Auth/KFE/Shared -> kerosene-contracts irmão
kerosene-admin          repositório independente
kerosene-rails          repositório independente
kerosene-deploy         repositório independente de empacotamento/orquestração
```

Core e KFE não consomem mais os artefatos de implementação um do outro. A
superfície compartilhada restante é a API versionada de Contracts e a fronteira
HTTP em runtime.

## Regras desta fase de organização

- Não adicionar features, transportes, consenso ou protocolos de segurança.
- Não restaurar KFE, Shared, Contracts, Admin ou Rails dentro do Core.
- Não colocar Compose, Kubernetes, secrets ou política de ambiente no Core.
- Preservar o comportamento enquanto cada fronteira futura é documentada e testada.
