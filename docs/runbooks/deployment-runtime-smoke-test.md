# Deployment Runtime Smoke Test

Last updated: 2026-04-17

Use this runbook when a change touches Docker delivery, runtime environment injection, admin-console packaging, or the same-origin `/api` proxy path.

This smoke proves the production-like local runtime contract:

- MySQL, Redis, and RabbitMQ run from the existing infra compose file.
- The API runs from the repository-root Docker image with `SPRING_PROFILES_ACTIVE=runtime`.
- The admin console runs from its Nginx image on `http://localhost:8081`.
- Browser API calls use same-origin `/api/...` through Nginx, not the Vite dev proxy.

It does not prove TLS, image publishing, K8s, Helm, a real secret manager, live AI provider behavior, refresh tokens, cookie sessions, token rotation, or CORS.

## 1. Prepare Local Environment

From the repository root:

```powershell
Copy-Item .env.example .env -ErrorAction SilentlyContinue
```

If `.env` already existed before Productization Baseline Slice C, confirm it contains a local-only `JWT_SECRET` value with at least 32 bytes of key material. The tracked `.env.example` includes a demo value for local smoke only; replace it for any real deployment.

Required local runtime values:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `RABBITMQ_DEFAULT_USER`
- `RABBITMQ_DEFAULT_PASS`
- `JWT_SECRET`
- `TZ`

Optional AI provider values such as `MERCHANTOPS_AI_API_KEY` can stay blank for this smoke.

## 2. Build Images

```powershell
docker build -t merchantops-api:local .
docker build -t merchantops-admin-web:local .\merchantops-admin-web
```

## 3. Start Runtime Stack

```powershell
docker compose -f docker-compose.yml -f docker-compose.runtime.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.runtime.yml ps
```

Expected result:

- `merchantops-mysql`, `merchantops-redis`, and `merchantops-rabbitmq` become healthy.
- `merchantops-api-runtime` starts on port `8080`.
- `merchantops-admin-web-runtime` starts on port `8081`.
- if a new Flyway migration was added, `docker logs merchantops-api-runtime` shows Flyway validating and applying through the new version.

## 4. Health Checks

```powershell
$apiBaseUrl = "http://localhost:8080"
$adminBaseUrl = "http://localhost:8081"

Invoke-RestMethod -Method Get -Uri "$apiBaseUrl/health"
Invoke-RestMethod -Method Get -Uri "$apiBaseUrl/actuator/health"
Invoke-WebRequest -Method Get -Uri "$adminBaseUrl/"
```

Expected result:

- `/health` returns `UP`.
- `/actuator/health` returns `UP`.
- `http://localhost:8081/` returns the admin HTML shell.

## 5. Same-Origin Auth Smoke

Use the admin origin for every API request in this section:

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri "$adminBaseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    tenantCode = "demo-shop"
    username = "admin"
    password = "123456"
  } | ConvertTo-Json -Compress)

$token = $login.data.accessToken
$headers = @{ Authorization = "Bearer $token" }

$context = Invoke-RestMethod `
  -Method Get `
  -Uri "$adminBaseUrl/api/v1/context" `
  -Headers $headers

$logout = Invoke-RestMethod `
  -Method Post `
  -Uri "$adminBaseUrl/api/v1/auth/logout" `
  -Headers $headers
```

Expected result:

- login returns an access token
- context returns `tenantCode=demo-shop` and `username=admin`
- logout returns `SUCCESS` with `data=null`

Verify the old token is revoked:

```powershell
curl.exe -i -H "Authorization: Bearer $token" "$adminBaseUrl/api/v1/context"
```

Expected result:

- `HTTP/1.1 401`
- response envelope has `code=UNAUTHORIZED` and `message=authentication required`

## 6. Browser Check

Open `http://localhost:8081`.

1. Log in with `demo-shop` / `admin` / `123456`.
2. Confirm the dashboard renders tenant and operator context.
3. Refresh the page and confirm context restores while the session is active.
4. Select `Sign out` and confirm the login screen returns.

Do not use `http://localhost:5173` for this runbook; that is the Vite dev-server path.

## 7. Stop Runtime Stack

```powershell
docker compose -f docker-compose.yml -f docker-compose.runtime.yml down
```

Use `docker compose -f docker-compose.yml -f docker-compose.runtime.yml down -v` only when intentionally resetting local data and re-running Flyway from a fresh database.

## Troubleshooting

- If the API exits during startup, check `docker logs merchantops-api-runtime`. Missing `JWT_SECRET`, database credentials, or RabbitMQ credentials indicate runtime env injection is incomplete.
- If `http://localhost:8081` loads but `/api/...` calls fail, check `docker logs merchantops-admin-web-runtime` and confirm the API service name is `merchantops-api`.
- If login returns `403`, confirm the local database still has the seeded `demo-shop` admin user and was not modified by earlier smoke data cleanup.
