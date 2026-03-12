# Observability

## Health Endpoints

- `GET /health`: lightweight service health endpoint
- `GET /actuator/health`: Spring Boot actuator health endpoint

Expected examples:

```json
{"status":"UP","service":"merchantops-saas"}
```

```json
{"status":"UP"}
```

## Request Tracing

The API uses `X-Request-Id` for request correlation.

- If the client sends `X-Request-Id`, the server normalizes it before echoing and persisting it
- If the client omits it, the server generates a UUID
- Normalization currently means:
  - surrounding whitespace is trimmed
  - the value is capped at `128` characters
  - oversized values are shortened deterministically instead of being rejected
- The response always includes `X-Request-Id`
- Console logs include the same `requestId` value through MDC

Quick check:

```bash
curl -i http://localhost:8080/health
curl -i -H "X-Request-Id: demo-request-id-001" http://localhost:8080/health
```

PowerShell:

```powershell
curl.exe -i http://localhost:8080/health
curl.exe -i -H "X-Request-Id: demo-request-id-001" http://localhost:8080/health
```
