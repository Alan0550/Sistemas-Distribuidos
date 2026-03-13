# Banco (cliente) - Checklist de prueba HMAC webhook

Este archivo resume lo minimo que debe hacer el lado banco para probar el webhook firmado.

## 1) Datos que necesitas del servidor

- URL webhook (ejemplo): `http://localhost:1915/tm/webhook/banco` o la URL publica de tu compañero.
- Secreto compartido HMAC: `BANK_WEBHOOK_SECRET` (mismo valor en ambos lados).
- Formato esperado del body (JSON exacto).

## 2) Script listo para enviar

Usa: `simulate-bank-webhook.ps1`

### Caso valido (firma correcta)

```powershell
cd "C:\Program Files\ticket_master-master"
.\simulate-bank-webhook.ps1 `
  -Url "http://localhost:1915/tm/webhook/banco" `
  -Secret "change-me" `
  -Body '{"evento":"pago","monto":100}'
```

Esperado: `200` y mensaje tipo `{"status":"OK","message":"Firma valida"}`.

### Caso invalido (firma alterada)

```powershell
cd "C:\Program Files\ticket_master-master"
.\simulate-bank-webhook.ps1 `
  -Url "http://localhost:1915/tm/webhook/banco" `
  -Secret "change-me" `
  -Body '{"evento":"pago","monto":100}' `
  -InvalidSignature
```

Esperado: `401`.

### Caso OPTIONS (preflight)

```powershell
cd "C:\Program Files\ticket_master-master"
.\simulate-bank-webhook.ps1 `
  -Url "http://localhost:1915/tm/webhook/banco" `
  -Secret "change-me" `
  -Body '{}' `
  -Options
```

## 3) Recomendaciones de demo

- Mostrar una prueba valida (200).
- Mostrar una prueba invalida (401).
- Mostrar el header `X-Signature` que se envia.
- Mostrar que el mismo body con otra firma falla.
