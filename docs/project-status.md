# Project Status

Last updated: 2026-03-10

## Overview

MerchantOps SaaS has completed Week 1 Platform Foundation and is now in Week 2 First Business Loop - Tenant User Management within the new 10-week market-aligned plan. The repository already demonstrates the main authentication, authorization, tenant-isolation, and local development flows, but it has not yet reached the later workflow and AI-enhanced stages of the roadmap. The intended progression is portfolio first, then open-source reference implementation, and only later potential commercial exploration if the workflow and AI layers become credible.

## Current Phase Summary

- Current phase: Week 2 First Business Loop - Tenant User Management
- Next phase: finish the remaining public user-management APIs and then move into Week 3 Ticket Workflow - System of Action
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
- current-tenant user query supports page, size, username, status, and roleCode filters
- Week 2 first-business-loop groundwork has started, but the public HTTP contract is still incomplete
- manual verification flow is documented

Not yet implemented:

- public user detail endpoint
- public user create, update, status-toggle, and role-assignment endpoints
- end-to-end write persistence inside `UserCommandService`
- audit logging fields and operator tracking
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
- integration tests
- deployment-ready Docker or Kubernetes manifests
- performance documentation and benchmark artifacts

## Current Limitations

Current implementation is intentionally focused on the Week 1 foundation plus the in-progress Week 2 user-management loop, so the following are not yet implemented:

- `/api/v1/users` is still the only Swagger-visible user-management business endpoint
- `/api/v1/users` is now a paged current-tenant query endpoint ordered by `id ASC`
- user detail and write flows exist only as internal DTO/service groundwork and are not yet published in controllers or Swagger
- `UserCommandService#createUser`, `updateUser`, and `updatePassword` currently return a unified business error until the write flows are implemented
- no workflow-level modules such as ticketing or import jobs are public yet
- no AI-assisted workflow endpoints, runtime AI audit trail, or code-backed evaluation datasets exist yet
- refresh token flow
- logout or token revocation
- fine-grained operator audit logs
- integration tests
- production-ready secret management
- tenant admin UI or frontend
- later-phase modules such as ticket workflow, async import, AI copilots, agent workflows, feature flag support, and billing-related capabilities

## Known Gaps

- `user_role` tenant consistency is not yet enforced at the database layer
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs
- the project currently relies on manual verification flows more than automated test coverage

See [architecture/tenant-rbac-integrity-gap.md](architecture/tenant-rbac-integrity-gap.md) for the current tenant-integrity design note.
See [runbooks/regression-checklist.md](runbooks/regression-checklist.md) for the current baseline regression checklist.
