# Roadmap

Last updated: 2026-03-11

## Current Phase

- Week 1 Platform Foundation is complete
- Week 2 First Business Loop - Tenant User Management is complete
- Week 3 Ticket Workflow - System of Action is the active phase, and Slice B queue/query enrichment is now public
- Public HTTP coverage currently includes `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `GET /api/v1/roles`, `PUT /api/v1/users/{id}/roles`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments`
- The broader 10-week plan now prioritizes workflow modules and embedded AI use cases over adding more generic SaaS breadth too early
- The project now explicitly targets a progression from portfolio-quality build to open-source reference project, then possible commercial exploration later

## Week 2 Outcome

Complete the first business loop by turning the current user-management groundwork into Swagger-visible business APIs on top of the Week 1 foundation.

## Phase Sequence

- Week 2: user management (complete)
- Week 3: ticket workflow - system of action (active phase)
- Week 4: audit trail and approval patterns
- Week 5: async import and data operations
- Week 6: AI Copilot for ticket operations
- Week 7: AI Copilot for import and data quality
- Week 8: agentic workflows with human oversight
- Week 9: AI governance, eval, cost, and usage
- Week 10: delivery hardening, feature flags, and portfolio packaging
- Stretch after Week 10: usage / ledger / invoice minimal loop

## Open-Source Track

- Current tagged milestone: `v0.1.1` marks Week 2 tenant user management loop complete on 2026-03-11
- Previous tagged baseline: `v0.1.0` marks Week 1 Platform Foundation on 2026-03-09
- Week 5 target: prepare for a next-stage preview such as `v0.2.0-alpha` after the first workflow and async-operation backbone is credible
- Week 6 or Week 7 target: make the first public open-source release that can honestly present the project as an AI-enhanced vertical SaaS, for example `v0.3.0-beta`
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery

## Recommended Next Step

Continue Week 3 ticket workflow after Slice B:

- enrich the ticket queue beyond the first closeable loop without expanding into Week 4 generic audit scope too early
- keep remaining schema hardening tasks scoped so they do not destabilize the now-public Week 3 Slice B contract

## Planned Work By Phase

Week 2 completion:

- keep Swagger, `api-demo.http`, and reference docs aligned
- Week 2 milestone recorded through the `v0.1.1` tag on 2026-03-11 before the Week 3 delivery story broadens

Week 3 target:

- land the first closeable ticket loop, then extend queue/query behavior (status + assignee + keyword + unassigned) without jumping to a full audit subsystem

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
- continue the Week 3 ticket workflow slices now that the first closeable loop is public
- keep Week 2 user-management docs aligned with the now-tagged public API baseline while Week 3 grows
- keep Week 3 ticket work layered on top of the already-usable Week 2 user-management baseline
- treat AI as an embedded workflow layer, not as a standalone chatbot detour
- design audit, approval, and evaluation hooks before agentic automation is added
- shape the architecture so later open-source packaging is straightforward, but do not front-run the active Week 3 delivery work with broader licensing or release chores
- improve test coverage for authentication, permission checks, and tenant isolation
- continue turning README-linked docs into a more complete developer handbook

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Implemented features and known current limitations are recorded in [project-status.md](project-status.md).
