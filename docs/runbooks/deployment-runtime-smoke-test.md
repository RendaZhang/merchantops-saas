# Deployment Runtime Smoke Test

Last updated: 2026-04-29

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
Invoke-WebRequest -Method Get -Uri "$adminBaseUrl/tickets"
Invoke-WebRequest -Method Get -Uri "$adminBaseUrl/feature-flags"
Invoke-WebRequest -Method Get -Uri "$adminBaseUrl/imports"
```

Expected result:

- `/health` returns `UP`.
- `/actuator/health` returns `UP`.
- `http://localhost:8081/` returns the admin HTML shell.
- `http://localhost:8081/tickets` returns the same admin HTML shell through SPA history fallback.
- `http://localhost:8081/feature-flags` returns the same admin HTML shell through SPA history fallback.
- `http://localhost:8081/imports` returns the same admin HTML shell through SPA history fallback.

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

$tickets = Invoke-RestMethod `
  -Method Get `
  -Uri "$adminBaseUrl/api/v1/tickets?page=0&size=10" `
  -Headers $headers

$imports = Invoke-RestMethod `
  -Method Get `
  -Uri "$adminBaseUrl/api/v1/import-jobs?page=0&size=10" `
  -Headers $headers

$featureFlags = Invoke-RestMethod `
  -Method Get `
  -Uri "$adminBaseUrl/api/v1/feature-flags" `
  -Headers $headers

$firstFeatureFlag = $featureFlags.data.items[0]
$targetFeatureFlagEnabled = -not [bool]$firstFeatureFlag.enabled

$featureFlagUpdate = Invoke-RestMethod `
  -Method Put `
  -Uri "$adminBaseUrl/api/v1/feature-flags/$($firstFeatureFlag.key)" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body (@{
    enabled = $targetFeatureFlagEnabled
  } | ConvertTo-Json -Compress)

$featureFlagRestore = Invoke-RestMethod `
  -Method Put `
  -Uri "$adminBaseUrl/api/v1/feature-flags/$($firstFeatureFlag.key)" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body (@{
    enabled = [bool]$firstFeatureFlag.enabled
  } | ConvertTo-Json -Compress)

$logout = Invoke-RestMethod `
  -Method Post `
  -Uri "$adminBaseUrl/api/v1/auth/logout" `
  -Headers $headers

$logoutAllLoginA = Invoke-RestMethod `
  -Method Post `
  -Uri "$adminBaseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    tenantCode = "demo-shop"
    username = "admin"
    password = "123456"
  } | ConvertTo-Json -Compress)

$logoutAllLoginB = Invoke-RestMethod `
  -Method Post `
  -Uri "$adminBaseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    tenantCode = "demo-shop"
    username = "admin"
    password = "123456"
  } | ConvertTo-Json -Compress)

$logoutAllTokenA = $logoutAllLoginA.data.accessToken
$logoutAllTokenB = $logoutAllLoginB.data.accessToken
$logoutAllHeaders = @{ Authorization = "Bearer $logoutAllTokenA" }

$logoutAll = Invoke-RestMethod `
  -Method Post `
  -Uri "$adminBaseUrl/api/v1/auth/logout-all" `
  -Headers $logoutAllHeaders
```

Expected result:

- login returns an access token
- context returns `tenantCode=demo-shop` and `username=admin`
- tickets returns `page=0`, `size=10`, an `items` array, and the current tenant's first ticket page
- imports returns `page=0`, `size=10`, an `items` array, and the current tenant's first import-job page or an empty list
- feature flags returns the fixed eight-key inventory for the current tenant
- feature flag update returns the requested toggled state and restore returns the original state
- logout returns `SUCCESS` with `data=null`
- logout-all returns `SUCCESS` with `data=null`

Verify the old tokens are revoked:

```powershell
curl.exe -i -H "Authorization: Bearer $token" "$adminBaseUrl/api/v1/context"
curl.exe -i -H "Authorization: Bearer $logoutAllTokenA" "$adminBaseUrl/api/v1/context"
curl.exe -i -H "Authorization: Bearer $logoutAllTokenB" "$adminBaseUrl/api/v1/context"
```

Expected result:

- `HTTP/1.1 401`
- response envelope has `code=UNAUTHORIZED` and `message=authentication required`

## 6. Browser Check

Open `http://localhost:8081`.

1. Log in with `demo-shop` / `admin` / `123456`.
2. Confirm the dashboard renders tenant and operator context.
3. Open `Tickets` and confirm `/tickets` renders the read-only current tenant ticket queue.
4. Open `Feature Flags` and confirm `/feature-flags` renders eight current-tenant feature flags.
5. Open `Imports` and confirm `/imports` renders the read-only current tenant import-job queue or empty state.
6. Toggle one feature flag and restore the original value.
7. Sign out, log in with `ops` or `viewer`, open `/feature-flags`, and confirm `权限不足` appears without returning to login.
8. Refresh `/tickets`, `/feature-flags`, and `/imports` and confirm context plus route data restore while the session is active.
9. Select `Sign out` and confirm the login screen returns.
10. Log in again, select `Sign out all sessions`, and confirm the login screen returns.

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
