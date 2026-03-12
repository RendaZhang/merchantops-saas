# Project Status

Last updated: 2026-03-12

## Overview

MerchantOps SaaS has completed Week 2 First Business Loop - Tenant User Management, the core Week 3 Ticket Workflow - System of Action, and the initial Week 4 Audit Trail And Approval Patterns baseline on top of the Week 1 Platform Foundation. The repository already demonstrates the main authentication, authorization, tenant-isolation, local development, first workflow-loop flows, and a reusable governance baseline through audit events plus approval requests, but it has not yet reached the later async-operation and AI-enhanced stages of the roadmap. The intended progression is portfolio first, then open-source reference implementation, and only later potential commercial exploration if the workflow and AI layers become credible.

## Current Phase Summary

- Current phase: Week 5 Async Import And Data Operations (starting from a completed Week 4 audit/approval baseline)
- Week 4 Slice A status: completed (generic `audit_event` backbone landed)
- Week 4 Slice B status: completed with minimal approval pattern (`USER_STATUS_DISABLE`) public
- Week 4 Slice C status: completed with approval queue read surface (`GET /api/v1/approval-requests`)
- Primary outcome: use the completed Week 2 tenant user-management loop, the completed Week 3 ticket workflow, and the completed Week 4 governance baseline as the stable foundation for async operations and later AI rollout
- Current tagged milestone: `v0.1.3` on 2026-03-12, recorded as `Week 4 complete: audit and approval baseline`
- Previous tagged milestone: `v0.1.2` on 2026-03-11, recorded as `Week 3 complete: ticket workflow baseline`
- Earlier milestones: `v0.1.1` on 2026-03-11 (`Week 2 complete: tenant user management loop`) and `v0.1.0` on 2026-03-09 (`Week 1 complete: foundation phase`)
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
- tenant-scoped ticket listing endpoint with `TICKET_READ`
- tenant-scoped ticket detail endpoint with `TICKET_READ`
- tenant-scoped ticket create, assignee, status, and comment endpoints with `TICKET_WRITE`
- tenant-scoped audit-event query endpoint with `USER_READ` for entity-scoped current-tenant governance reads
- tenant-scoped approval-request create/queue/detail/approve/reject endpoints for minimal user-disable approval flow
- workflow-level ticket operation logging for create, assign, status change, and comment events
- generic `audit_event` backbone for existing user and ticket public write operations
- generic `approval_request` backbone for `USER_STATUS_DISABLE`
- tenant-aware user query scaffolding for detail lookup, status filtering, and page normalization
- tenant-aware ticket query scaffolding for page reads, detail hydration, and workflow-log hydration
- write-side user command service with tenant-scoped username uniqueness checks, transactional create/update/status/role-assignment flows, and lightweight operator attribution
- write-side ticket command service with explicit `tenantId`, `operatorId`, and `requestId`
- request-time JWT claim revalidation against current user status, roles, and permissions
- focused automated coverage for auth security integration, user-management controller/service behavior, approval behavior, repository query behavior, and the Week 3 ticket loop
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
- current-tenant ticket list works with `TICKET_READ` and page-style output
- current-tenant ticket detail works with `TICKET_READ` and returns current comments plus workflow logs
- current-tenant ticket create works with `TICKET_WRITE` and default `OPEN` status
- current-tenant ticket assignment works with `TICKET_WRITE` and rejects cross-tenant or inactive assignees
- current-tenant ticket status management works with `TICKET_WRITE` and enforces the current `OPEN` / `IN_PROGRESS` / `CLOSED` transition rules, including reopen from `CLOSED -> OPEN`
- current-tenant ticket comments work with `TICKET_WRITE` and append workflow log entries
- current-tenant audit query works with `USER_READ`, `entityType`, and `entityId`, returning only current-tenant audit rows
- existing user and ticket public writes now emit generic `audit_event` rows with `operatorId` and `requestId` while ticket workflow logs remain in place
- focused automated tests now cover auth security integration, current user-management paths, the current audit and approval paths, and the Week 3 ticket workflow paths including reopen behavior
- Week 2 first-business-loop public HTTP contract now covers list, detail, create, profile update, status management, tenant role lookup, and role reassignment
- Week 3 public HTTP contract now covers ticket list, detail, create, assignee change, status change, comment, close-through-status, queue filters (`assigneeId`, `keyword`, `unassignedOnly`), and reopen semantics (`CLOSED -> OPEN`)
- Week 4 public HTTP contract now includes `GET /api/v1/audit-events` plus minimal approval create, queue, review, and execution endpoints for user-disable requests
- Week 3 core acceptance criteria are now met, so remaining ticket enrichments are treated as post-Week-3 follow-up rather than blockers
- manual and automated verification flows are documented

Not yet implemented:

- broader multi-action approval patterns and richer audit-read surface beyond current minimal entity/query-by-id endpoints
- post-Week-3 ticket enrichments such as priority/SLA, attachments, or notifications
- Week 5 async import and data operations
- Week 6 ticket AI Copilot
- Week 7 import AI Copilot
- Week 8 agentic workflows with human oversight
- Week 9 AI governance, eval, cost, and usage tracking
- Week 10 delivery hardening, feature flag rollout control, and portfolio packaging
- formal open-source release packaging such as license choice, contribution guide, security policy, and sanitized public demo assets
- usage / ledger / invoice remains a stretch goal after the core workflow + AI path is stable
- broader automated coverage beyond the current login + `/api/v1/roles` + `/api/v1/users` + `/api/v1/tickets` surface
- deployment-ready Docker or Kubernetes manifests
- performance documentation and benchmark artifacts

## Current Limitations

Current implementation is intentionally focused on the completed Week 1 foundation, the completed Week 2 user-management loop, the completed Week 3 ticket slices, and the completed Week 4 audit/approval baseline, so the following are not yet implemented:

- Swagger-visible business endpoints are currently `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `GET /api/v1/roles`, `PUT /api/v1/users/{id}/roles`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, `POST /api/v1/tickets/{id}/comments`, and `GET /api/v1/audit-events`, `POST /api/v1/users/{id}/disable-requests`, `GET /api/v1/approval-requests`, `GET /api/v1/approval-requests/{id}`, `POST /api/v1/approval-requests/{id}/approve`, `POST /api/v1/approval-requests/{id}/reject`
- `GET /api/v1/users` is a paged current-tenant query endpoint ordered by `id ASC`
- `GET /api/v1/users/{id}` is the current tenant-scoped detail query endpoint and includes current `roleCodes`
- `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles` are the public user-management write endpoints today
- `GET /api/v1/tickets` is the current paged ticket query endpoint and supports `status`, `assigneeId`, `keyword` (title/description), and `unassignedOnly`
- `GET /api/v1/tickets/{id}` is the current tenant-scoped ticket detail endpoint and includes comments and workflow logs
- `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments` are the current ticket write endpoints today
- `GET /api/v1/audit-events` is the current minimal tenant-scoped audit query endpoint and requires both `entityType` and `entityId`
- `POST /api/v1/users/{id}/disable-requests` only creates a `PENDING` approval request; it does not change user status by itself
- `GET /api/v1/approval-requests` now provides a tenant-scoped queue with `page`, `size`, `status`, `actionType`, and `requestedBy` filters
- `PUT /api/v1/users/{id}` updates only `displayName` and `email`
- `PATCH /api/v1/users/{id}/status` accepts only `ACTIVE` and `DISABLED`
- `GET /api/v1/roles` returns only current-tenant roles that can be assigned by the current operator
- `PUT /api/v1/users/{id}/roles` replaces the user's current role bindings within the current tenant
- current ticket transition rules now include reopen: `OPEN -> IN_PROGRESS`, `OPEN -> CLOSED`, `IN_PROGRESS -> CLOSED`, and `CLOSED -> OPEN`
- current generic audit read surface remains minimal: `GET /api/v1/audit-events` still supports only entity-scoped reads by `entityType + entityId`; approval queue pagination is now available at `GET /api/v1/approval-requests`
- when a user's current roles or effective permissions no longer match the JWT claims, the next protected request is rejected and the user must log in again
- lightweight operator attribution remains stored internally on `users.created_by` / `users.updated_by`, while reusable cross-entity audit records now live in `audit_event`; approval workflows and richer governance policy are still pending
- `UserCommandService#updatePassword` still returns a unified business error until that write flow is implemented
- no AI-assisted workflow endpoints, runtime AI audit trail, or code-backed evaluation datasets exist yet
- refresh token flow
- logout or token revocation
- fine-grained operator audit logs
- broader multi-module automated coverage outside the current auth + user-management + ticket workflow path
- production-ready secret management
- tenant admin UI or frontend
- post-Week-3 ticket enrichments such as priority or SLA handling, attachments, and notifications, plus later-phase modules such as async import, AI copilots, agent workflows, feature flag support, and billing-related capabilities

## Known Gaps

- `user_role` tenant consistency is not yet enforced at the database layer
- ticket assignee / creator / operator tenant consistency is enforced in service logic today, not yet at the database-constraint level
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs
- the project now has focused automated coverage for login, `GET /api/v1/roles`, `/api/v1/users` (`GET`, `GET /{id}`, `POST`, `PUT`, and `PATCH`), `PUT /api/v1/users/{id}/roles`, `/api/v1/tickets` (`GET`, `GET /{id}`, `POST`, and `PATCH`), `GET /api/v1/audit-events`, `POST /api/v1/users/{id}/disable-requests`, `GET /api/v1/approval-requests`, `GET /api/v1/approval-requests/{id}`, `POST /api/v1/approval-requests/{id}/approve`, `POST /api/v1/approval-requests/{id}/reject`, operator attribution, stale-claim rejection, query/service behavior, generic audit emission, and the ticket workflow-log path, but still relies on manual and smoke verification for Swagger rendering, real infra health, and endpoints outside the covered auth + user-management + ticket + audit + approval path

See [architecture/non-blocking-backlog.md](architecture/non-blocking-backlog.md) for the current non-blocking follow-up items, including the Week 1 `user_role` tenant-integrity gap and later ticket/productization carry-overs.
See [runbooks/regression-checklist.md](runbooks/regression-checklist.md) for the current baseline regression checklist.
