# Project Status

Last updated: 2026-03-09

## Overview

MerchantOps SaaS is currently a runnable multi-tenant backend skeleton. The repository already demonstrates the main authentication, authorization, tenant-isolation, and local development flows, but it is not yet a complete merchant operations product.

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
- health checks, request tracing, and Swagger / OpenAPI support

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

## Known Gaps

- `user_role` tenant consistency is not yet enforced at the database layer
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs
- the project currently relies on manual verification flows more than automated test coverage

See [architecture/tenant-rbac-integrity-gap.md](architecture/tenant-rbac-integrity-gap.md) for the current tenant-integrity design note.
