# Architecture

This section stores architecture notes, technical decisions, and recorded structural gaps.

## ADRs

- [adr/0001-use-multi-module-maven-structure.md](adr/0001-use-multi-module-maven-structure.md): separate API, domain, infrastructure, and common concerns
- [adr/0002-use-flyway-for-schema-management.md](adr/0002-use-flyway-for-schema-management.md): manage schema changes through migrations
- [adr/0003-put-tenant-identity-in-jwt-and-request-context.md](adr/0003-put-tenant-identity-in-jwt-and-request-context.md): propagate tenant identity through authentication and request context
- [adr/0004-enforce-endpoint-permissions-with-require-permission.md](adr/0004-enforce-endpoint-permissions-with-require-permission.md): keep permission rules explicit at endpoint level
- [adr/0005-require-tenant-aware-repository-methods.md](adr/0005-require-tenant-aware-repository-methods.md): enforce tenant scoping closer to the repository layer
- [adr/0006-keep-week-1-focused-on-foundation-before-business-modules.md](adr/0006-keep-week-1-focused-on-foundation-before-business-modules.md): defer larger business modules until the foundation is stable
- [adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md](adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md): place AI inside business workflows with approval, audit, and tenant boundaries
- [adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): require a minimum AI audit and regression baseline before public AI APIs are considered stable
- [adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md](adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md): begin AI integration with deployment-owned provider keys and defer tenant BYOK until later
- [adr/0010-prefer-modular-monolith-and-workload-based-extraction-before-microservices.md](adr/0010-prefer-modular-monolith-and-workload-based-extraction-before-microservices.md): keep the primary architecture as a workflow-first modular monolith and defer service extraction until workload boundaries are proven

## Other Pages

- [../diagrams/target-architecture.md](../diagrams/target-architecture.md): visual target architecture for the modular-monolith-first path and later selective extraction
- [tenant-rbac-integrity-gap.md](tenant-rbac-integrity-gap.md): database-level tenant isolation gap in `user_role`
