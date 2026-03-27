# AI Integration

Last updated: 2026-03-27

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
| `GET` | `/api/v1/tickets/{id}/ai-interactions` | `TICKET_READ` | Page operator-visible AI interaction history for one current-tenant ticket |
| `POST` | `/api/v1/tickets/{id}/ai-summary` | `TICKET_READ` | Generate a suggestion-only summary for one current-tenant ticket |
| `POST` | `/api/v1/tickets/{id}/ai-triage` | `TICKET_READ` | Generate suggestion-only classification and priority guidance for one current-tenant ticket |
| `POST` | `/api/v1/tickets/{id}/ai-reply-draft` | `TICKET_READ` | Generate a suggestion-only internal ticket comment draft for one current-tenant ticket |
| `POST` | `/api/v1/import-jobs/{id}/ai-error-summary` | `USER_READ` | Generate a suggestion-only error summary for one current-tenant import job |
| `POST` | `/api/v1/import-jobs/{id}/ai-mapping-suggestion` | `USER_READ` | Generate a suggestion-only canonical-field mapping proposal for one current-tenant import job |

Current public AI scope is intentionally narrow:

- six public AI endpoints only: one ticket history read endpoint plus five suggestion-generating endpoints
- the generation endpoints use no request body; the server derives the prompt from current tenant-scoped ticket or import-job context
- the history endpoint supports `page`, `size`, `interactionType`, and `status` query params over stored `ai_interaction_record` rows
- no ticket status change, comment write, approval trigger, replay trigger, or other workflow mutation
- no public import AI history endpoint yet; import AI is public only as the one-shot read-only generation path
- no public raw prompt or raw provider response in the response body, and no billing or ledger semantics on the history response

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

`POST /api/v1/import-jobs/{id}/ai-error-summary` returns a minimal suggestion shape:

- `importJobId`
- `summary`
- `topErrorPatterns`
- `recommendedNextSteps`
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
    "importJobId": 1201,
    "summary": "The import is dominated by tenant role validation failures, with a smaller duplicate-username tail. The sampled failed rows are structurally complete, so the next human step is to correct role mappings first and then decide which usernames need edits before replay.",
    "topErrorPatterns": [
      "UNKNOWN_ROLE is the dominant error code in both the aggregated counts and the first failed-row window.",
      "Most sampled failed rows still contain username, displayName, email, password, and at least one role code."
    ],
    "recommendedNextSteps": [
      "Confirm which tenant role codes should replace the invalid mappings before replay.",
      "Review duplicate-username rows separately because those rows need edits rather than role-map cleanup."
    ],
    "promptVersion": "import-error-summary-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-27T10:25:15",
    "latencyMs": 512,
    "requestId": "import-ai-error-summary-req-1"
  }
}
```

`POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` returns a minimal suggestion shape:

- `importJobId`
- `summary`
- `suggestedFieldMappings`
- `confidenceNotes`
- `recommendedOperatorChecks`
- `promptVersion`
- `modelId`
- `generatedAt`
- `latencyMs`
- `requestId`

Each `suggestedFieldMappings[]` record includes:

- `canonicalField`
- `observedColumnSignal`
- `reasoning`
- `reviewRequired`

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "importJobId": 1202,
    "summary": "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.",
    "suggestedFieldMappings": [
      {
        "canonicalField": "username",
        "observedColumnSignal": {
          "headerName": "login",
          "headerPosition": 1
        },
        "reasoning": "`login` is the closest observed header for the canonical username field.",
        "reviewRequired": false
      },
      {
        "canonicalField": "displayName",
        "observedColumnSignal": {
          "headerName": "display_name",
          "headerPosition": 2
        },
        "reasoning": "`display_name` is the closest semantic match for displayName.",
        "reviewRequired": false
      },
      {
        "canonicalField": "email",
        "observedColumnSignal": {
          "headerName": "email_address",
          "headerPosition": 3
        },
        "reasoning": "`email_address` is the most likely email column.",
        "reviewRequired": false
      },
      {
        "canonicalField": "password",
        "observedColumnSignal": {
          "headerName": "passwd",
          "headerPosition": 4
        },
        "reasoning": "`passwd` should be manually confirmed.",
        "reviewRequired": true
      },
      {
        "canonicalField": "roleCodes",
        "observedColumnSignal": {
          "headerName": "roles",
          "headerPosition": 5
        },
        "reasoning": "`roles` is the closest available signal for roleCodes.",
        "reviewRequired": true
      }
    ],
    "confidenceNotes": [
      "The source file failed header validation, so each suggested mapping should be reviewed before reuse."
    ],
    "recommendedOperatorChecks": [
      "Confirm the source header order before editing any replay input.",
      "Verify that the observed `roles` column really contains tenant role codes in the expected delimiter format."
    ],
    "promptVersion": "import-mapping-suggestion-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-27T10:30:15",
    "latencyMs": 544,
    "requestId": "import-ai-mapping-suggestion-req-1"
  }
}
```

`GET /api/v1/tickets/{id}/ai-interactions` returns a narrowed page shape over stored ticket AI history:

- `items[]`
- `page`
- `size`
- `total`
- `totalPages`

Each `items[]` record includes:

- `id`
- `interactionType`
- `status`
- `outputSummary`
- `promptVersion`
- `modelId`
- `latencyMs`
- `requestId`
- `usagePromptTokens`
- `usageCompletionTokens`
- `usageTotalTokens`
- `usageCostMicros`
- `createdAt`

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 9003,
        "interactionType": "TRIAGE",
        "status": "INVALID_RESPONSE",
        "outputSummary": null,
        "promptVersion": "ticket-triage-v1",
        "modelId": "gpt-4.1-mini",
        "latencyMs": 251,
        "requestId": "ticket-ai-triage-invalid-response-1",
        "usagePromptTokens": null,
        "usageCompletionTokens": null,
        "usageTotalTokens": null,
        "usageCostMicros": null,
        "createdAt": "2026-03-22T09:00:00"
      },
      {
        "id": 9002,
        "interactionType": "REPLY_DRAFT",
        "status": "SUCCEEDED",
        "outputSummary": "nextStep=Confirm whether the replacement restored printer health and note any blocker before moving toward closure.",
        "promptVersion": "ticket-reply-draft-v1",
        "modelId": "gpt-4.1-mini",
        "latencyMs": 436,
        "requestId": "ticket-ai-reply-draft-req-1",
        "usagePromptTokens": 140,
        "usageCompletionTokens": 88,
        "usageTotalTokens": 228,
        "usageCostMicros": 2100,
        "createdAt": "2026-03-22T09:00:00"
      }
    ],
    "page": 0,
    "size": 2,
    "total": 3,
    "totalPages": 2
  }
}
```

## Current Context Assembly Boundary

The current ticket summary, triage, and reply-draft prompts are built only from the target ticket in the current tenant:

- ticket core fields
- current status, assignee, creator, and timestamps
- the most recent 20 ticket comments
- the most recent 20 workflow-level `ticket_operation_log` entries

Current ticket prompt shaping stays intentionally narrow:

- if more than 20 comments or workflow logs exist, the prompt keeps the most recent window, restores that window to ascending order, and marks the section with an `earlier ... omitted` line
- `description` is truncated before prompt assembly
- each comment and workflow-log detail is truncated before prompt assembly

The current import error-summary and mapping-suggestion prompts are built only from the target import job in the current tenant:

- import job core fields and counters from `ImportJobDetail`
- aggregated `errorCodeCounts`
- sanitized header/global parse-error signal derived from existing `rowNumber=null` item errors when present
- the first 20 failure rows from `GET /errors`-equivalent query ordering
- one local structural summary per failed row containing only presence/count signals such as `columnCount`, `usernamePresent`, `displayNamePresent`, `emailPresent`, `passwordPresent`, `roleCodesPresent`, and `roleCodeCount`

Current import prompt shaping stays intentionally narrow:

- the server sanitizes raw failed-row CSV locally before prompt assembly and never forwards raw `itemErrors.rawPayload`
- the mapping-suggestion slice forwards only normalized header names, header positions, header-column count, and bounded structural row summaries; it does not rescan the source file or infer from raw row values
- the prompt includes `rowNumber`, `errorCode`, and `errorMessage`, but not raw username, displayName, email, password, or role-code text
- if raw payload parsing fails, the prompt keeps only structural fallback metadata rather than sending the original row text

The current implementation intentionally does not pull in:

- attachments
- external systems
- other tickets
- full source-file rescans for import AI
- cross-tenant examples or historical corpus lookups

Tenant scoping is inherited from the existing ticket and import query paths. The AI slices do not bypass the existing read rules.

## Current Provider And Execution Boundary

The current runtime keeps AI plumbing narrow rather than introducing a general chat framework:

- a ticket-summary-specific prompt builder and output validator
- a ticket-triage-specific prompt builder and output validator
- a ticket-reply-draft-specific prompt builder and output validator
- an import-error-summary-specific prompt builder and output validator
- an import-mapping-suggestion-specific prompt builder and output validator
- a shared AI interaction execution support layer for feature gating, request-id normalization, failure mapping, and `ai_interaction_record` persistence across ticket and import workflows
- a shared provider-normalized structured-output client
- instance-level provider configuration under `merchantops.ai.*`
- an enable/disable flag plus provider-configuration guard
- timeout-based controlled degradation

The current provider-normalized client supports two protocol paths:

- `OPENAI`: `POST /v1/responses` with strict `json_schema`
- `DEEPSEEK`: `POST /chat/completions` with `response_format={type=json_object}` plus provider-aware JSON-only instructions and a minimal example JSON payload

Endpoint-specific output policy remains strict across both providers:

- summary requires `summary`
- triage requires `classification`, `priority`, and `reasoning`
- reply draft requires `opening`, `body`, `nextStep`, and `closing`
- import error summary requires `summary`, `topErrorPatterns`, and `recommendedNextSteps`
- import mapping suggestion requires `summary`, `suggestedFieldMappings`, `confidenceNotes`, and `recommendedOperatorChecks`
- request tests lock `Authorization`, `X-Client-Request-Id`, model id, system or user roles, and provider-specific structured-output wiring for both protocol paths
- OpenAI response parsing scans all `output[].content[]` parts, concatenates later `output_text` fragments in order, and ignores earlier non-text parts when valid text exists
- DeepSeek response parsing extracts the message content string, then applies the same endpoint-specific JSON validation and failure mapping
- upstream `408` and `504` HTTP responses are classified as provider timeouts so the timeout degradation path and `ai_interaction_record.status=PROVIDER_TIMEOUT` stay aligned
- provider-returned blank items in import `topErrorPatterns` or `recommendedNextSteps` stay mapped as `INVALID_RESPONSE`
- usage tokens and resolved `modelId` are normalized before the service records `ai_interaction_record`

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
- optional runtime metadata `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`, and `usageCostMicros`

Current status values include:

- `SUCCEEDED`
- `FEATURE_DISABLED`
- `PROVIDER_NOT_CONFIGURED`
- `PROVIDER_TIMEOUT`
- `PROVIDER_UNAVAILABLE`
- `INVALID_RESPONSE`

This record is still the governance-facing source of truth. The public ticket history endpoint exposes a narrowed read shape over the ticket rows, including runtime usage/cost metadata when available, while still not exposing raw prompt text or raw provider payloads and while remaining outside billing or ledger semantics. The import error-summary and mapping-suggestion slices persist the same record model as `entityType=IMPORT_JOB` with `interactionType=ERROR_SUMMARY` or `MAPPING_SUGGESTION`, but do not add a public import history read surface yet.

## Evaluation Baseline

The current public AI slices establish a minimal eval and visibility path:

- explicit prompt versioning through `ticket-summary-v1`, `ticket-triage-v1`, `ticket-reply-draft-v1`, `import-error-summary-v1`, and `import-mapping-suggestion-v1`
- golden-sample ticket and import inputs at `merchantops-api/src/test/resources/ai/ticket-summary/golden-samples.json`, `merchantops-api/src/test/resources/ai/ticket-triage/golden-samples.json`, `merchantops-api/src/test/resources/ai/ticket-reply-draft/golden-samples.json`, `merchantops-api/src/test/resources/ai/import-job-error-summary/golden-samples.json`, and `merchantops-api/src/test/resources/ai/import-job-mapping-suggestion/golden-samples.json`
- checked-in provider-response fixtures per workflow that drive the real provider parser and service path in automated golden tests
- focused automated tests for generation-endpoint happy path, permission failure, tenant isolation, symmetric degraded-mode coverage, provider-normalized request-contract assertions, endpoint-specific required-field validation, import prompt sanitization, import no-side-effect assertions, import `400` eligibility checks for no-failure and no-header-signal jobs, and ticket history-endpoint filter/sort/non-leakage coverage
- the local provider live smoke path in [../runbooks/ai-live-smoke-test.md](../runbooks/ai-live-smoke-test.md)
- the operational checklist in [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md)

## Failure And Safety Expectations

Current non-happy-path behavior:

- missing `TICKET_READ` remains `403`
- missing `USER_READ` remains `403` for import AI error summary and mapping suggestion
- cross-tenant or missing tickets remain `404`
- cross-tenant or missing import jobs remain `404`
- `GET /api/v1/tickets/{id}/ai-interactions` is read-only and does not create new interaction rows or mutate ticket workflow state
- disabled AI returns controlled `503 SERVICE_UNAVAILABLE` messages such as `ticket ai summary is disabled`, `ticket ai triage is disabled`, or `ticket ai reply draft is disabled`
- disabled import AI returns controlled `503 SERVICE_UNAVAILABLE` messages such as `import ai error summary is disabled` or `import ai mapping suggestion is disabled`
- missing provider configuration or provider failure returns controlled `503 SERVICE_UNAVAILABLE`
- mapping suggestion returns controlled `400 BAD_REQUEST` when the job has no failure signal or no sanitized header signal
- import AI prompt context never includes raw `itemErrors.rawPayload` text or raw `USER_CSV` password, email, username, display-name, or role-code values
- import AI error-summary and mapping-suggestion calls do not mutate `import_job`, `import_job_item_error`, replay lineage, approvals, or business `audit_event`
- raw provider exceptions are not exposed to API consumers
- the rest of the ticket workflow remains usable even when AI is unavailable
- the rest of the import workflow remains usable even when AI is unavailable

## Current Limitations

The current public AI slices are intentionally narrower than the Week 6 long-range plan.

Not implemented yet:
- any AI-driven ticket write-back flow
- approval-integrated AI execution
- public import AI history
- import fix recommendation
- tenant-level BYOK
- tenant billing, ledger, or invoice-style AI usage/cost reporting
- attachments or external-system context enrichment

## Planned Next Workflow Areas

The current public AI baseline should stay stable while the next new workflow slices move forward:

- Week 7 import fix recommendation
- future approval-aware write-back only after the suggestion-only slices are stable

Later roadmap areas remain:

- Week 8 agentic workflows with human oversight
- Week 9 broader AI governance, cost, and usage reporting

## Related Documents

- [ticket-workflow.md](ticket-workflow.md): current ticket endpoint surface and workflow details
- [authentication-and-rbac.md](authentication-and-rbac.md): auth and permission boundaries
- [ai-provider-configuration.md](ai-provider-configuration.md): active provider-key ownership and config keys
- [../runbooks/ai-live-smoke-test.md](../runbooks/ai-live-smoke-test.md): local provider live smoke path for `.env`-driven AI verification
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): rollout checklist for current and future AI endpoint changes
- [../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md](../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md): architecture decision for AI workflow placement and governance
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): minimum audit and eval baseline for public AI APIs
- [../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md](../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md): deployment-owned provider configuration baseline
- [../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md](../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md): separation rule for AI runtime traceability versus generic business audit
