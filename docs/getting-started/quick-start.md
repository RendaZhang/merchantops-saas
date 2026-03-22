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

The same repository-root local `.env` is also auto-loaded by the API main entrypoint during dev-profile `spring-boot:run`, so local AI provider keys belong there instead of in tracked config files.

## 2. Build Required Modules

Run from the repository root:

```bash
./mvnw -pl merchantops-api -am -DskipTests install
```

PowerShell:

```powershell
mvnw.cmd -pl merchantops-api -am -DskipTests install
```

Why this step matters:

- `merchantops-api` depends on sibling modules from the same repository, including `merchantops-infra`
- `-am` rebuilds those modules in the same reactor instead of relying on whatever SNAPSHOT jars are already in your local Maven cache
- if later local runs fail with missing repository classes, missing entity fields, or stale signatures, rerun this step before debugging the API module itself

## 3. Start the API Module

Use the module POM so Maven resolves the Spring Boot plugin correctly:

```bash
./mvnw -f merchantops-api/pom.xml spring-boot:run
```

PowerShell:

```powershell
mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

If you changed sibling-module signatures, JPA entities, or repositories, rerun step 2 before starting again so `spring-boot:run` does not pick up stale local SNAPSHOT artifacts.

Profile override example:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

## 4. Verify Startup

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health endpoint: `http://localhost:8080/health`

For a fuller verification flow, see [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md).
