# merchantops-api

This module owns the Spring Boot application, HTTP contracts, request-context handling, security integration, and application-layer orchestration.

Use this module for:
- Controllers, Swagger-visible API interfaces, and HTTP DTOs
- Capability-scoped application services, mappers, coordinators, and worker adapters
- Cross-cutting API support such as config, filters, security, request IDs, and `ApiResponse`

Do not put these here:
- JPA entities or Spring Data repositories
- Shared business errors or business records that belong to `merchantops-domain`
- Infrastructure adapter logic that exists only to satisfy a domain port

Current package rule:
- Put business code under capability packages such as `api.user`, `api.ticket`, `api.importjob`, and `api.approval`
- Keep root `api.controller` and `api.contract` limited to platform endpoints only
- Keep HTTP DTOs under `api.dto.<capability>`

Validation baseline:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
.\mvnw.cmd verify
```
