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

## 2. Build Required Modules

Run from the repository root:

```bash
./mvnw -pl merchantops-api -am -DskipTests install
```

PowerShell:

```powershell
mvnw.cmd -pl merchantops-api -am -DskipTests install
```

## 3. Start the API Module

Use the module POM so Maven resolves the Spring Boot plugin correctly:

```bash
./mvnw -f merchantops-api/pom.xml spring-boot:run
```

PowerShell:

```powershell
mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

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
