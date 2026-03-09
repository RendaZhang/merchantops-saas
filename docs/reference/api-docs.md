# API Docs

## OpenAPI Endpoints

- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Security Scheme

- Swagger exposes `bearerAuth` as the HTTP Bearer JWT scheme

## Public Endpoints

These routes are currently accessible without authentication:

- `/health`
- `/actuator/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/v3/api-docs/**`
- `/api/v1/dev/**`
- `/api/v1/auth/login`

All other endpoints require a Bearer token.

## Usage Notes

- If newly added endpoints do not appear in Swagger UI, confirm you are calling the latest local process on port `8080`
- Check `/v3/api-docs` directly when debugging stale UI content
