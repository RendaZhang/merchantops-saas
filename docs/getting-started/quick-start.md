# Quick Start

## 1. Start Local Dependencies

```bash
cp .env.example .env
docker compose up -d
```

PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

The local infra stack keeps MySQL, Redis, and RabbitMQ on the pinned bridge network `merchantops-infra`. Containers on that network can reach the services by hostname `mysql`, `redis`, and `rabbitmq`.

## 2. Choose An API Startup Path

### Option A. Local Maven Startup

The repository-root local `.env` is auto-loaded by the API main entrypoint during dev-profile `spring-boot:run`, so local AI provider keys belong there instead of in tracked config files.

Run from the repository root:

```bash
./mvnw -pl merchantops-api -am -DskipTests install
```

PowerShell:

```powershell
.\mvnw.cmd -pl merchantops-api -am -DskipTests install
```

Why this step matters:

- `merchantops-api` depends on sibling modules from the same repository, including `merchantops-infra`
- `-am` rebuilds those modules in the same reactor instead of relying on whatever SNAPSHOT jars are already in your local Maven cache
- if later local runs fail with missing repository classes, missing entity fields, or stale signatures, rerun this step before debugging the API module itself

Start the API module:

Use the module POM so Maven resolves the Spring Boot plugin correctly:

```bash
./mvnw -f merchantops-api/pom.xml spring-boot:run
```

PowerShell:

```powershell
.\mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

If you changed sibling-module signatures, JPA entities, or repositories, rerun step 2 before starting again so `spring-boot:run` does not pick up stale local SNAPSHOT artifacts.

Profile override example:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

### Option B. Dockerized API Startup

Build the image from the repository root:

```bash
docker build -t merchantops-api:local .
```

PowerShell:

```powershell
docker build -t merchantops-api:local .
```

Run the API container on the same bridge network as the compose-managed infra:

```bash
docker run --rm --name merchantops-api-local \
  --env-file .env \
  --network merchantops-infra \
  -p 8080:8080 \
  -e MYSQL_HOST=mysql \
  -e REDIS_HOST=redis \
  -e RABBITMQ_HOST=rabbitmq \
  merchantops-api:local
```

PowerShell:

```powershell
docker run --rm --name merchantops-api-local `
  --env-file .env `
  --network merchantops-infra `
  -p 8080:8080 `
  -e MYSQL_HOST=mysql `
  -e REDIS_HOST=redis `
  -e RABBITMQ_HOST=rabbitmq `
  merchantops-api:local
```

Notes:

- the image packages only the `merchantops-api` Spring Boot boot jar, but it is built from the repository-root reactor so sibling modules stay in sync
- the image does not read the repository-root `.env` automatically; use `--env-file .env` plus explicit `MYSQL_HOST`, `REDIS_HOST`, and `RABBITMQ_HOST` values
- port `8080` stays the default API port on both startup paths

## 3. Verify Startup

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health endpoint: `http://localhost:8080/health`
- Actuator health endpoint: `http://localhost:8080/actuator/health`

Minimal protected smoke:

```powershell
$baseUrl = "http://localhost:8080"
$login = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    tenantCode = "demo-shop"
    username = "admin"
    password = "123456"
  } | ConvertTo-Json -Compress)

$token = $login.data.accessToken
Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/context" -Headers @{ Authorization = "Bearer $token" }
```

For a fuller verification flow, see [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md).
