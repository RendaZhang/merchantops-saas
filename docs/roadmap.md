# Roadmap

Last updated: 2026-04-04

> Maintenance note: keep this page focused on the active phase, the next recommended slices, and near-term sequencing. Link to [project-status.md](project-status.md) and the relevant pages under [reference/](reference/README.md) for exact current contracts instead of repeating full implementation inventories here.

## Current Phase

- Week 1 Platform Foundation is complete.
- Week 2 First Business Loop - Tenant User Management is complete.
- Week 3 Ticket Workflow - System of Action is complete.
- Week 4 Audit Trail And Approval Patterns is complete.
- Week 5 Async Import And Data Operations is complete.
- Week 6 AI Copilot For Ticket Operations is complete.
- Week 7 AI Copilot For Import And Data Quality is complete.
- Week 8 Agentic Workflows With Human Oversight is the current active phase.
- Week 5 still provides the current async-operations baseline: import submission/list/detail/errors, narrowed `USER_CSV` business-row execution, replay variants, queued-job recovery, stale-processing handling, and throughput guardrails.
- Exact current endpoint inventory and current limitations live in [project-status.md](project-status.md) and the matching pages under [reference/](reference/README.md).

## Current Focus

The current Week 8 implementation should stay narrow and human-governed:

- keep the completed Week 6 ticket AI surface stable: ticket interaction history, ticket summary, ticket triage, and ticket internal reply draft remain `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft`
- keep the completed Week 7 import AI surface stable: `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` stay `USER_READ`, read-only, tenant-scoped, and suggestion-only
- the shipped Week 8 slices are now `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals` and `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`, which add low-risk proposal, approval, and execution flows with human oversight by connecting import fix guidance to the existing selective replay path and connecting ticket reply-draft output to the existing comment write path
- keep the shared approval queue action-aware: mixed approval surfaces now route read/review permissions by approval action capability instead of a controller-wide `USER_*` gate
- keep the Week 8 governance baseline hardened: pending proposals are now deduplicated on executable payload semantics, concurrent duplicate creates collapse to one pending row, and resolved requests release their pending key for future proposals
- continue using Week 8 to add low-risk proposal, approval, and execution flows with human oversight instead of widening the existing ticket or import AI slices into direct autonomous write-back
- keep Week 8 focused on approval-bounded workflow execution rather than tenant billing, ledger semantics, or generic chat tooling

## Recommended Next Steps

- treat the import selective replay proposal flow plus the ticket reply-draft comment proposal flow, together with the shared pending-proposal uniqueness hardening, as the Week 8 baseline pattern for proposal -> approval -> execution
- keep the completed Week 6 ticket AI surface and the completed Week 7 import AI read surface stable while any further Week 8 slice reuses the same human-reviewed execution pattern through separate workflow endpoints rather than widening the AI endpoints themselves
- the next Week 8 decision is now close-out versus one bonus third narrow workflow, not another round of approval-engine hardening
- if a bonus third workflow is added later, keep approval payloads narrow and non-sensitive; avoid broader write-back, billing, ledger semantics, and generic chat tooling until the proposal/approval/execution shape has been proven across these first two actions plus the hardening baseline

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

- Current tagged milestone: `v0.4.0-beta` marks the completed Week 7 AI Copilot for Import and Data Quality beta baseline on 2026-03-28.
- Previous tagged milestone: `v0.3.0-beta` marks the completed Week 6 AI Copilot for Ticket Operations beta baseline on 2026-03-22.
- Earlier tagged milestones: `v0.2.0-alpha` marks the completed Week 5 async import and data operations preview on 2026-03-19, `v0.1.3` marks the completed Week 4 audit and approval baseline on 2026-03-12, `v0.1.2` marks the completed Week 3 ticket workflow baseline on 2026-03-11, `v0.1.1` marks Week 2 tenant user management loop complete on 2026-03-11, and `v0.1.0` marks Week 1 Platform Foundation on 2026-03-09.
- The current open-source line can now honestly present the project as a workflow-first, AI-enhanced vertical SaaS backend through the Week 7 tag, with untagged Week 8 workflow slices now extending that baseline in the working tree.
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery.

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Historical implementation detail belongs in [project-status.md](project-status.md), [project-plan.md](project-plan.md), and [CHANGELOG.md](../CHANGELOG.md), not here.
