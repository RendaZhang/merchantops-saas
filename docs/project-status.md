# Project Status

Last updated: 2026-03-09

## Overview

MerchantOps SaaS has completed its Week 1 foundation phase and is entering Week 2. The repository already demonstrates the main authentication, authorization, tenant-isolation, and local development flows, but it is not yet a complete merchant operations product.

## Foundation Phase Summary

- Current phase: Week 1 completed
- Next phase: Week 2 business API expansion
- Primary outcome: the backend foundation is in place and ready for real business modules

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
- manual verification flow is documented

Not yet implemented:

- real user-management create and update APIs
- pagination and query filters for user listing
- audit logging fields and operator tracking
- ticket, import, and billing demo modules
- integration tests
- deployment-ready Docker or Kubernetes manifests
- deeper observability and performance documentation

## Current Limitations

Current implementation is intentionally focused on the Week 1 foundation, so the following are not yet implemented:

- refresh token flow
- logout or token revocation
- fine-grained operator audit logs
- pagination for user listing
- create, update, and delete user APIs
- integration tests
- production-ready secret management
- tenant admin UI or frontend
- async business modules such as import, billing, and ticket workflow

## Known Gaps

- `user_role` tenant consistency is not yet enforced at the database layer
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs
- the project currently relies on manual verification flows more than automated test coverage

See [architecture/tenant-rbac-integrity-gap.md](architecture/tenant-rbac-integrity-gap.md) for the current tenant-integrity design note.
See [runbooks/regression-checklist.md](runbooks/regression-checklist.md) for the current Week 1 regression checklist.
