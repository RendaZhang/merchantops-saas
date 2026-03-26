# merchantops-domain

This module owns business use cases, ports, records, and shared business errors for the modular monolith.

Use this module for:
- Capability use-case interfaces and services
- Domain ports implemented by `merchantops-infra`
- Business records, policy logic, and shared business exceptions

Do not put these here:
- Spring MVC or Swagger types
- JPA annotations, entities, or Spring Data repositories
- HTTP DTOs or request-context adapters

Current package rule:
- Keep business logic in `domain.<capability>`
- Keep shared business errors in `domain.shared.error`
- Prefer framework-light Java types so the module stays reusable across adapters
