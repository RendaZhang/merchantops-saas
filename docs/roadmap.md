# Roadmap

Last updated: 2026-03-22

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

- the current public AI slices are ticket interaction history, ticket summary, ticket triage, and ticket internal reply draft: `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft`
- keep the public AI surface read-only, and keep the generation slices suggestion-only, tenant-scoped, RBAC-scoped, and failure-tolerant
- preserve explicit prompt versioning, model tracking, latency capture, separate AI interaction persistence, and narrowed operator-visible interaction history across the public ticket AI slices
- treat symmetric degraded-mode, provider-adapter, golden-sample, and no-side-effect coverage across the three endpoints as part of the current Week 6 baseline, not as the next missing slice
- keep the completed Week 5 import baseline stable while Week 6 AI work expands carefully

## Recommended Next Steps

- add a narrow public AI usage / cost read surface on top of existing `ai_interaction_record` data, still under tenant scope and without widening Week 6 into write-back
- keep the ticket interaction-history, ticket summary, ticket triage, and ticket reply-draft contracts stable while layering operator-visible usage / cost visibility on top of the current Week 6 baseline
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
