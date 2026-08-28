# Integrações API

Documentação corporativa dos endpoints de integração externa.

Fonte real inspecionada:

- `auth-service/src/main/java/com/kerosene/common/security/EndpointPolicyRegistry.java`
- Lista atual de controllers em `/src/main/java/**`.

## Estado real do serviço

A integração BTCPay está parcialmente presente na política de segurança, pois o registry declara:

```text
/integrations/btcpay/webhook/** -> PUBLIC
```

Porém, no código-fonte atual, **não existe `BtcPayWebhookController` ativo**. Portanto, a rota deve ser tratada como:

```text
STALE_DOCUMENTATION / CONTROLLER_ABSENT
```

Na prática, isso tende a retornar `404 Not Found`, não `204`, porque a requisição não encontra handler REST.

## Endpoint legado: Webhook BTCPay

```http
POST /integrations/btcpay/webhook/{storeId}
```

### Para que serviria

Receber eventos de invoice/pagamento do BTCPay Server e sincronizar estado externo com o backend Kerosene.

### Quando usar

Somente se o controller de webhook for restaurado no backend. No estado atual do código, não configure esse endpoint no BTCPay como URL produtiva.

### Estado atual

| Item | Valor |
| --- | --- |
| Policy | `PUBLIC` para `/integrations/btcpay/webhook/**` |
| Controller ativo | Não encontrado |
| Auth esperada | Pública, com validação de assinatura do provedor quando restaurada |
| Status real provável | `404 Not Found` |

### Headers esperados se restaurado

| Nome | Tipo | Obrigatório | Descrição | Exemplo |
| --- | --- | --- | --- | --- |
| `Content-Type` | string | Sim | Payload JSON enviado pelo provedor. | `application/json` |
| `BTCPAY-SIG` | string | Recomendado/condicional | Assinatura HMAC do BTCPay. | `sha256=...` |
| `X-Correlation-Id` | string | Opcional | Rastreabilidade interna. | `btcpay-20260619-0001` |

### Path parameters

| Nome | Tipo | Obrigatório | Descrição | Exemplo |
| --- | --- | --- | --- | --- |
| `storeId` | string | Sim | ID da loja BTCPay. | `store-abc` |

### Request body esperado se restaurado

O body deve ser recebido como JSON bruto do provedor. Exemplo representativo:

```json
{
  "type": "InvoiceSettled",
  "invoiceId": "inv-123",
  "storeId": "store-abc",
  "metadata": {
    "orderId": "order-123"
  }
}
```

### Response esperada se restaurado

Status: `204 No Content`; body vazio.

### Exemplo curl esperado se restaurado

```bash
curl -X POST "$BASE_URL/integrations/btcpay/webhook/store-abc" \
  -H "Content-Type: application/json" \
  -H "BTCPAY-SIG: sha256=<signature>" \
  -d '{"type":"InvoiceSettled","invoiceId":"inv-123","storeId":"store-abc"}'
```

## Status codes

| Status | Quando ocorre | Como resolver | Exemplo |
| --- | --- | --- | --- |
| `204 No Content` | Webhook aceito, se o controller for restaurado. | Não tentar parsear body. | body vazio |
| `400 Bad Request` | Payload inválido. | Conferir evento enviado pelo BTCPay. | Varia. |
| `403 Forbidden` | Assinatura inválida, se validação for restaurada. | Corrigir secret. | Varia. |
| `404 Not Found` | Estado atual: controller ausente. | Restaurar controller/service ou remover policy stale. | Varia. |
| `500 Internal Server Error` | Falha de processamento, se restaurado. | Reprocessar com backoff e logs. | Varia. |

## Recomendação de manutenção

- Restaurar `BtcPayWebhookController` se a integração BTCPay ainda for produto ativo.
- Remover `/integrations/btcpay/webhook/**` do `EndpointPolicyRegistry` se a integração foi descontinuada.
- Ao restaurar, documentar exatamente o schema aceito pelo service de webhook e o algoritmo de validação de `BTCPAY-SIG`.
