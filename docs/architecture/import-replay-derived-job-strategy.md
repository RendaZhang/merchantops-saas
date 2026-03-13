# Import Replay Derived-Job Strategy

Last updated: 2026-03-12

## Purpose

This note records the intended replay boundary for the current Week 5 import path now that import jobs already support execution, queue reads, and paged error reporting.

## Current Decision

For the current Week 5 import slices:

- replay should create a new derived `import_job`, not mutate or reset the old one
- replay should stay row-level in Week 5, supporting failed-row replay, exact error-code selective replay, and edited failed-row replay
- edited replay should use the same derived-job model instead of overwriting the source job
- if whole-file replay is added later, its first implementation should be limited to `FAILED` source jobs; partial-success jobs should continue using row-level replay variants
- the replay job should keep a lineage link back to the source job

## Why

- the current import path already allows partial success, so rerunning the original job in place would blur which rows already succeeded
- preserving the original job makes counters, error summaries, and audit history stable after a run has finished
- a derived job keeps replay behavior aligned with the existing async import model instead of introducing a second execution path

## Implementation Boundary

- the original source job should remain immutable once it reaches a terminal state
- replay should create a new queued job with its own `id`, `requestId`, status timeline, and audit trail
- the new job should carry a reference such as `sourceJobId` back to the original job
- replay input should be built from the source job's failed-row material rather than by mutating the old stored file
- the worker should process replay jobs through the same normal import pipeline whenever possible
- replay-related audit should capture lineage and edit scope only; it must not persist sensitive edited values such as passwords or full replacement-row payloads
- whole-file replay should not be the default retry path for partial-success jobs because it would blur already-succeeded-row semantics

## Deferred On Purpose

The first replay slice should not require:

- resetting the old job back to `QUEUED` or `PROCESSING`
- replaying the entire original file
- a generic retry engine for every import type
- automatic dedupe or idempotency ledgers beyond current business validation rules

Those can be added later if replay becomes a larger product surface.

## Future Direction

Once replay is stable, later import slices can add more control around:

- replaying the entire original file without blurring already-succeeded-row semantics
- if whole-file replay is introduced, keeping its first version restricted to `FAILED` source jobs before considering wider retry semantics
- expanding edited replay beyond current full-row replacement input while keeping the derived-job model and scope-only audit metadata
- supporting replay for additional import types
- attaching richer lineage metadata between source jobs and replay jobs beyond the current `sourceJobId` plus selective-replay audit snapshots
- layering AI-assisted remediation on top of the same derived-job model
