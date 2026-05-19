---
name: import-surface-sync
description: Align import-related repository docs with the current public import workflow and import AI surface. Use when import endpoints, request or response shapes, filters, replay modes, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`, `GET /api/v1/import-jobs/{id}/ai-interactions`, worker behavior, runtime guardrails, import AI runtime wording, approval-backed import execution bridges, or import verification steps change and the import docs, AI docs, runbooks, examples, approval docs, and milestone text must move together.
---

# Import Surface Sync

## Overview

Use this skill when import workflow or import AI work spreads across multiple doc layers at once. Keep it centered on the current public import workflow surface, approval-backed import execution bridges, and supporting runtime behavior, including the current import AI surface, without letting import-specific routing sprawl back into `AGENTS.md`.

Responsibility split:

- `import-surface-sync` owns import workflow docs, approval-backed import execution bridge docs, import AI docs, examples, runbooks, and milestone wording
- `ai-ticket-surface-sync` stays focused on the ticket AI surface
- `phase-status-sync` still owns the division of facts across status, roadmap, and plan
- `release-tag-prep` still owns release-cut, tag, changelog, and tagged-baseline wording
- `doc-staged-sync` owns shared AI governance wording that spans both ticket and import AI instead of a clearly import-only surface update

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [docs/reference/import-jobs.md](../../../docs/reference/import-jobs.md), [docs/reference/api-docs.md](../../../docs/reference/api-docs.md), [docs/reference/ai-integration.md](../../../docs/reference/ai-integration.md), [docs/reference/ai-provider-configuration.md](../../../docs/reference/ai-provider-configuration.md), [docs/ai/README.md](../../../docs/ai/README.md), [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md), [docs/runbooks/local-smoke-test.md](../../../docs/runbooks/local-smoke-test.md), [docs/runbooks/ai-live-smoke-test.md](../../../docs/runbooks/ai-live-smoke-test.md), [docs/runbooks/ai-regression-checklist.md](../../../docs/runbooks/ai-regression-checklist.md), [docs/runbooks/regression-checklist.md](../../../docs/runbooks/regression-checklist.md), and [../../../api-demo.http](../../../api-demo.http) before editing.
2. Classify the import change:
   - public read surface change: list, detail, errors, filters, derived fields
   - public write surface change: create, replay, selective replay, edited replay, whole-file replay
   - approval-backed execution bridge change: selective replay proposals, approval handoff, executable payload semantics, or import-specific AI provenance on proposal flows
   - import AI public surface change: AI generation, interaction history, narrowed metadata, sanitized context, or grounded response fields
   - runtime or worker hardening: chunking, max rows, counters, request-id propagation, file storage, execution service changes
   - architecture or lineage note: replay strategy, storage strategy, backlog follow-up
3. Route updates by change type:
   - public surface: update [docs/reference/import-jobs.md](../../../docs/reference/import-jobs.md), [docs/reference/api-docs.md](../../../docs/reference/api-docs.md), [../../../api-demo.http](../../../api-demo.http), matching runbooks, and [docs/project-status.md](../../../docs/project-status.md)
   - approval-backed execution bridge: update [docs/reference/import-jobs.md](../../../docs/reference/import-jobs.md), [docs/reference/audit-approval.md](../../../docs/reference/audit-approval.md), [docs/reference/authentication-and-rbac.md](../../../docs/reference/authentication-and-rbac.md), [docs/reference/api-docs.md](../../../docs/reference/api-docs.md), [../../../api-demo.http](../../../api-demo.http), matching runbooks, and [docs/project-status.md](../../../docs/project-status.md)
   - import AI surface: update [docs/reference/import-jobs.md](../../../docs/reference/import-jobs.md), [docs/reference/ai-integration.md](../../../docs/reference/ai-integration.md), [docs/reference/api-docs.md](../../../docs/reference/api-docs.md), related pages under [docs/ai/README.md](../../../docs/ai/README.md), [../../../api-demo.http](../../../api-demo.http), matching AI runbooks, and [docs/project-status.md](../../../docs/project-status.md)
   - runtime or guardrails: update [docs/reference/configuration.md](../../../docs/reference/configuration.md), [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md), [docs/project-status.md](../../../docs/project-status.md), and [docs/roadmap.md](../../../docs/roadmap.md) as needed
   - architecture or lineage: update the relevant page under [docs/architecture/README.md](../../../docs/architecture/README.md) plus any affected milestone docs
   - release-worthy change: update [../../../CHANGELOG.md](../../../CHANGELOG.md)
4. Keep import wording precise:
   - only call an import endpoint public if it is controller-backed and visible in Swagger
   - keep CSV header examples, filter lists, and replay constraints aligned with code
   - distinguish parse or header failures from business-row execution failures
   - keep `errorCodeCounts`, `/errors`, and replay lineage responsibilities distinct
   - keep approval-backed selective replay proposals human-reviewed until approval, and treat `sourceInteractionId` as provenance only unless code changed that execution boundary
   - keep import AI endpoints suggestion-only or read-only unless code changed that boundary
   - keep import AI grounded fields, sanitized-context wording, and `ai_interaction_record` semantics aligned with the current contract
5. Keep verification docs realistic:
   - `local-smoke-test` should only claim executable local paths
   - move complex failure or replay coverage into `regression-checklist` when local smoke would become too noisy
   - keep automated test coverage notes aligned with actual test classes and runtime boundaries
   - use `ai-live-smoke-test` and `ai-regression-checklist` when import AI live-vendor or degraded-mode behavior changed
6. Finish with an import audit:
   - sample CSV column order still matches code
   - example request bodies still match current replay mode and proposal semantics
   - runbooks do not overclaim unsupported failure paths
   - approval, RBAC, and proposal wording still matches the current selective replay bridge
   - import AI response fields and interaction-history metadata still match current Swagger-visible shapes
   - README and phase docs use high-level import wording instead of duplicating full API details

## Repo Anchors

- [docs/reference/import-jobs.md](../../../docs/reference/import-jobs.md)
- [docs/reference/audit-approval.md](../../../docs/reference/audit-approval.md)
- [docs/reference/authentication-and-rbac.md](../../../docs/reference/authentication-and-rbac.md)
- [docs/reference/api-docs.md](../../../docs/reference/api-docs.md)
- [docs/reference/ai-integration.md](../../../docs/reference/ai-integration.md)
- [docs/reference/ai-provider-configuration.md](../../../docs/reference/ai-provider-configuration.md)
- [docs/reference/configuration.md](../../../docs/reference/configuration.md)
- [docs/ai/README.md](../../../docs/ai/README.md)
- [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md)
- [docs/runbooks/local-smoke-test.md](../../../docs/runbooks/local-smoke-test.md)
- [docs/runbooks/ai-live-smoke-test.md](../../../docs/runbooks/ai-live-smoke-test.md)
- [docs/runbooks/ai-regression-checklist.md](../../../docs/runbooks/ai-regression-checklist.md)
- [docs/runbooks/regression-checklist.md](../../../docs/runbooks/regression-checklist.md)
- [../../../api-demo.http](../../../api-demo.http)
- [docs/project-status.md](../../../docs/project-status.md)
- [docs/roadmap.md](../../../docs/roadmap.md)
- [../../../CHANGELOG.md](../../../CHANGELOG.md)

## Output Shape

- State the import change type first.
- List the docs or runbooks updated or confirmed aligned.
- Call out remaining import doc gaps, or explicitly say none remain.
