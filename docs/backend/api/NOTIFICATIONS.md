# Notifications API

> Fonte de verdade desta página: controllers, DTOs, records, enums e `EndpointPolicyRegistry` do backend Spring Boot em `/src/main/java`.
> Quando uma rota existe no controller mas não está declarada no `EndpointPolicyRegistry`, ela é documentada como `DENIED_BY_DEFAULT`, porque `Security.anyRequest().denyAll()` bloqueia a chamada antes do método executar.

## Convenções globais

### Envelope padrão `ApiResponse<T>`

A maior parte dos endpoints retorna o envelope abaixo. Endpoints explicitamente marcados como `raw` retornam o payload diretamente sem envelope. Campos `null` podem ser omitidos por `@JsonInclude`.

| Campo | Tipo | Nullable | Descrição | Exemplo |
|---|---:|:---:|---|---|
| `success` | boolean | não | Indica sucesso lógico da operação. | `true` |
| `message` | string | sim | Mensagem operacional retornada pelo controller/service. | `KFE wallet created.` |
| `data` | object/array/string | sim | Payload específico do endpoint. | `{...}` |
| `errorCode` | string | sim | Código de erro de negócio/validação quando `success=false`. | `AUTH_INVALID_CREDENTIALS` |
| `timestamp` | string date-time | não | Momento de montagem do envelope no servidor. | `2026-06-19T10:30:00` |

### Headers comuns

| Header | Tipo | Obrigatório | Quando usar | Exemplo |
|---|---:|:---:|---|---|
| `Content-Type` | string | sim em requests com body | Deve ser `application/json` para JSON. | `application/json` |
| `Authorization` | string | sim para `AUTHENTICATED`/`ADMIN` | Bearer token JWT emitido por login/TOTP/passkey/device-key. | `Bearer eyJhbGciOi...` |
| `X-Correlation-Id` | string | não | Correlação distribuída entre serviços/logs. Aceita caracteres seguros de 8 a 64 posições. | `req-20260619-0001` |
| `X-Request-Id` | string | não | Identificador idempotente/observável da requisição. | `mobile-01HD...` |
| `X-Device-Hash` | string | condicional | Usado por endpoints device-scoped de segurança/PIN. | `sha256:abc123...` |
| `X-Idempotency-Key` | string | recomendado | Recomendado em operações financeiras, embora KFE use `idempotencyKey` no body. | `txn-01J...` |

### Classes de autenticação

| Valor na documentação | Significado efetivo |
|---|---|
| `PUBLIC` | Permitido sem JWT. |
| `AUTHENTICATED` | Exige JWT válido. |
| `ADMIN` | Exige JWT com role `ADMIN`; alguns métodos também usam `@PreAuthorize`. |
| `DENIED_BY_DEFAULT` | Controller/DTO pode existir, mas a rota não tem policy e é bloqueada por `anyRequest().denyAll()`. |
| `STALE` | Documento legado sem controller ativo confirmado no código atual. |

### Estruturas de erro

Erro via envelope `ApiResponse`:

```json
{
  "success": false,
  "message": "Invalid token context",
  "data": null,
  "errorCode": "AUTH_SESSION_EXPIRED",
  "timestamp": "2026-06-19T10:30:00"
}
```

Erro MVC/validação ou fallback pode aparecer como `ResponseError`/erro Spring, dependendo do ponto da falha:

```json
{
  "timestamp": "2026-06-19T10:30:00",
  "status": "BAD_REQUEST",
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/auth/signup"
}
```

### Status codes comuns

| Status | Quando ocorre | Como resolver |
|---:|---|---|
| `200 OK` | Consulta ou operação concluída. | Consumir `data`. |
| `201 Created` | Recurso criado. | Persistir o `id` retornado. |
| `202 Accepted` | Fluxo assíncrono/pendente, comum em login com TOTP/aprovação. | Continuar no próximo passo indicado. |
| `204 No Content` | Operação concluída sem corpo. | Não tentar parsear JSON. |
| `400 Bad Request` | JSON inválido, campo faltante, enum inválido ou regra básica violada. | Corrigir request body/query/path. |
| `401 Unauthorized` | JWT ausente/inválido/expirado ou credencial incorreta. | Fazer login/renovar token ou reenviar credenciais. |
| `403 Forbidden` | Usuário autenticado sem permissão, admin token inválido ou policy nega acesso. | Verificar role/headers/policy. |
| `404 Not Found` | Recurso, sessão, dispositivo ou endpoint não encontrado. | Verificar identificadores. |
| `409 Conflict` | Duplicidade/idempotência/conflito de estado. | Consultar recurso existente ou trocar chave idempotente. |
| `410 Gone` | Sessão de recuperação expirada. | Reiniciar o fluxo. |
| `412 Precondition Failed` | Pré-condição criptográfica/security step-up falhou. | Refazer challenge/assinatura/fator. |
| `422 Unprocessable Entity` | Regra de negócio não satisfeita. | Ajustar estado da conta/recurso antes de repetir. |
| `429 Too Many Requests` | Rate limit. | Aplicar backoff exponencial. |
| `500 Internal Server Error` | Falha não tratada. | Registrar `X-Correlation-Id` e abrir incidente. |
| `503 Service Unavailable` | Dependência indisponível ou health `DOWN`. | Tentar novamente ou acionar operação. |

## Visão geral do serviço

API de notificações e tokens de dispositivo. Alguns endpoints retornam payload raw em vez do envelope `ApiResponse`.





## Endpoints

## List notifications

**Método e URL:** `GET /notifications`  
**Autenticação efetiva:** `AUTHENTICATED`  
**Tipo de resposta:** `raw`

**O que faz:** Lista notificações do usuário autenticado.

**Quando usar:** Use na inbox/central de notificações.

**Regras de negócio e limitações:** Retorna payload raw `List<NotificationEntity>`, não `ApiResponse`.

### Headers obrigatórios

| Nome | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| Authorization | string | sim | Bearer JWT com permissão adequada. | Bearer <JWT> |

### Headers opcionais

| Nome | Tipo | Descrição | Exemplo |
|---|---|---|---|
| X-Correlation-Id | string | Rastreabilidade fim a fim. | req-20260619-0001 |
| X-Request-Id | string | Identificador do cliente para logs. | mobile-01J... |

### Path Parameters

| Nome | Tipo | Obrigatório | Descrição | Exemplo | Restrições |
|---|---|---|---|---|---|
| Nenhum | - | - | - | - | - |

### Query Parameters

| Nome | Tipo | Obrigatório | Default | Descrição | Exemplo |
|---|---|---|---|---|---|
| Nenhum | - | - | - | - | - |

### Request Body

Este endpoint não recebe body.


### Exemplo de requisição

```bash
curl -X GET 'http://localhost:8080/notifications' \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer <JWT>'
```

### Response de sucesso

**Status:** `200 OK`  
**Descrição:** Operação concluída.

Campos retornados:

| Campo | Tipo | Nullable | Descrição | Exemplo |
|---|---|---|---|---|
| [] | array<NotificationEntity> | não | Lista raw de notificações. | [{...}] |
| id | long | não | ID da notificação. | 1 |
| userId | long | não | Usuário dono. | 42 |
| title | string | sim | Título. | Pagamento recebido |
| body | string | sim | Mensagem. | Você recebeu sats |
| kind | string | sim | Tipo lógico. | PAYMENT |
| severity | string | sim | Severidade. | INFO |
| deeplink | string | sim | Deep link app. | kerosene://tx/018f |
| entityType | string | sim | Tipo relacionado. | TRANSACTION |
| entityId | string | sim | ID relacionado. | 018f... |
| metadata | object | sim | Metadados. | {} |
| readAt | date-time | sim | Leitura. | null |
| createdAt | date-time | não | Criação. | 2026-06-19T10:30:00 |

Exemplo completo:

```json
[
  {
    "id": 1,
    "userId": 42,
    "title": "Pagamento recebido",
    "body": "Você recebeu 25000 sats",
    "kind": "PAYMENT",
    "severity": "INFO",
    "deeplink": "kerosene://tx/018f",
    "entityType": "TRANSACTION",
    "entityId": "018f",
    "metadata": {},
    "readAt": null,
    "createdAt": "2026-06-19T10:30:00"
  }
]
```

### Status codes específicos

| Status | Nome | Quando ocorre | Como resolver |
|---|---|---|---|
| 200 | OK | Operação descrita acima. | Consumir response conforme schema. |
| 401 | Unauthorized | JWT ausente, inválido ou expirado. | Refazer autenticação. |
| 403 | Forbidden | Token sem permissão ou policy nega acesso. | Validar role/policy. |

### Exemplo de erro

```json
{
  "success": false,
  "message": "Request rejected or validation failed",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-06-19T10:30:00"
}
```

## Mark notification as read

**Método e URL:** `PUT /notifications/{id}/read`  
**Autenticação efetiva:** `AUTHENTICATED`  
**Tipo de resposta:** `raw`

**O que faz:** Marca uma notificação como lida.

**Quando usar:** Use quando o usuário abrir ou dispensar uma notificação.

**Regras de negócio e limitações:** Retorna `200` sem body (`Void`).

### Headers obrigatórios

| Nome | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| Authorization | string | sim | Bearer JWT com permissão adequada. | Bearer <JWT> |

### Headers opcionais

| Nome | Tipo | Descrição | Exemplo |
|---|---|---|---|
| X-Correlation-Id | string | Rastreabilidade fim a fim. | req-20260619-0001 |
| X-Request-Id | string | Identificador do cliente para logs. | mobile-01J... |

### Path Parameters

| Nome | Tipo | Obrigatório | Descrição | Exemplo | Restrições |
|---|---|---|---|---|---|
| id | long | sim | ID da notificação. | 1 | Long positivo |

### Query Parameters

| Nome | Tipo | Obrigatório | Default | Descrição | Exemplo |
|---|---|---|---|---|---|
| Nenhum | - | - | - | - | - |

### Request Body

Este endpoint não recebe body.


### Exemplo de requisição

```bash
curl -X PUT 'http://localhost:8080/notifications/{id}/read' \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer <JWT>'
```

### Response de sucesso

**Status:** `200 OK`  
**Descrição:** Sem body.

Campos retornados:

| Campo | Tipo | Nullable | Descrição | Exemplo |
|---|---|---|---|---|
| body | void | sim | Sem payload. |  |

### Status codes específicos

| Status | Nome | Quando ocorre | Como resolver |
|---|---|---|---|
| 200 | OK | Operação descrita acima. | Consumir response conforme schema. |
| 401 | Unauthorized | JWT ausente, inválido ou expirado. | Refazer autenticação. |
| 403 | Forbidden | Token sem permissão ou policy nega acesso. | Validar role/policy. |

### Exemplo de erro

```json
{
  "success": false,
  "message": "Request rejected or validation failed",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-06-19T10:30:00"
}
```

## Register device token

**Método e URL:** `POST /notifications/register-token`  
**Autenticação efetiva:** `AUTHENTICATED`  
**Tipo de resposta:** `raw`

**O que faz:** Registra ou atualiza token de push do dispositivo.

**Quando usar:** Use após login ou refresh de token FCM/APNS.

**Regras de negócio e limitações:** Retorna `DeviceTokenResponse` raw.

### Headers obrigatórios

| Nome | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| Authorization | string | sim | Bearer JWT com permissão adequada. | Bearer <JWT> |
| Content-Type | string | sim | JSON request body. | application/json |

### Headers opcionais

| Nome | Tipo | Descrição | Exemplo |
|---|---|---|---|
| X-Correlation-Id | string | Rastreabilidade fim a fim. | req-20260619-0001 |
| X-Request-Id | string | Identificador do cliente para logs. | mobile-01J... |

### Path Parameters

| Nome | Tipo | Obrigatório | Descrição | Exemplo | Restrições |
|---|---|---|---|---|---|
| Nenhum | - | - | - | - | - |

### Query Parameters

| Nome | Tipo | Obrigatório | Default | Descrição | Exemplo |
|---|---|---|---|---|---|
| Nenhum | - | - | - | - | - |

### Request Body

| Campo | Tipo | Obrigatório | Nullable | Default | Validações | Descrição | Exemplo |
|---|---|---|---|---|---|---|---|
| platform | string | sim | não | - | ex.: android|ios|web | Plataforma. | android |
| token | string | sim | não | - | token push | Token FCM/APNS. | fcm-token |
| deviceId | string | sim | não | - | id do device | Device id. | device-123 |
| appVersion | string | não | sim | - | semver/build | Versão app. | 1.0.0 |

Exemplo de body:

```json
{
  "platform": "android",
  "token": "fcm-token",
  "deviceId": "device-123",
  "appVersion": "1.0.0"
}
```

### Exemplo de requisição

```bash
curl -X POST 'http://localhost:8080/notifications/register-token' \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  --data '{
  "platform": "android",
  "token": "fcm-token",
  "deviceId": "device-123",
  "appVersion": "1.0.0"
}'
```

### Response de sucesso

**Status:** `200 OK`  
**Descrição:** Operação concluída.

Campos retornados:

| Campo | Tipo | Nullable | Descrição | Exemplo |
|---|---|---|---|---|
| id | long | não | ID do registro. | 1 |
| platform | string | não | Plataforma. | android |
| tokenRef | string | não | Referência segura do token, não o token puro. | tok_abc |
| deviceRef | string | não | Referência do device. | dev_abc |
| appVersion | string | sim | Versão. | 1.0.0 |
| createdAt | date-time | não | Criação. | 2026-06-19T10:30:00 |
| lastSeenAt | date-time | não | Último uso. | 2026-06-19T10:30:00 |
| revokedAt | date-time | sim | Revogação. | null |
| active | boolean | não | Ativo se não revogado. | true |

Exemplo completo:

```json
{
  "id": 1,
  "platform": "android",
  "tokenRef": "tok_abc",
  "deviceRef": "dev_abc",
  "appVersion": "1.0.0",
  "createdAt": "2026-06-19T10:30:00",
  "lastSeenAt": "2026-06-19T10:30:00",
  "revokedAt": null,
  "active": true
}
```

### Status codes específicos

| Status | Nome | Quando ocorre | Como resolver |
|---|---|---|---|
| 200 | OK | Operação descrita acima. | Consumir response conforme schema. |
| 401 | Unauthorized | JWT ausente, inválido ou expirado. | Refazer autenticação. |
| 403 | Forbidden | Token sem permissão ou policy nega acesso. | Validar role/policy. |
| 400 | Bad Request | Body inválido, enum inválido ou validação Bean Validation. | Corrigir payload. |

### Exemplo de erro

```json
{
  "success": false,
  "message": "Request rejected or validation failed",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-06-19T10:30:00"
}
```

## List active device tokens

**Método e URL:** `GET /notifications/device-tokens`  
**Autenticação efetiva:** `AUTHENTICATED`  
**Tipo de resposta:** `raw`

**O que faz:** Lista tokens ativos do usuário.

**Quando usar:** Use em tela de dispositivos/notificações.

**Regras de negócio e limitações:** Retorna lista raw de `DeviceTokenResponse`.

### Headers obrigatórios

| Nome | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| Authorization | string | sim | Bearer JWT com permissão adequada. | Bearer <JWT> |

### Headers opcionais

| Nome | Tipo | Descrição | Exemplo |
|---|---|---|---|
| X-Correlation-Id | string | Rastreabilidade fim a fim. | req-20260619-0001 |
| X-Request-Id | string | Identificador do cliente para logs. | mobile-01J... |

### Path Parameters

| Nome | Tipo | Obrigatório | Descrição | Exemplo | Restrições |
|---|---|---|---|---|---|
| Nenhum | - | - | - | - | - |

### Query Parameters

| Nome | Tipo | Obrigatório | Default | Descrição | Exemplo |
|---|---|---|---|---|---|
| Nenhum | - | - | - | - | - |

### Request Body

Este endpoint não recebe body.


### Exemplo de requisição

```bash
curl -X GET 'http://localhost:8080/notifications/device-tokens' \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer <JWT>'
```

### Response de sucesso

**Status:** `200 OK`  
**Descrição:** Operação concluída.

Campos retornados:

| Campo | Tipo | Nullable | Descrição | Exemplo |
|---|---|---|---|---|
| [] | array<DeviceTokenResponse> | não | Tokens ativos. | [{...}] |

Exemplo completo:

```json
[
  {
    "id": 1,
    "platform": "android",
    "tokenRef": "tok_abc",
    "deviceRef": "dev_abc",
    "appVersion": "1.0.0",
    "createdAt": "2026-06-19T10:30:00",
    "lastSeenAt": "2026-06-19T10:30:00",
    "revokedAt": null,
    "active": true
  }
]
```

### Status codes específicos

| Status | Nome | Quando ocorre | Como resolver |
|---|---|---|---|
| 200 | OK | Operação descrita acima. | Consumir response conforme schema. |
| 401 | Unauthorized | JWT ausente, inválido ou expirado. | Refazer autenticação. |
| 403 | Forbidden | Token sem permissão ou policy nega acesso. | Validar role/policy. |

### Exemplo de erro

```json
{
  "success": false,
  "message": "Request rejected or validation failed",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-06-19T10:30:00"
}
```

## Revoke device token

**Método e URL:** `DELETE /notifications/device-tokens/{id}`  
**Autenticação efetiva:** `AUTHENTICATED`  
**Tipo de resposta:** `raw`

**O que faz:** Revoga token de push.

**Quando usar:** Use em logout, remoção de dispositivo ou opt-out.

**Regras de negócio e limitações:** Retorna `204 No Content`.

### Headers obrigatórios

| Nome | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| Authorization | string | sim | Bearer JWT com permissão adequada. | Bearer <JWT> |

### Headers opcionais

| Nome | Tipo | Descrição | Exemplo |
|---|---|---|---|
| X-Correlation-Id | string | Rastreabilidade fim a fim. | req-20260619-0001 |
| X-Request-Id | string | Identificador do cliente para logs. | mobile-01J... |

### Path Parameters

| Nome | Tipo | Obrigatório | Descrição | Exemplo | Restrições |
|---|---|---|---|---|---|
| id | long | sim | ID do token. | 1 | Long positivo |

### Query Parameters

| Nome | Tipo | Obrigatório | Default | Descrição | Exemplo |
|---|---|---|---|---|---|
| Nenhum | - | - | - | - | - |

### Request Body

Este endpoint não recebe body.


### Exemplo de requisição

```bash
curl -X DELETE 'http://localhost:8080/notifications/device-tokens/{id}' \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer <JWT>'
```

### Response de sucesso

**Status:** `204 No Content`  
**Descrição:** Token revogado; sem body.

Campos retornados:

| Campo | Tipo | Nullable | Descrição | Exemplo |
|---|---|---|---|---|
| body | void | sim | Sem payload. |  |

### Status codes específicos

| Status | Nome | Quando ocorre | Como resolver |
|---|---|---|---|
| 204 | No Content | Operação descrita acima. | Consumir response conforme schema. |
| 401 | Unauthorized | JWT ausente, inválido ou expirado. | Refazer autenticação. |
| 403 | Forbidden | Token sem permissão ou policy nega acesso. | Validar role/policy. |

### Exemplo de erro

```json
{
  "success": false,
  "message": "Request rejected or validation failed",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-06-19T10:30:00"
}
```
