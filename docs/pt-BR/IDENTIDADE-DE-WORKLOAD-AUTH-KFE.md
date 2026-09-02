# Identidade de workload entre Auth e KFE

Auth continua sendo o único gateway público para `/kfe/**`,
`/api/public/kfe/**` e `/api/admin/kfe/**`; a borda web nunca acessa o KFE
diretamente. O KFE é acessado somente pela porta HTTPS interna dedicada. Auth recebe um X.509-SVID curto pela Workload API,
prova o ID `.../service/auth` e aceita apenas o URI SAN exato configurado como
`.../service/kfe`. A mesma regra, invertida, protege callbacks em
`/internal/kfe/**`.

Configuração obrigatória em produção:

- `SPIFFE_ENDPOINT_SOCKET=unix:///spiffe-workload-api/spire-agent.sock`
- `KEROSENE_OWN_SPIFFE_ID=spiffe://<dominio-de-confianca>/service/auth`
- `KEROSENE_PEER_SPIFFE_ID=spiffe://<dominio-de-confianca>/service/kfe`
- `KEROSENE_INTERNAL_MTLS_PORT=8443`
- `KFE_INTERNAL_BASE_URL=https://kfe-service:8443`
- `KFE_REMOTE_BASE_URL=https://kfe-service:8443`

Produção recusa URL sem TLS, endpoint da Workload API que não seja Unix,
reutilização da porta pública, identidade ausente e qualquer valor em
`KFE_INTERNAL_SHARED_SECRET`. A porta pública não executa callbacks do KFE,
porque o filtro exige simultaneamente a porta interna e o SVID correto.

Código e manifests não provam que o cluster está pronto. Antes da ativação,
execute o preflight do deploy e um teste Auth↔KFE real cobrindo SVID ausente,
incorreto e expirado. Mantenha a ativação desligada até anexar essa evidência à
issue de deploy.
