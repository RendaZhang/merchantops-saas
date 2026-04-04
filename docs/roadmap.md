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
- Week 8 Agentic Workflows With Human Oversight is complete.
- Week 9 AI Governance, Eval, Cost, and Usage is the current active phase.
- Week 5 still provides the current async-operations baseline: import submission/list/detail/errors, narrowed `USER_CSV` business-row execution, replay variants, queued-job recovery, stale-processing handling, and throughput guardrails.
- Exact current endpoint inventory and current limitations live in [project-status.md](project-status.md) and the matching pages under [reference/](reference/README.md).

## Current Focus

The current Week 9 implementation should build governance and observability on top of the now-completed Week 8 workflow baseline:

- keep the completed Week 6 ticket AI surface stable: ticket interaction history, ticket summary, ticket triage, and ticket internal reply draft remain `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft`
- keep the completed Week 7 import AI surface stable: `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` stay `USER_READ`, read-only, tenant-scoped, and suggestion-only
- treat the completed Week 8 workflow baseline as fixed input: `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`, action-aware approval routing, and shared pending-request-key hardening now form the human-reviewed execution bridge pattern
- add governance and visibility around that baseline: prompt-version discipline, eval datasets, regression comparators, usage and cost visibility, and rollout-safe verification rather than another immediate workflow mutation slice
- keep approval payloads narrow and non-sensitive while governance work lands; avoid broader write-back, billing, ledger semantics, and generic chat tooling in the public workflow surface itself

## Recommended Next Steps

- treat the completed Week 8 import selective replay proposal flow plus the completed Week 8 ticket reply-draft comment proposal flow, together with the shared pending-proposal uniqueness hardening, as the fixed proposal -> approval -> execution baseline for later work
- start Week 9 by tightening the AI governance loop: prompt-version inventory, eval dataset coverage, regression comparators, and cost/usage visibility over the existing public AI slices
- keep the completed Week 6 ticket AI surface and the completed Week 7 import AI read surface stable while Week 9 adds governance and observability instead of widening the AI endpoints themselves
- only consider another bonus workflow slice after the Week 9 governance/eval/cost baseline is credible enough to support a broader beta story

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

- Current tagged milestone: `v0.5.0-beta` marks the completed Week 8 Agentic Workflows with Human Oversight beta baseline on 2026-04-04.
- Previous tagged milestone: `v0.4.0-beta` marks the completed Week 7 AI Copilot for Import and Data Quality beta baseline on 2026-03-28.
- Earlier tagged milestones: `v0.2.0-alpha` marks the completed Week 5 async import and data operations preview on 2026-03-19, `v0.1.3` marks the completed Week 4 audit and approval baseline on 2026-03-12, `v0.1.2` marks the completed Week 3 ticket workflow baseline on 2026-03-11, `v0.1.1` marks Week 2 tenant user management loop complete on 2026-03-11, and `v0.1.0` marks Week 1 Platform Foundation on 2026-03-09.
- The current open-source line can now honestly present the project as a workflow-first, AI-enhanced vertical SaaS backend through the completed Week 8 tag while Week 9 governance and observability work stays active above that baseline.
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery.

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Historical implementation detail belongs in [project-status.md](project-status.md), [project-plan.md](project-plan.md), and [CHANGELOG.md](../CHANGELOG.md), not here.
