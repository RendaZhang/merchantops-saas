# Roadmap

Last updated: 2026-04-06

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
- Week 9 AI Governance, Eval, Cost, and Usage is complete.
- Week 10 Delivery Hardening and Portfolio Packaging is the current active phase.
- Week 5 still provides the current async-operations baseline: import submission/list/detail/errors, narrowed `USER_CSV` business-row execution, replay variants, queued-job recovery, stale-processing handling, and throughput guardrails.
- Exact current endpoint inventory and current limitations live in [project-status.md](project-status.md) and the matching pages under [reference/](reference/README.md).

## Current Focus

The current Week 10 active state should start delivery hardening on top of the now-completed Week 9 governance baseline:

- keep the completed Week 6 ticket AI surface stable: ticket interaction history, ticket summary, ticket triage, and ticket internal reply draft remain `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft`
- keep the completed Week 7 import AI surface stable: `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` stay `USER_READ`, read-only, tenant-scoped, and suggestion-only
- treat the completed Week 8 workflow baseline as fixed input: `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`, action-aware approval routing, and shared pending-request-key hardening now form the human-reviewed execution bridge pattern
- Week 9 Slice A is now complete on that baseline: executable prompt-version inventory, per-workflow golden plus failure plus policy datasets, and one shared comparator pass now run in the default Maven suite without widening the public AI surface
- Week 9 Slice B is now complete on that baseline: `GET /api/v1/ai-interactions/usage-summary` adds one tenant-scoped governance read over stored `ai_interaction_record` rows with inclusive `from` / `to`, narrow exact-match filters, aggregate totals, and stable breakdown ordering while staying read-only and outside billing or ledger semantics
- Week 9 Slice C is now complete on that same surface: the tenant usage-summary response now also exposes stable `byPromptVersion` aggregate visibility without adding a new endpoint, filter, billing surface, or per-request cross-entity detail list
- treat the completed Week 9 governance baseline as fixed input: the executable six-workflow prompt inventory, shared comparator pass, tenant usage-summary aggregate reads, and `byPromptVersion` visibility are now part of the tagged baseline
- keep approval payloads narrow and non-sensitive while Week 10 delivery hardening lands; avoid broader write-back, billing, ledger semantics, and generic chat tooling in the public workflow surface itself

## Recommended Next Steps

- treat the completed Week 8 import selective replay proposal flow plus the completed Week 8 ticket reply-draft comment proposal flow, together with the shared pending-proposal uniqueness hardening, as the fixed proposal -> approval -> execution baseline for later work
- treat the completed Week 9 Slice A prompt-inventory plus comparator baseline and the completed Week 9 Slice B plus Slice C tenant usage-summary read as the fixed governance input for Week 10
- next, add Week 10 delivery-hardening slices such as feature flags, Dockerfile and delivery docs, minimal CI/CD, and portfolio packaging
- keep the completed Week 6 ticket AI surface, the completed Week 7 import AI read surface, and the completed Week 8 workflow bridges stable while Week 10 improves delivery readiness instead of widening the AI endpoints themselves

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

- Current tagged milestone: `v0.6.0-beta` marks the completed Week 9 AI Governance, Eval, Cost, and Usage beta baseline on 2026-04-06.
- Previous tagged milestone: `v0.5.0-beta` marks the completed Week 8 Agentic Workflows with Human Oversight beta baseline on 2026-04-04.
- Earlier previous tagged milestone: `v0.4.0-beta` marks the completed Week 7 AI Copilot for Import and Data Quality beta baseline on 2026-03-28.
- Earlier tagged milestones: `v0.2.0-alpha` marks the completed Week 5 async import and data operations preview on 2026-03-19, `v0.1.3` marks the completed Week 4 audit and approval baseline on 2026-03-12, `v0.1.2` marks the completed Week 3 ticket workflow baseline on 2026-03-11, `v0.1.1` marks Week 2 tenant user management loop complete on 2026-03-11, and `v0.1.0` marks Week 1 Platform Foundation on 2026-03-09.
- The current open-source line can now honestly present the project as a workflow-first, AI-enhanced vertical SaaS backend through the completed Week 9 governance, eval, cost, and usage beta baseline.
- Week 10 target: reach a more stable open-source reference-implementation milestone and gather input for later commercial discovery.

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Historical implementation detail belongs in [project-status.md](project-status.md), [project-plan.md](project-plan.md), and [CHANGELOG.md](../CHANGELOG.md), not here.
