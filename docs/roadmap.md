# Roadmap

Last updated: 2026-03-10

## Current Phase

- Week 1 Platform Foundation is complete
- Week 2 First Business Loop - Tenant User Management is in progress
- Public user-management HTTP coverage currently includes `GET /api/v1/users`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, and `PATCH /api/v1/users/{id}/status`
- The broader 10-week plan now prioritizes workflow modules and embedded AI use cases over adding more generic SaaS breadth too early
- The project now explicitly targets a progression from portfolio-quality build to open-source reference project, then possible commercial exploration later

## Week 2 Goal

Complete the first business loop by turning the current user-management groundwork into Swagger-visible business APIs on top of the Week 1 foundation.

## Phase Sequence

- Week 2: user management (active)
- Week 3: ticket workflow - system of action
- Week 4: audit trail and approval patterns
- Week 5: async import and data operations
- Week 6: AI Copilot for ticket operations
- Week 7: AI Copilot for import and data quality
- Week 8: agentic workflows with human oversight
- Week 9: AI governance, eval, cost, and usage
- Week 10: delivery hardening, feature flags, and portfolio packaging
- Stretch after Week 10: usage / ledger / invoice minimal loop

## Open-Source Track

- Current tagged baseline: `v0.1.0` already marks Week 1 Platform Foundation on 2026-03-09
- Week 5 target: prepare for a next-stage preview such as `v0.2.0-alpha` after the first workflow and async-operation backbone is credible
- Week 6 or Week 7 target: make the first public open-source release that can honestly present the project as an AI-enhanced vertical SaaS, for example `v0.3.0-beta`
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery

## Recommended Next Step

Continue with real user management APIs:

- add tenant-scoped user detail lookup
- implement role-assignment flow end to end
- add operator audit fields and verification coverage
- finish Week 2 before expanding into ticket workflow and AI layers

## Planned Work By Phase

Week 2 completion:

- add `GET /api/v1/users/{userId}` detail endpoint
- add role-assignment endpoint
- add audit logging fields and operator tracking for user-management writes
- keep Swagger, `api-demo.http`, and reference docs aligned

Week 3 target:

- build the ticket workflow module as the first real system-of-action surface

Week 4 target:

- add audit trail and approval patterns that later AI and agent flows can reuse

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
- finish the remaining detail and role-assignment pieces of the first user-management loop
- keep Week 2 documentation aligned with the public API as endpoints are exposed
- sequence Week 3 ticket work only after Week 2 user management is usable end to end
- treat AI as an embedded workflow layer, not as a standalone chatbot detour
- design audit, approval, and evaluation hooks before agentic automation is added
- shape the architecture so later open-source packaging is straightforward, but do not front-run Week 2 with licensing or release chores yet
- improve test coverage for authentication, permission checks, and tenant isolation
- continue turning README-linked docs into a more complete developer handbook

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Implemented features and known current limitations are recorded in [project-status.md](project-status.md).
