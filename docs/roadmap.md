# Roadmap

Last updated: 2026-03-21

> Maintenance note: keep this page focused on the active phase, the next recommended slices, and near-term sequencing. Link to [project-status.md](project-status.md) and the relevant pages under [reference/](reference/README.md) for exact current contracts instead of repeating full implementation inventories here.

## Current Phase

- Week 1 Platform Foundation is complete.
- Week 2 First Business Loop - Tenant User Management is complete.
- Week 3 Ticket Workflow - System of Action is complete.
- Week 4 Audit Trail And Approval Patterns is complete.
- Week 5 Async Import And Data Operations is complete.
- Week 6 AI Copilot For Ticket Operations is the active phase.
- Week 5 still provides the current async-operations baseline: import submission/list/detail/errors, narrowed `USER_CSV` business-row execution, replay variants, queued-job recovery, stale-processing handling, and throughput guardrails.
- Exact current endpoint inventory and current limitations live in [project-status.md](project-status.md) and the matching pages under [reference/](reference/README.md).

## Current Focus

Week 6 should stay narrow and workflow-oriented:

- the current public AI slices are ticket summary and ticket triage: `POST /api/v1/tickets/{id}/ai-summary` and `POST /api/v1/tickets/{id}/ai-triage`
- keep the current slices suggestion-only, read-only, tenant-scoped, RBAC-scoped, and failure-tolerant
- preserve explicit prompt versioning, model tracking, latency capture, and separate AI interaction persistence across the public ticket AI slices
- keep the completed Week 5 import baseline stable while Week 6 AI work expands carefully

## Recommended Next Steps

- keep the ticket summary and ticket triage contracts stable while validating provider configuration, timeout behavior, and golden-sample regression coverage
- consider reply-draft suggestion after summary and triage are credible, still without widening into automatic write-back
- avoid turning Week 6 into a generic chatbot shell, agent loop, or tenant-BYOK project before the narrow ticket slices are proven

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

- Current tagged milestone: `v0.2.0-alpha` marks the completed Week 5 async import and data operations preview on 2026-03-19.
- Previous tagged milestone: `v0.1.3` marks the completed Week 4 audit and approval baseline on 2026-03-12.
- Earlier tagged milestones: `v0.1.2` marks the completed Week 3 ticket workflow baseline on 2026-03-11, `v0.1.1` marks Week 2 tenant user management loop complete on 2026-03-11, and `v0.1.0` marks Week 1 Platform Foundation on 2026-03-09.
- Week 6 or Week 7 target: make the first public open-source release that can honestly present the project as an AI-enhanced vertical SaaS, for example `v0.3.0-beta`.
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery.

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Historical implementation detail belongs in [project-status.md](project-status.md), [project-plan.md](project-plan.md), and [CHANGELOG.md](../CHANGELOG.md), not here.
