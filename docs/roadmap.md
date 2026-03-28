# Roadmap

Last updated: 2026-03-28

> Maintenance note: keep this page focused on the active phase, the next recommended slices, and near-term sequencing. Link to [project-status.md](project-status.md) and the relevant pages under [reference/](reference/README.md) for exact current contracts instead of repeating full implementation inventories here.

## Current Phase

- Week 1 Platform Foundation is complete.
- Week 2 First Business Loop - Tenant User Management is complete.
- Week 3 Ticket Workflow - System of Action is complete.
- Week 4 Audit Trail And Approval Patterns is complete.
- Week 5 Async Import And Data Operations is complete.
- Week 6 AI Copilot For Ticket Operations is complete.
- Week 7 AI Copilot For Import And Data Quality is completion-ready and remains the current pre-tag phase.
- Week 5 still provides the current async-operations baseline: import submission/list/detail/errors, narrowed `USER_CSV` business-row execution, replay variants, queued-job recovery, stale-processing handling, and throughput guardrails.
- Exact current endpoint inventory and current limitations live in [project-status.md](project-status.md) and the matching pages under [reference/](reference/README.md).

## Current Focus

The current Week 7 implementation should stay narrow and workflow-oriented:

- keep the completed Week 6 ticket AI surface stable: ticket interaction history, ticket summary, ticket triage, and ticket internal reply draft remain `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft`
- keep the existing public ticket AI surface read-only, and keep the generation slices suggestion-only, tenant-scoped, RBAC-scoped, and failure-tolerant
- keep the new Week 7 import AI read surface narrow: `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` stay `USER_READ`, read-only, tenant-scoped, and suggestion-only
- keep Week 7 focused on import/data-quality AI guidance rather than widening into write-back, billing, ledger semantics, or generic chat tooling

## Recommended Next Steps

- run the Week 7 release-doc sync and final tag-readiness pass for the completed import AI read baseline
- cut the next Week 7 tag once docs and regression evidence remain clean, without widening the phase into replay execution or source-file mutation in the same step
- keep the completed Week 6 ticket AI surface and the completion-ready Week 7 import AI read surface stable while Week 8 planning shifts to agentic workflows with human oversight

## Near-Term Sequence

- Week 5: async import and data operations
- Week 6: AI Copilot for ticket operations
- Week 7: AI Copilot for import and data quality
- Week 8: agentic workflows with human oversight
- Week 9: AI governance, eval, cost, and usage
- Week 10: delivery hardening, feature flags, and portfolio packaging
- Stretch after Week 10: usage / ledger / invoice minimal loop

## Week 5 Outcome

- Week 5 ended with a credible async import and data-operations baseline: create/list/detail/errors, filtered queue reads, row-level execution, replay variants, and derived-job lineage.
- Runtime hardening includes sequential chunk execution, per-chunk counter visibility during `PROCESSING`, queued-job recovery after after-commit publish failure, stale-processing handling, and bounded import controls.
- That remains the intended handoff into Week 6 AI ticket work rather than an invitation to keep widening Week 5 breadth.

## Open-Source Track

- Current tagged milestone: `v0.3.0-beta` marks the completed Week 6 AI Copilot for Ticket Operations beta baseline on 2026-03-22.
- Prepared next tag: `v0.4.0-beta` for the completed Week 7 AI Copilot for Import and Data Quality beta baseline.
- Previous tagged milestone: `v0.2.0-alpha` marks the completed Week 5 async import and data operations preview on 2026-03-19.
- Earlier tagged milestones: `v0.1.3` marks the completed Week 4 audit and approval baseline on 2026-03-12, `v0.1.2` marks the completed Week 3 ticket workflow baseline on 2026-03-11, `v0.1.1` marks Week 2 tenant user management loop complete on 2026-03-11, and `v0.1.0` marks Week 1 Platform Foundation on 2026-03-09.
- The current open-source line can now honestly present the project as a workflow-first, AI-enhanced vertical SaaS backend through the Week 6 tag.
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery.

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Historical implementation detail belongs in [project-status.md](project-status.md), [project-plan.md](project-plan.md), and [CHANGELOG.md](../CHANGELOG.md), not here.
