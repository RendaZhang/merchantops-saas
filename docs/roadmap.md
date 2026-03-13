# Roadmap

Last updated: 2026-03-13

> Maintenance note: keep this page focused on the active phase, the next recommended slices, and near-term sequencing. Link to [project-status.md](project-status.md) and the relevant pages under [reference/](reference/README.md) for exact current contracts instead of repeating full implementation inventories here.

## Current Phase

- Week 1 Platform Foundation is complete.
- Week 2 First Business Loop - Tenant User Management is complete.
- Week 3 Ticket Workflow - System of Action is complete.
- Week 4 Audit Trail And Approval Patterns is complete.
- Week 5 Async Import And Data Operations is the active phase.
- Week 5 Slice A is complete with import submission/list/detail plus queue backbone.
- Week 5 Slice B is complete with narrow `USER_CSV` business-row execution, row-level failure isolation, and filtered import queue reads.
- Week 5 Slice C is complete with paged import error reporting and detail-level `errorCodeCounts`.
- Week 5 Slice D is complete with failed-row replay as a derived import job plus `sourceJobId` lineage.
- Week 5 Slice E is complete with sequential chunked execution, per-chunk counter visibility, and basic throughput guardrails.
- Week 5 Slice F is complete with selective failed-row replay by exact `errorCode` plus `selectedErrorCodes` audit snapshots.
- Week 5 Slice G is complete with edited failed-row replay by exact `errorId`, full replacement-row input, and scope-only replay audit metadata.
- Week 5 Slice H is complete with whole-file replay for `FAILED` `USER_CSV` source jobs that have no successful rows plus `replayMode=WHOLE_FILE` audit snapshots.
- Exact current endpoint inventory and current limitations live in [project-status.md](project-status.md) and the matching pages under [reference/](reference/README.md).

## Current Focus

Week 5 should stay narrow and workflow-oriented:

- keep the landed `USER_CSV` schema and example files aligned across Swagger, `api-demo.http`, reference docs, and runbooks
- keep the filtered import queue read surface, replay-derived job semantics, sequential chunk runtime, and paged error surface operationally useful before adding more import breadth
- continue using import jobs as an async operations backbone, not as a generic bulk-admin shortcut
- keep audit and approval groundwork reusable for later Week 6-9 AI flows

## Recommended Next Steps

- keep the landed queue filters, `/errors` surface, replay-file boundary, replay lineage, edited replay semantics, chunk semantics, and job-summary semantics aligned across tests, Swagger, and reference docs
- add the next narrow follow-up slice such as broader import-type support or richer replay reporting now that Week 5 whole-file replay, selective replay, edited replay, and chunk / throughput controls are in place
- keep Week 2-4 docs aligned only where Week 5 changes shared workflow or governance expectations
- avoid pulling Week 6 AI scope forward until the import execution model, failure reporting, replay semantics, and replay breadth are stable

## Near-Term Sequence

- Week 5: async import and data operations
- Week 6: AI Copilot for ticket operations
- Week 7: AI Copilot for import and data quality
- Week 8: agentic workflows with human oversight
- Week 9: AI governance, eval, cost, and usage
- Week 10: delivery hardening, feature flags, and portfolio packaging
- Stretch after Week 10: usage / ledger / invoice minimal loop

## Active Phase Notes

Week 5 currently has eight clear slices:

- Slice A is complete:
  - public API includes create/list/detail for import jobs
  - create flow persists files locally, writes `QUEUED` jobs, and publishes RabbitMQ messages after commit
- Slice B is complete:
  - worker enforces fixed `USER_CSV` header `username,displayName,email,password,roleCodes`
  - valid rows create tenant users through the existing user command service path
  - row failures are isolated in `import_job_item_error` with parse and business codes such as `DUPLICATE_USERNAME`, `UNKNOWN_ROLE`, `INVALID_EMAIL`, and `INVALID_PASSWORD`
  - import job counters now reflect real create success/failure counts, with partial-success terminal `SUCCEEDED`
  - import queue reads now support `status`, `importType`, `requestedBy`, and `hasFailuresOnly` while keeping `createdAt DESC, id DESC`
- Slice C is complete:
  - detail now exposes `errorCodeCounts` for quick triage while preserving backward-compatible `itemErrors`
  - `GET /api/v1/import-jobs/{id}/errors` pages failure items with `page`, `size`, and exact `errorCode`
  - failure-item ordering is stable: null `rowNumber` first, then `rowNumber ASC, id ASC`
  - reporting now has a cleaner large-job read surface for larger jobs
- Slice D is complete:
  - `POST /api/v1/import-jobs/{id}/replay-failures` creates a new derived `QUEUED` job from replayable failed rows only
  - replay-derived jobs keep `sourceJobId` lineage and use the same standard worker path
  - system-generated replay files now use the same storage abstraction as uploaded files
  - audit stays bidirectional through `IMPORT_JOB_REPLAY_REQUESTED` on the source job and `IMPORT_JOB_CREATED` on the replay job
- Slice E is complete:
  - one job still maps to one worker, but the worker now processes internal sequential chunks instead of one long file transaction
  - `totalCount`, `successCount`, and `failureCount` now flush after each chunk so detail reads show real progress during `PROCESSING`
  - configurable `chunk-size` and `max-rows-per-job` guardrails now bound Week 5 runtime behavior without adding new public endpoints
  - oversized files now fail with `MAX_ROWS_EXCEEDED` instead of running unbounded
- Slice F is complete:
  - `POST /api/v1/import-jobs/{id}/replay-failures/selective` creates a new derived `QUEUED` job from replayable failed rows whose `errorCode` exactly matches one of the requested values
  - selective replay keeps the same standard worker path and `sourceJobId` lineage as the existing replay endpoint
  - empty `errorCodes`, cross-tenant source jobs, and selections with no replayable matching rows are rejected
  - selective replay keeps the schema unchanged in this slice and records `selectedErrorCodes` only in source/replay audit snapshots
- Slice G is complete:
  - `POST /api/v1/import-jobs/{id}/replay-failures/edited` creates a new derived `QUEUED` job from caller-provided full replacement rows keyed by replayable failed-row `errorId`
  - edited replay keeps the same standard worker path and `sourceJobId` lineage as the existing replay endpoints
  - duplicate `errorId`, cross-job / cross-tenant `errorId`, and header/global errors are rejected
  - edited replay records only edit scope metadata such as `editedErrorIds` and `editedRowCount`; replacement values are intentionally excluded from replay audit snapshots
- Slice H is complete:
  - `POST /api/v1/import-jobs/{id}/replay-file` creates a new derived `QUEUED` job by copying the stored source file
  - whole-file replay is intentionally limited to current-tenant `FAILED` `USER_CSV` source jobs that have no successful rows
  - partial-success jobs continue using row-level replay variants, and source/replay audit snapshots add `replayMode=WHOLE_FILE`

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
