# Roadmap

Last updated: 2026-03-12

> Maintenance note: keep this page focused on the active phase, the next recommended slices, and near-term sequencing. Link to [project-status.md](project-status.md) and the relevant pages under [reference/](reference/README.md) for exact current contracts instead of repeating full implementation inventories here.

## Current Phase

- Week 1 Platform Foundation is complete.
- Week 2 First Business Loop - Tenant User Management is complete.
- Week 3 Ticket Workflow - System of Action is complete.
- Week 4 Audit Trail And Approval Patterns is complete.
- Week 5 Async Import And Data Operations is the active phase.
- Week 5 Slice A is complete with import submission/list/detail plus queue backbone.
- Week 5 Slice B is in progress with narrow `USER_CSV` business-row execution and row-level failure isolation.
- Exact current endpoint inventory and current limitations live in [project-status.md](project-status.md) and the matching pages under [reference/](reference/README.md).

## Current Focus

Week 5 should stay narrow and workflow-oriented:

- keep the landed `USER_CSV` schema and example files aligned across Swagger, `api-demo.http`, reference docs, and runbooks
- stabilize row-level success/failure semantics before adding more import breadth
- continue using import jobs as an async operations backbone, not as a generic bulk-admin shortcut
- keep audit and approval groundwork reusable for later Week 6-9 AI flows

## Recommended Next Steps

- finish the current Week 5 Slice B hardening around `USER_CSV` row execution, error codes, and job-summary semantics
- add narrow follow-up slices such as retry/chunk controls or richer import reporting without expanding to unrelated modules
- keep Week 2-4 docs aligned only where Week 5 changes shared workflow or governance expectations
- avoid pulling Week 6 AI scope forward until the import execution model and failure reporting are stable

## Near-Term Sequence

- Week 5: async import and data operations
- Week 6: AI Copilot for ticket operations
- Week 7: AI Copilot for import and data quality
- Week 8: agentic workflows with human oversight
- Week 9: AI governance, eval, cost, and usage
- Week 10: delivery hardening, feature flags, and portfolio packaging
- Stretch after Week 10: usage / ledger / invoice minimal loop

## Active Phase Notes

Week 5 currently has two clear slices:

- Slice A is complete:
  - public API includes create/list/detail for import jobs
  - create flow persists files locally, writes `QUEUED` jobs, and publishes RabbitMQ messages after commit
- Slice B is in progress:
  - worker enforces fixed `USER_CSV` header `username,displayName,email,password,roleCodes`
  - valid rows create tenant users through the existing user command service path
  - row failures are isolated in `import_job_item_error` with parse and business codes such as `DUPLICATE_USERNAME`, `UNKNOWN_ROLE`, `INVALID_EMAIL`, and `INVALID_PASSWORD`
  - import job counters now reflect real create success/failure counts, with partial-success terminal `SUCCEEDED`

## Open-Source Track

- Current tagged milestone: `v0.1.3` marks the completed Week 4 audit and approval baseline on 2026-03-12.
- Previous tagged milestone: `v0.1.2` marks the completed Week 3 ticket workflow baseline on 2026-03-11.
- Earlier tagged milestones: `v0.1.1` marks Week 2 tenant user management loop complete on 2026-03-11 and `v0.1.0` marks Week 1 Platform Foundation on 2026-03-09.
- Week 5 target: prepare for a next-stage preview such as `v0.2.0-alpha` after the async-operation story is credible.
- Week 6 or Week 7 target: make the first public open-source release that can honestly present the project as an AI-enhanced vertical SaaS, for example `v0.3.0-beta`.
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery.

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Historical implementation detail belongs in [project-status.md](project-status.md), [project-plan.md](project-plan.md), and [CHANGELOG.md](../CHANGELOG.md), not here.
