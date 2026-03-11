# Changelog

This file tracks release-level changes that matter to users, reviewers, and future open-source collaborators.

Low-level implementation steps stay in Git commit history. This changelog is intentionally version-oriented instead of day-by-day development-oriented.

## [Unreleased]

### Added

- Started the Week 4 audit-and-approval track with a generic `audit_event` backbone for current public user and ticket write flows.
- Added a minimal tenant-scoped audit query endpoint: `GET /api/v1/audit-events?entityType=...&entityId=...`.

### Changed

- Kept workflow-level `ticket_operation_log` in place while adding reusable cross-entity audit records for governance follow-up work.
- Strengthened the audit schema with DB-level same-tenant linkage for audit operator attribution.

### Docs

- Added audit/approval reference docs plus Swagger, smoke, regression, and API-example updates for the new audit query surface.

## [v0.1.2] - 2026-03-11

Tagged as `Week 3 complete: ticket workflow baseline`.

### Added

- Completed the Week 3 ticket workflow baseline with tenant-scoped ticket list, detail, create, assignee change, status change, comment, queue filters, and reopen support.
- Added workflow-level ticket operation logging plus focused automated coverage for queue/query behavior, reopen, and the closeable ticket lifecycle.

### Changed

- Updated status, roadmap, and plan docs to treat Week 3 as complete and Week 4 audit/approval work as the active phase.

### Docs

- Added a shared non-blocking backlog page for cross-phase follow-up items and folded the former standalone Week 1 tenant-integrity note into that backlog.
- Expanded ticket workflow reference, smoke flow, regression checklist entries, and API examples to match the completed Week 3 public contract.

## [v0.1.1] - 2026-03-11

Tagged as `Week 2 complete: tenant user management loop`.

### Changed

- Expanded the public API from the Week 1 foundation into a complete Week 2 tenant-scoped user-management loop with list, detail, create, profile update, status management, role lookup, and role reassignment flows.
- Enforced re-login after role or permission changes by rejecting stale JWT claims on the next protected request.
- Reframed the project plan as a 10-week workflow-first, AI-enhanced vertical SaaS roadmap.
- Clarified the project progression as portfolio first, open-source second, and potential commercial exploration later.
- Aligned version planning with the existing `v0.1.0` tag instead of reusing that version number for later milestones.
- Updated status and roadmap docs to mark Week 2 complete and Week 3 as the next active phase.

### Added

- Added lightweight operator attribution through `users.created_by` / `users.updated_by` for create, profile update, status update, and role reassignment writes.
- Added focused automated coverage for user detail, user writes, role reassignment, stale-claim rejection, and operator attribution.
- Added AI reference docs for workflow integration, provider configuration, prompt versioning, eval datasets, and candidate workflow rollout.
- Added AI governance ADRs for workflow placement, audit and evaluation baseline, and instance-level provider-key ownership.
- Added an explicit release-versioning reference page to define tag ownership and future version progression.

### Docs

- Expanded README and docs navigation so project plan, release versioning, AI docs, and AI runbooks are easier to discover.
- Recorded the Week 2 milestone and Week 3 handoff in the release, status, and roadmap docs.

## [v0.1.0] - 2026-03-09

Tagged as `Week 1 complete: foundation phase`.

### Added

- Multi-module Spring Boot backend foundation for a tenant-aware SaaS system.
- Local development support for MySQL, Redis, RabbitMQ, Docker Compose, and Flyway migrations.
- JWT login, Bearer-token authentication, tenant and user context propagation, and endpoint-level RBAC checks.
- Public foundation endpoints including `POST /api/v1/auth/login`, `GET /api/v1/user/me`, `GET /api/v1/context`, and `GET /api/v1/users`.
- Swagger / OpenAPI support, request tracing, unified API response handling, and health checks.

### Changed

- Standardized the current-user endpoint to `GET /api/v1/user/me`.
- Improved Swagger contract coverage, reusable examples, and demo-account consistency.
- Strengthened demo seed consistency through follow-up migration and documentation cleanup.

### Docs

- Split high-level status, roadmap, reference docs, runbooks, and ADRs into a clearer documentation structure.
- Added API demo requests, regression guidance, and reference pages for authentication, Swagger, configuration, and user management.
