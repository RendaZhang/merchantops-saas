# Architecture

This section stores architecture notes, technical decisions, and recorded structural gaps.

## ADRs

- [adr/0001-use-multi-module-maven-structure.md](adr/0001-use-multi-module-maven-structure.md): separate API, domain, infrastructure, and common concerns
- [adr/0002-use-flyway-for-schema-management.md](adr/0002-use-flyway-for-schema-management.md): manage schema changes through migrations
- [adr/0003-put-tenant-identity-in-jwt-and-request-context.md](adr/0003-put-tenant-identity-in-jwt-and-request-context.md): propagate tenant identity through authentication and request context
- [adr/0004-enforce-endpoint-permissions-with-require-permission.md](adr/0004-enforce-endpoint-permissions-with-require-permission.md): keep permission rules explicit at endpoint level
- [adr/0005-require-tenant-aware-repository-methods.md](adr/0005-require-tenant-aware-repository-methods.md): enforce tenant scoping closer to the repository layer
- [adr/0006-keep-week-1-focused-on-foundation-before-business-modules.md](adr/0006-keep-week-1-focused-on-foundation-before-business-modules.md): defer larger business modules until the foundation is stable

## Other Pages

- [tenant-rbac-integrity-gap.md](tenant-rbac-integrity-gap.md): database-level tenant isolation gap in `user_role`
