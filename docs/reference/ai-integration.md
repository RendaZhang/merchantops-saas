# AI Integration

Last updated: 2026-03-21

## Purpose

This project treats AI as an embedded workflow layer, not as a standalone chatbot feature.

The current target direction is:

- tenant-scoped AI Copilot inside real business workflows
- human-reviewed suggestions before any higher-risk write action
- auditable and evaluable AI outputs
- clear RBAC boundaries around AI-assisted actions

## Current Public Boundary

The current public AI contracts are now live:

| Method | Path | Permission | Current Scope |
| --- | --- | --- | --- |
| `POST` | `/api/v1/tickets/{id}/ai-summary` | `TICKET_READ` | Generate a suggestion-only summary for one current-tenant ticket |
| `POST` | `/api/v1/tickets/{id}/ai-triage` | `TICKET_READ` | Generate suggestion-only classification and priority guidance for one current-tenant ticket |
| `POST` | `/api/v1/tickets/{id}/ai-reply-draft` | `TICKET_READ` | Generate a suggestion-only internal ticket comment draft for one current-tenant ticket |

Current Week 6 scope is intentionally narrow:

- three public AI endpoints only
- no request body; the server derives the prompt from the current tenant-scoped ticket detail context
- no ticket status change, comment write, approval trigger, or other workflow mutation
- no public raw prompt, raw provider response, token breakdown, or cost breakdown in the response body

## Public Response Contract

`POST /api/v1/tickets/{id}/ai-summary` returns a minimal suggestion shape:

- `ticketId`
- `summary`
- `promptVersion`
- `modelId`
- `generatedAt`
- `latencyMs`
- `requestId`

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": 302,
    "summary": "Issue: Printer cable replacement is in progress under ops. Current: the ticket is assigned and the latest signal says cable swap started. Next: confirm the replacement outcome and close the ticket if the printer is healthy.",
    "promptVersion": "ticket-summary-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-19T13:20:15",
    "latencyMs": 412,
    "requestId": "ticket-ai-summary-req-1"
  }
}
```

`POST /api/v1/tickets/{id}/ai-triage` returns a minimal suggestion shape:

- `ticketId`
- `classification`
- `priority`
- `reasoning`
- `promptVersion`
- `modelId`
- `generatedAt`
- `latencyMs`
- `requestId`

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": 302,
    "classification": "DEVICE_ISSUE",
    "priority": "HIGH",
    "reasoning": "The ticket describes a store printer issue affecting active operations and the latest signal still points to an unfinished hardware fix, so it should be treated as a high-priority device issue.",
    "promptVersion": "ticket-triage-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-21T14:20:15",
    "latencyMs": 418,
    "requestId": "ticket-ai-triage-req-1"
  }
}
```

`POST /api/v1/tickets/{id}/ai-reply-draft` returns a structured internal draft shape:

- `ticketId`
- `draftText`
- `opening`
- `body`
- `nextStep`
- `closing`
- `promptVersion`
- `modelId`
- `generatedAt`
- `latencyMs`
- `requestId`

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": 302,
    "draftText": "Quick update from ops.\n\nThe ticket is still in progress and the latest ticket activity confirms the cable swap has started for the store printer issue.\n\nNext step: Confirm whether the replacement restored printer health and note any blocker before moving toward closure.\n\nI will add another internal update once the verification result is confirmed.",
    "opening": "Quick update from ops.",
    "body": "The ticket is still in progress and the latest ticket activity confirms the cable swap has started for the store printer issue.",
    "nextStep": "Confirm whether the replacement restored printer health and note any blocker before moving toward closure.",
    "closing": "I will add another internal update once the verification result is confirmed.",
    "promptVersion": "ticket-reply-draft-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-21T15:10:15",
    "latencyMs": 436,
    "requestId": "ticket-ai-reply-draft-req-1"
  }
}
```

## Current Context Assembly Boundary

The current summary, triage, and reply-draft prompts are built only from the target ticket in the current tenant:

- ticket core fields
- current status, assignee, creator, and timestamps
- the most recent 20 ticket comments
- the most recent 20 workflow-level `ticket_operation_log` entries

Current prompt shaping stays intentionally narrow:

- if more than 20 comments or workflow logs exist, the prompt keeps the most recent window, restores that window to ascending order, and marks the section with an `earlier ... omitted` line
- `description` is truncated before prompt assembly
- each comment and workflow-log detail is truncated before prompt assembly

The current implementation intentionally does not pull in:

- attachments
- external systems
- other tickets
- import-job data
- cross-tenant examples or historical corpus lookups

Tenant scoping is inherited from the existing ticket query path. The AI slice does not bypass ticket read rules.

## Current Provider And Execution Boundary

The current runtime keeps AI plumbing narrow rather than introducing a general chat framework:

- a ticket-summary-specific prompt builder and provider adapter
- a ticket-triage-specific prompt builder and provider adapter
- a ticket-reply-draft-specific prompt builder and provider adapter
- instance-level provider configuration under `merchantops.ai.*`
- an enable/disable flag plus provider-configuration guard
- timeout-based controlled degradation

The current provider adapters call an OpenAI-compatible Responses API shape and request strict JSON schemas:

- summary requires `summary`
- triage requires `classification`, `priority`, and `reasoning`
- reply draft requires `opening`, `body`, `nextStep`, and `closing`
- request tests lock `Authorization`, `X-Client-Request-Id`, model id, `input` roles, and `text.format` schema wiring for all three adapters
- response parsing now scans all `output[].content[]` parts, concatenates later `output_text` fragments in order, and ignores earlier non-text parts when valid text exists
- upstream `408` and `504` HTTP responses are classified as provider timeouts so the timeout degradation path and `ai_interaction_record.status=PROVIDER_TIMEOUT` stay aligned

The current implementation does not include:

- tenant BYOK
- streaming
- tool calling
- model routing
- agent loops
- automatic retries with write-back behavior

## Audit And Traceability Baseline

Week 6 does not overload the existing generic `audit_event` model with AI runtime metadata.

Instead, AI invocations now persist a dedicated `ai_interaction_record` row with:

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
- optional usage and cost placeholders when available

Current status values include:

- `SUCCEEDED`
- `FEATURE_DISABLED`
- `PROVIDER_NOT_CONFIGURED`
- `PROVIDER_TIMEOUT`
- `PROVIDER_UNAVAILABLE`
- `INVALID_RESPONSE`

This record is governance-facing internal persistence. There is no public read API for AI interaction history yet.

## Evaluation Baseline

The current public AI slices establish a minimal eval path:

- explicit prompt versioning through `ticket-summary-v1`, `ticket-triage-v1`, and `ticket-reply-draft-v1`
- golden-sample ticket inputs at `merchantops-api/src/test/resources/ai/ticket-summary/golden-samples.json`, `merchantops-api/src/test/resources/ai/ticket-triage/golden-samples.json`, and `merchantops-api/src/test/resources/ai/ticket-reply-draft/golden-samples.json`
- checked-in provider-response fixtures per workflow that drive the real provider parser and service path in automated golden tests
- focused automated tests for happy path, permission failure, tenant isolation, feature-disabled behavior, timeout degradation, request-contract assertions, multi-part `output_text` parsing, and endpoint-specific required-field validation
- the operational checklist in [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md)

## Failure And Safety Expectations

Current non-happy-path behavior:

- missing `TICKET_READ` remains `403`
- cross-tenant or missing tickets remain `404`
- disabled AI returns controlled `503 SERVICE_UNAVAILABLE` messages such as `ticket ai summary is disabled`, `ticket ai triage is disabled`, or `ticket ai reply draft is disabled`
- missing provider configuration or provider failure returns controlled `503 SERVICE_UNAVAILABLE`
- raw provider exceptions are not exposed to API consumers
- the rest of the ticket workflow remains usable even when AI is unavailable

## Current Limitations

The current public AI slices are intentionally narrower than the Week 6 long-range plan.

Not implemented yet:
- any AI-driven ticket write-back flow
- approval-integrated AI execution
- tenant-level BYOK
- public AI usage or cost reporting
- attachments or external-system context enrichment

## Planned Next Workflow Areas

Near-term Week 6 follow-up work should stay in the ticket workflow lane:

- stronger failure-set and policy-set eval coverage
- future approval-aware write-back only after the suggestion-only slices are stable

Later roadmap areas remain:

- Week 7 import and data-quality AI workflows
- Week 8 agentic workflows with human oversight
- Week 9 broader AI governance, cost, and usage reporting

## Related Documents

- [ticket-workflow.md](ticket-workflow.md): current ticket endpoint surface and workflow details
- [authentication-and-rbac.md](authentication-and-rbac.md): auth and permission boundaries
- [ai-provider-configuration.md](ai-provider-configuration.md): active provider-key ownership and config keys
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): rollout checklist for current and future AI endpoint changes
- [../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md](../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md): architecture decision for AI workflow placement and governance
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): minimum audit and eval baseline for public AI APIs
- [../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md](../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md): deployment-owned provider configuration baseline
- [../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md](../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md): separation rule for AI runtime traceability versus generic business audit
