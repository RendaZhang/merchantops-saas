# ADR-0012: Keep AI Interaction Records Separate From Generic Audit Events

- Status: Accepted
- Date: 2026-03-19

## Context

MerchantOps SaaS has started Week 6 by publishing its first public AI endpoint: `POST /api/v1/tickets/{id}/ai-summary`.

That endpoint is intentionally read-only and suggestion-only, but it still needs runtime traceability for:

- which tenant and user invoked the AI path
- which ticket the request targeted
- which prompt version and model were used
- whether the AI call succeeded, timed out, was disabled, or failed for another controlled reason
- how long the AI call took
- what summary was returned to the operator
- usage and cost metadata when the provider exposes them

The repository already has a generic `audit_event` backbone for business write governance and an existing `ticket_operation_log` for ticket workflow playback.

If the project tries to force AI runtime metadata into `audit_event`, several problems appear:

1. read-only AI calls start looking like ordinary business write audit rows even when no business state changed
2. provider-specific fields such as `promptVersion`, `modelId`, latency, usage, and output summary overload a model designed for before/after business snapshots
3. the existing `audit_event` read surface becomes harder to interpret because business governance records and AI runtime traces are mixed together

Week 6 therefore needs a clear persistence rule for AI runtime traceability.

## Decision

MerchantOps SaaS will persist AI runtime traceability in a dedicated `ai_interaction_record` model instead of overloading generic `audit_event` rows.

That means:

- keep `ticket_operation_log` for ticket workflow playback
- keep `audit_event` for reusable business-governance audit of write actions
- add `ai_interaction_record` for AI invocation metadata, including read-only suggestion calls
- allow future workflows to emit both an AI interaction record and a business audit event when an AI-assisted action later leads to an approved business write

The AI interaction record should capture fields such as:

- `tenantId`
- `userId`
- `requestId`
- `entityType`
- `entityId`
- `interactionType`
- `promptVersion`
- `modelId`
- `status`
- `latencyMs`
- `outputSummary`
- usage or cost fields when available
- `createdAt`

The current public API does not expose this table directly. It exists first as a governance and supportability baseline.

## Consequences

- the Week 6 AI slice can remain read-only in business terms while still being fully traceable in runtime terms
- `audit_event` remains easier to read because it continues to represent business-governance history rather than provider-runtime telemetry
- some identifiers such as `tenantId`, `userId`, `requestId`, and `entityId` now appear in more than one persistence layer, and that duplication is intentional
- future AI-assisted write flows can connect AI interaction history and business audit history without forcing them into the same schema too early
- tests and docs should verify the distinction clearly: workflow logs are module-facing, `audit_event` is business-governance-facing, and `ai_interaction_record` is AI-runtime-facing
