# Project Status

Last updated: 2026-03-10

## Overview

MerchantOps SaaS has completed Week 1 Platform Foundation and is now in Week 2 First Business Loop - Tenant User Management. The repository already demonstrates the main authentication, authorization, tenant-isolation, and local development flows, but its public business API surface is still intentionally narrow while the first real business loop is being completed.

## Current Phase Summary

- Current phase: Week 2 First Business Loop - Tenant User Management
- Next phase: finish the remaining public user-management APIs and then move into Week 3 Ticket Workflow and Audit Trail
- Primary outcome: the backend foundation is being validated by the first real tenant-scoped business loop

## Implemented Scope

The current repository includes:

- multi-module Spring Boot backend structure
- local development environment with MySQL, Redis, and RabbitMQ configuration
- Flyway schema migrations and demo seed data
- login endpoint with `tenantCode + username + password`
- JWT issuance and Bearer-token authentication
- authenticated current-user endpoint
- authenticated tenant/user context endpoint
- tenant-scoped user listing endpoint with `USER_READ`
- tenant-aware user query scaffolding for detail lookup, status filtering, and page normalization
- initial write-side user command service skeleton with tenant-scoped username uniqueness checks
- permission checks through `@RequirePermission`
- RBAC demo endpoints for permission verification
- unified API response and exception handling model
- health checks, request tracing, and enriched Swagger / OpenAPI support

## Week 1 Completion Checklist

- [x] Multi-module project skeleton
- [x] Docker Compose for MySQL / Redis / RabbitMQ
- [x] Spring Boot connects to infra dependencies
- [x] Flyway migration setup
- [x] Core SaaS schema initialized
- [x] Demo tenant / users / roles / permissions seeded
- [x] Unified API response structure
- [x] Global exception handling
- [x] RequestId and request logging
- [x] Swagger / OpenAPI enabled with examples and grouped coverage
- [x] Login API implemented
- [x] JWT token issuance implemented
- [x] JWT authentication filter implemented
- [x] `/api/v1/user/me` implemented
- [x] Tenant context implemented
- [x] Current user context implemented
- [x] RBAC permission interceptor implemented
- [x] Tenant-scoped user listing API implemented

## Current Completion Status

Completed:

- local startup flow is documented and runnable
- demo tenant, users, roles, and permissions are seeded
- authentication flow works end to end
- authorization flow works for protected endpoints
- current-tenant user query works with permission protection
- Week 2 first-business-loop groundwork has started, but the public HTTP contract is still incomplete
- manual verification flow is documented

Not yet implemented:

- public user detail endpoint
- public user create, update, and password-update endpoints
- public pagination and status-filter query parameters for `/api/v1/users`
- end-to-end write persistence inside `UserCommandService`
- audit logging fields and operator tracking
- Week 3 ticket workflow and audit trail module
- Week 4 async import and background processing module
- Week 5 feature flag, delivery hardening, and stronger observability coverage
- usage / ledger / invoice minimal loop
- integration tests
- deployment-ready Docker or Kubernetes manifests
- performance documentation and benchmark artifacts

## Current Limitations

Current implementation is intentionally focused on the Week 1 foundation plus the in-progress Week 2 user-management loop, so the following are not yet implemented:

- `/api/v1/users` is still the only Swagger-visible user-management business endpoint
- `/api/v1/users` currently returns an unpaged array of current-tenant users ordered by `id ASC`
- user detail, page, and write flows exist only as internal DTO/service groundwork and are not yet published in controllers or Swagger
- `UserCommandService#createUser`, `updateUser`, and `updatePassword` currently stop at `UnsupportedOperationException`
- refresh token flow
- logout or token revocation
- fine-grained operator audit logs
- integration tests
- production-ready secret management
- tenant admin UI or frontend
- later-phase modules such as ticket workflow, async import, feature flag support, and billing-related capabilities

## Known Gaps

- `user_role` tenant consistency is not yet enforced at the database layer
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs
- the project currently relies on manual verification flows more than automated test coverage

See [architecture/tenant-rbac-integrity-gap.md](architecture/tenant-rbac-integrity-gap.md) for the current tenant-integrity design note.
See [runbooks/regression-checklist.md](runbooks/regression-checklist.md) for the current baseline regression checklist.
