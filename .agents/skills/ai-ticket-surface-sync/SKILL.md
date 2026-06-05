---
name: ai-ticket-surface-sync
description: Align the repository docs for the current public ticket AI surface. Use when `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, or `POST /api/v1/tickets/{id}/ai-reply-draft` changes, or when ticket AI provider configuration, degraded-mode behavior, AI runbooks, AI examples, changelog wording, or phase docs must move together with the current public ticket AI implementation.
---

# AI Ticket Surface Sync

## Overview

Use this skill when ticket AI work spreads across reference docs, runbooks, examples, and milestone text at once. Keep it focused on the current public ticket AI surface rather than generic future AI work.

When the same ticket AI change also moves phase framing or release framing, combine this skill with the narrower companion skill that owns that concern:

- pair it with [phase-status-sync](../phase-status-sync/SKILL.md) when `docs/project-status.md`, `docs/roadmap.md`, `docs/project-plan.md`, or `docs/product-strategy.md` need the same ticket-AI boundary or active-slice adjustment
- pair it with [release-tag-prep](../release-tag-prep/SKILL.md) when the same AI doc change is part of pre-tag, post-tag, release-cut, or open-source milestone wording
- prefer [doc-staged-sync](../doc-staged-sync/SKILL.md) when the work is a shared AI governance read or wording change that spans both ticket and import AI instead of a ticket-only surface update

Responsibility split:

- `ai-ticket-surface-sync` owns ticket AI reference docs, runbooks, examples, provider/runtime wording, and current AI boundary wording
- `phase-status-sync` owns the division of facts across status, roadmap, and plan
- `release-tag-prep` owns tag baseline, changelog, release-versioning, README baseline wording, and open-source release-cut framing
- `doc-staged-sync` owns shared AI governance wording that is not clearly ticket-only or import-only

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [docs/reference/ai-integration.md](../../../docs/reference/ai-integration.md), [docs/reference/ai-provider-configuration.md](../../../docs/reference/ai-provider-configuration.md), [docs/reference/ticket-workflow.md](../../../docs/reference/ticket-workflow.md), [docs/reference/api-docs.md](../../../docs/reference/api-docs.md), [docs/runbooks/ai-regression-checklist.md](../../../docs/runbooks/ai-regression-checklist.md), [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md), [docs/runbooks/local-smoke-test.md](../../../docs/runbooks/local-smoke-test.md), and [../../../api-demo.http](../../../api-demo.http) before editing.
2. Classify the AI change:
   - public AI endpoint or Swagger contract change
   - AI runtime, degraded-mode, provider-behavior, or automated-coverage change
   - AI milestone, current-boundary, or release-level wording change
3. Route updates by change type:
   - public contract: update [docs/reference/ai-integration.md](../../../docs/reference/ai-integration.md), [docs/reference/ticket-workflow.md](../../../docs/reference/ticket-workflow.md), [docs/reference/api-docs.md](../../../docs/reference/api-docs.md), [../../../api-demo.http](../../../api-demo.http), matching AI runbooks, and [docs/project-status.md](../../../docs/project-status.md)
   - runtime or provider behavior: update [docs/reference/ai-provider-configuration.md](../../../docs/reference/ai-provider-configuration.md), [docs/runbooks/ai-regression-checklist.md](../../../docs/runbooks/ai-regression-checklist.md), [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md), and [docs/project-status.md](../../../docs/project-status.md)
   - milestone or release wording: update [../../../README.md](../../../README.md), [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), and [../../../CHANGELOG.md](../../../CHANGELOG.md) as needed
4. Keep ticket AI wording precise:
   - call the endpoints public only when they are controller-backed and Swagger-visible
   - keep the slices suggestion-only, read-only, tenant-scoped, and `TICKET_READ`-scoped unless code changed those boundaries
   - distinguish `ai_interaction_record` from generic `audit_event`
   - keep instance-level provider ownership and degraded-mode wording aligned with the current runtime
   - do not imply write-back, comment creation, streaming, tool calling, or BYOK when those are still out of scope
5. Keep verification docs realistic:
   - `ai-regression-checklist` owns AI-specific safety, provider, and eval checks
   - `local-smoke-test` should only claim executable local paths and should point to the AI checklist when live provider validation is out of scope
   - `automated-tests` should track real provider-adapter and degraded-mode coverage rather than generic AI aspirations
6. Finish with an AI audit:
   - `ai-summary`, `ai-triage`, and `ai-reply-draft` wording is symmetric where it should be
   - examples and response fields still match the current public contract
   - README stays high-level and does not duplicate the full AI contract
   - phase docs reflect the current public ticket AI boundary without pretending later AI work is already public

## Repo Anchors

- [docs/reference/ai-integration.md](../../../docs/reference/ai-integration.md)
- [docs/reference/ai-provider-configuration.md](../../../docs/reference/ai-provider-configuration.md)
- [docs/reference/ticket-workflow.md](../../../docs/reference/ticket-workflow.md)
- [docs/reference/api-docs.md](../../../docs/reference/api-docs.md)
- [docs/runbooks/ai-regression-checklist.md](../../../docs/runbooks/ai-regression-checklist.md)
- [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md)
- [docs/project-status.md](../../../docs/project-status.md)
- [docs/roadmap.md](../../../docs/roadmap.md)
- [../../../CHANGELOG.md](../../../CHANGELOG.md)
- [../../../api-demo.http](../../../api-demo.http)

## Output Shape

- State the AI change type first.
- List the docs updated or confirmed already aligned.
- Call out remaining AI doc gaps, or explicitly say none remain.
