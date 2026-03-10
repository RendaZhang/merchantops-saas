# Project Status

Last updated: 2026-03-10

## Overview

MerchantOps SaaS has completed Week 1 Platform Foundation and is now in Week 2 First Business Loop - Tenant User Management within the new 10-week market-aligned plan. The repository already demonstrates the main authentication, authorization, tenant-isolation, and local development flows, but it has not yet reached the later workflow and AI-enhanced stages of the roadmap. The intended progression is portfolio first, then open-source reference implementation, and only later potential commercial exploration if the workflow and AI layers become credible.

## Current Phase Summary

- Current phase: Week 2 First Business Loop - Tenant User Management
- Next phase: begin Week 3 Ticket Workflow - System of Action while keeping remaining Week 2 schema hardening tasks scoped
- Primary outcome: the backend foundation is being validated by the first real tenant-scoped business loop before broader workflow and AI delivery begins
- Current tagged milestone: `v0.1.0` on 2026-03-09, recorded as `Week 1 complete: foundation phase`
- Open-source timing expectation: an early preview becomes more realistic after Week 5, while a stronger public release should wait until at least the first AI Copilot milestone in Week 6 or Week 7

## Project Direction

- near-term goal: turn the project into a credible workflow-first portfolio system instead of a generic CRUD backend
- mid-term goal: turn that system into an open-source reference implementation that other developers can run and study
- longer-term goal: use the open-source project as the base for product discovery, collaboration, or commercial validation once workflow, AI, and governance layers are proven

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
- tenant-scoped user detail endpoint with `USER_READ`
- tenant-scoped user create endpoint with `USER_WRITE`, BCrypt password hashing, and tenant-local role binding
- tenant-scoped user profile update endpoint with `USER_WRITE`
- tenant-scoped user status-management endpoint with `USER_WRITE`
- tenant-scoped role listing endpoint with `USER_WRITE`
- tenant-scoped user role-reassignment endpoint with `USER_WRITE`
- tenant-aware user query scaffolding for detail lookup, status filtering, and page normalization
- write-side user command service with tenant-scoped username uniqueness checks, transactional create/update/status/role-assignment flows, and lightweight operator attribution
- request-time JWT claim revalidation against current user status, roles, and permissions
- focused automated coverage for auth security integration, user-management controller/service behavior, and repository query behavior
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
- current-tenant user query supports page, size, username, status, and roleCode filters
- current-tenant user detail works with `USER_READ` and returns current tenant-local role codes
- current-tenant user create works with `USER_WRITE`, username uniqueness, BCrypt storage, and tenant-local role validation
- current-tenant user profile update works with `USER_WRITE` and explicit mutable-field boundaries
- current-tenant user status management works with `USER_WRITE` and `ACTIVE` / `DISABLED` validation
- current-tenant role listing works with `USER_WRITE` and returns only tenant-local assignable roles
- current-tenant role reassignment works with `USER_WRITE`, clears old bindings, and rewrites current tenant role bindings transactionally
- lightweight operator attribution now records `created_by` / `updated_by` on `users` for create, profile update, status update, and role reassignment writes
- role or permission changes now invalidate previously issued JWTs on the next protected request, forcing re-login before new privileges apply
- focused automated tests now cover auth security integration, current user-management controller/service paths, and repository query behavior
- Week 2 first-business-loop public HTTP contract now covers list, create, profile update, status management, tenant role lookup, and role reassignment
- manual and automated verification flows are documented

Not yet implemented:

- Week 3 ticket workflow module
- Week 4 audit trail and approval patterns
- Week 5 async import and data operations
- Week 6 ticket AI Copilot
- Week 7 import AI Copilot
- Week 8 agentic workflows with human oversight
- Week 9 AI governance, eval, cost, and usage tracking
- Week 10 delivery hardening, feature flag rollout control, and portfolio packaging
- formal open-source release packaging such as license choice, contribution guide, security policy, and sanitized public demo assets
- usage / ledger / invoice remains a stretch goal after the core workflow + AI path is stable
- broader automated coverage beyond the current login + `/api/v1/roles` + `/api/v1/users` Week 2 surface
- deployment-ready Docker or Kubernetes manifests
- performance documentation and benchmark artifacts

## Current Limitations

Current implementation is intentionally focused on the Week 1 foundation plus the in-progress Week 2 user-management loop, so the following are not yet implemented:

- Swagger-visible Week 2 business endpoints are currently `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `GET /api/v1/roles`, and `PUT /api/v1/users/{id}/roles`
- `GET /api/v1/users` is a paged current-tenant query endpoint ordered by `id ASC`
- `GET /api/v1/users/{id}` is the current tenant-scoped detail query endpoint and includes current `roleCodes`
- `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles` are the public user-management write endpoints today
- `PUT /api/v1/users/{id}` updates only `displayName` and `email`
- `PATCH /api/v1/users/{id}/status` accepts only `ACTIVE` and `DISABLED`
- `GET /api/v1/roles` returns only current-tenant roles that can be assigned by the current operator
- `PUT /api/v1/users/{id}/roles` replaces the user's current role bindings within the current tenant
- when a user's current roles or effective permissions no longer match the JWT claims, the next protected request is rejected and the user must log in again
- lightweight operator attribution is stored internally on `users.created_by` / `users.updated_by`, but no public audit endpoint or generic audit event model exists yet
- `UserCommandService#updatePassword` still returns a unified business error until that write flow is implemented
- no workflow-level modules such as ticketing or import jobs are public yet
- no AI-assisted workflow endpoints, runtime AI audit trail, or code-backed evaluation datasets exist yet
- refresh token flow
- logout or token revocation
- fine-grained operator audit logs
- broader multi-module automated coverage outside the current auth + user-management path
- production-ready secret management
- tenant admin UI or frontend
- later-phase modules such as ticket workflow, async import, AI copilots, agent workflows, feature flag support, and billing-related capabilities

## Known Gaps

- `user_role` tenant consistency is not yet enforced at the database layer
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs
- the project now has focused automated coverage for login, `GET /api/v1/roles`, `/api/v1/users` (`GET`, `GET /{id}`, `POST`, `PUT`, and `PATCH`), `PUT /api/v1/users/{id}/roles`, operator attribution, stale-claim rejection, query/service behavior, and repository-backed search behavior, but still relies on manual and smoke verification for Swagger rendering, real infra health, and endpoints outside the covered auth + user-management flow

See [architecture/tenant-rbac-integrity-gap.md](architecture/tenant-rbac-integrity-gap.md) for the current tenant-integrity design note.
See [runbooks/regression-checklist.md](runbooks/regression-checklist.md) for the current baseline regression checklist.
