# MerchantOps SaaS

MerchantOps SaaS is a multi-tenant backend project for merchant operations scenarios. The current repository focuses on a working Spring Boot skeleton with JWT authentication, RBAC demo endpoints, tenant/user context propagation, Flyway migrations, health checks, request tracing, and OpenAPI support.

## Target Users

- Cross-border e-commerce seller teams
- Platform operators
- Merchant administrators
- Internal support and customer service teams

## Core Goals

- Build a realistic multi-tenant SaaS backend
- Support interview storytelling and portfolio presentation
- Keep the repository easy to run and easy to extend

## Current Capabilities

- Multi-module Spring Boot backend
- JWT login and authenticated user context endpoints
- Current-tenant user listing endpoint protected by `USER_READ`
- RBAC demo endpoints with permission interception
- Flyway-based schema and seed data bootstrapping
- Health checks, request tracing, and Swagger UI

## Repository Structure

```text
merchantops-saas/
├── merchantops-api/
├── merchantops-common/
├── merchantops-domain/
├── merchantops-infra/
├── deploy/
├── docs/
├── scripts/
├── sql/
├── CHANGELOG.md
├── README.md
├── .env.example
├── docker-compose.yml
└── pom.xml
```

## Module Responsibilities

- `merchantops-api`: REST controllers, DTOs, security, and application bootstrap
- `merchantops-domain`: business models, domain services, and rules
- `merchantops-infra`: persistence, repositories, and infrastructure integration
- `merchantops-common`: shared responses, exceptions, and utilities

## Requirements

- JDK 21
- Maven Wrapper or Maven 3.9.x
- Docker Compose for the default local environment
- Access to Maven Central

## Quick Start

1. Create the local environment file and start dependencies:

```bash
cp .env.example .env
docker compose up -d
```

PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

2. Build the API module and its dependencies:

```bash
./mvnw -pl merchantops-api -am -DskipTests install
```

PowerShell:

```powershell
mvnw.cmd -pl merchantops-api -am -DskipTests install
```

3. Start the API:

```bash
./mvnw -f merchantops-api/pom.xml spring-boot:run
```

PowerShell:

```powershell
mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

After startup:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/health`

More setup details live in [docs/getting-started/README.md](docs/getting-started/README.md).

## Documentation

- [docs/README.md](docs/README.md): documentation index
- [docs/getting-started/local-environment.md](docs/getting-started/local-environment.md): Docker services, ports, and `.env`
- [docs/getting-started/quick-start.md](docs/getting-started/quick-start.md): full local startup flow
- [docs/reference/configuration.md](docs/reference/configuration.md): profiles and environment variables
- [docs/reference/database-migrations.md](docs/reference/database-migrations.md): Flyway and seed data details
- [docs/reference/api-conventions.md](docs/reference/api-conventions.md): response and error handling rules
- [docs/reference/authentication-and-rbac.md](docs/reference/authentication-and-rbac.md): login, JWT, context, and RBAC behavior
- [docs/reference/api-docs.md](docs/reference/api-docs.md): Swagger and OpenAPI access
- [docs/reference/observability.md](docs/reference/observability.md): health checks and request tracing
- [docs/runbooks/local-smoke-test.md](docs/runbooks/local-smoke-test.md): manual verification flow
- [docs/runbooks/troubleshooting.md](docs/runbooks/troubleshooting.md): common local problems
- [docs/architecture/tenant-rbac-integrity-gap.md](docs/architecture/tenant-rbac-integrity-gap.md): recorded database isolation gap

## Current Status

- Initial repository setup is complete
- Core backend skeleton is running
- Documentation is split into README for entry-level context and `docs/` for detailed references
