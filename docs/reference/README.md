# Reference

This section holds stable technical reference for the system itself: configuration, auth, APIs, observability, and other implementation-facing project details.

Contributor and agent workflow guidance now lives under [../contributing/README.md](../contributing/README.md).

## Pages

- [configuration.md](configuration.md): runtime profiles, environment variable overrides, import controls, and active AI provider settings
- [database-migrations.md](database-migrations.md): Flyway setup, migration history, and seed data notes
- [api-conventions.md](api-conventions.md): response wrapper and error handling rules
- [authentication-and-rbac.md](authentication-and-rbac.md): login flow, JWT usage, context propagation, RBAC expectations, and the current AI interaction-history, summary, triage, and reply-draft read permission boundary
- [user-management.md](user-management.md): current `/api/v1/users` behavior, Swagger verification, and the current Week 2 delivery boundary
- [ticket-workflow.md](ticket-workflow.md): current `/api/v1/tickets` workflow loop, workflow-log behavior, and the public AI interaction-history, summary, triage, and reply-draft read slices
- [import-jobs.md](import-jobs.md): current `/api/v1/import-jobs` async contract, `USER_CSV` row-execution behavior, import AI interaction history plus error summary/mapping suggestion/fix recommendation, and status/error model
- [audit-approval.md](audit-approval.md): generic audit-event backbone status, minimal audit query API, approval-scope boundary, and how AI interaction records stay separate
- [ai-integration.md](ai-integration.md): current AI workflow placement, permission boundaries, public ticket and import AI contracts, and audit/eval expectations
- [ai-provider-configuration.md](ai-provider-configuration.md): current instance-level provider configuration, active config keys, and the deferred tenant-BYOK boundary
- [api-docs.md](api-docs.md): OpenAPI and Swagger access details
- [observability.md](observability.md): health checks and request tracing
