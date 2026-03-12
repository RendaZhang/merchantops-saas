# Roadmap

Last updated: 2026-03-12

## Current Phase

- Week 1 Platform Foundation is complete
- Week 2 First Business Loop - Tenant User Management is complete
- Week 3 Ticket Workflow - System of Action is complete
- Week 4 Audit Trail And Approval Patterns is complete
- Week 5 Async Import And Data Operations is the active phase
- Public HTTP coverage currently includes `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `GET /api/v1/roles`, `PUT /api/v1/users/{id}/roles`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, `POST /api/v1/tickets/{id}/comments`, `POST /api/v1/import-jobs`, `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, and `GET /api/v1/audit-events`, `POST /api/v1/users/{id}/disable-requests`, `GET /api/v1/approval-requests`, `GET /api/v1/approval-requests/{id}`, `POST /api/v1/approval-requests/{id}/approve`, `POST /api/v1/approval-requests/{id}/reject`
- The broader 10-week plan now prioritizes workflow modules and embedded AI use cases over adding more generic SaaS breadth too early
- The project now explicitly targets a progression from portfolio-quality build to open-source reference project, then possible commercial exploration later

## Week 2 Baseline

The first business loop is complete: Week 2 turned the user-management groundwork into Swagger-visible business APIs on top of the Week 1 foundation.

## Phase Sequence

- Week 2: user management (complete)
- Week 3: ticket workflow - system of action (complete)
- Week 4: audit trail and approval patterns (complete)
- Week 5: async import and data operations
- Week 6: AI Copilot for ticket operations
- Week 7: AI Copilot for import and data quality
- Week 8: agentic workflows with human oversight
- Week 9: AI governance, eval, cost, and usage
- Week 10: delivery hardening, feature flags, and portfolio packaging
- Stretch after Week 10: usage / ledger / invoice minimal loop

## Open-Source Track

- Current tagged milestone: `v0.1.3` marks the completed Week 4 audit and approval baseline on 2026-03-12
- Previous tagged milestone: `v0.1.2` marks the completed Week 3 ticket workflow baseline on 2026-03-11
- Earlier tagged milestones: `v0.1.1` marks Week 2 tenant user management loop complete on 2026-03-11 and `v0.1.0` marks Week 1 Platform Foundation on 2026-03-09
- Week 5 target: prepare for a next-stage preview such as `v0.2.0-alpha` after the first workflow and async-operation backbone is credible
- Week 6 or Week 7 target: make the first public open-source release that can honestly present the project as an AI-enhanced vertical SaaS, for example `v0.3.0-beta`
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery

## Recommended Next Step

Begin Week 5 async import and data operations from the completed Week 4 governance baseline:

- keep the landed Week 4 audit/approval baseline aligned across Swagger, reference docs, runbooks, and examples while import-related code starts landing
- introduce `import_job` and `import_job_item_error` as tenant-scoped async-operation primitives without breaking the existing Week 2-4 public baseline

## Planned Work By Phase

Week 2 completion:

- keep Swagger, `api-demo.http`, and reference docs aligned
- Week 2 milestone recorded through the `v0.1.1` tag on 2026-03-11 before the Week 3 delivery story broadens

Week 3 target:

- complete the first real workflow module through closeable ticket loop, queue/query behavior, and reopen semantics without jumping to a full audit subsystem
- Week 3 milestone recorded through the `v0.1.2` tag on 2026-03-11 before Week 4 governance work becomes the mainline story

Week 4 target:

- extend the current audit backbone into approval patterns that later AI and agent flows can reuse
- record the Week 4 milestone through `v0.1.3` on 2026-03-12 before Week 5 delivery broadens the story toward async operations

Week 5 target:

- build async import and data operations

Week 6 target:

- add AI Copilot for ticket operations

Week 7 target:

- add AI Copilot for import and data quality

Week 8 target:

- add low-risk agentic workflows with explicit approval boundaries

Week 9 target:

- add AI governance, evaluation, cost tracking, and usage records

Week 10 target:

- add feature flag support, deployment hardening, performance artifacts, and portfolio-ready documentation
- add integration tests where the current surface is stable

Stretch target after Week 10:

- add usage / ledger / invoice minimal loop

## Near-Term Priorities

- move from RBAC demo endpoints to clearer business-oriented endpoints
- begin Week 5 async import and data-operation work from the completed Week 4 governance baseline
- keep Week 2-4 docs aligned with the now-public workflow and governance baseline while Week 5 adds async primitives
- keep Week 3 and Week 4 hardening follow-up narrow and separate from the Week 5 mainline
- treat AI as an embedded workflow layer, not as a standalone chatbot detour
- design audit, approval, and evaluation hooks before agentic automation is added
- shape the architecture so later open-source packaging is straightforward, but do not front-run the active Week 5 delivery work with broader licensing or release chores
- improve test coverage for authentication, permission checks, and tenant isolation
- continue turning README-linked docs into a more complete developer handbook

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Implemented features and known current limitations are recorded in [project-status.md](project-status.md).


## Week 4 Progress Notes (2026-03-12)

- Slice A (generic audit backbone) is complete:
  - `audit_event` migration/entity/repository/service added
  - user and ticket public write flows now emit generic audit rows
  - workflow-level `ticket_operation_log` remains separate by design
  - minimal tenant-scoped query endpoint added: `GET /api/v1/audit-events`
- Slice B (minimal approval pattern) is complete with `USER_STATUS_DISABLE` create/detail/approve/reject.
- Slice C (approval queue read surface) is complete with tenant-scoped paging and filters (`status`, `actionType`, `requestedBy`) at `GET /api/v1/approval-requests`.
- Week 4 milestone is now recorded through `v0.1.3`.
- Next step is expanding to additional action types after queue operations are stable, while Week 5 async import becomes the active delivery stream.


## Week 5 Progress Notes (2026-03-12)

- Slice A (import submission backbone) is now landed:
  - `import_job` and `import_job_item_error` schema/entities/repositories added
  - public API now includes create/list/detail for import jobs
  - create flow persists files locally, writes `QUEUED` jobs, and publishes RabbitMQ messages
  - worker now consumes jobs and advances `QUEUED -> PROCESSING -> SUCCEEDED/FAILED` with parse-level errors
  - import create/process actions now emit reusable `audit_event` records
