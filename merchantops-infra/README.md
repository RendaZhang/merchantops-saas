# merchantops-infra

This module owns persistence and adapter implementations for domain ports.

Use this module for:
- JPA entities and Spring Data repositories
- Adapters that translate between domain ports and persistence models
- Persistence-specific query or save mechanics that should stay out of `merchantops-api`

Do not put these here:
- HTTP controllers, Swagger contracts, or API DTOs
- Core business rules that belong in `merchantops-domain`
- Cross-cutting API concerns such as request IDs, security filters, or `ApiResponse`

Current package rule:
- Keep adapter code in `infra.<capability>`
- Keep entities in `infra.persistence.entity`
- Keep Spring Data repositories in `infra.repository`
