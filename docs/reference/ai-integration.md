# AI Integration

Last updated: 2026-04-09

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
| `GET` | `/api/v1/ai-interactions/usage-summary` | `USER_READ` | Aggregate current-tenant AI runtime usage and cost metadata across stored ticket and import rows |
| `GET` | `/api/v1/import-jobs/{id}/ai-interactions` | `USER_READ` | Page operator-visible AI interaction history for one current-tenant import job |
| `POST` | `/api/v1/import-jobs/{id}/ai-error-summary` | `USER_READ` | Generate a suggestion-only error summary for one current-tenant import job |
| `POST` | `/api/v1/import-jobs/{id}/ai-mapping-suggestion` | `USER_READ` | Generate a suggestion-only canonical-field mapping proposal for one current-tenant import job |
| `POST` | `/api/v1/import-jobs/{id}/ai-fix-recommendation` | `USER_READ` | Generate a suggestion-only fix recommendation for one current-tenant import job from grounded row-level error groups |

Current public AI scope is intentionally narrow:

- nine public AI endpoints only: three read endpoints plus six suggestion-generating endpoints
- the generation endpoints use no request body; the server derives the prompt from current tenant-scoped ticket or import-job context
- the history endpoints support `page`, `size`, `interactionType`, and `status` query params over stored `ai_interaction_record` rows
- the tenant-scoped usage-summary endpoint supports optional `from`, `to`, `entityType`, `interactionType`, and `status` query params over the same stored `ai_interaction_record` rows
- no new public AI generation endpoint was added in Week 8; instead, the current workflow now includes two normal workflow endpoints outside the AI endpoint set:
  - `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, which can optionally reference a successful import `FIX_RECOMMENDATION` interaction as provenance
  - `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`, which accepts final `commentContent` plus optional same-ticket `REPLY_DRAFT` provenance
- the six generation endpoints now require both config-level `merchantops.ai.enabled=true` and their matching persisted feature flag:
  - `ai.ticket.summary.enabled`
  - `ai.ticket.triage.enabled`
  - `ai.ticket.reply-draft.enabled`
  - `ai.import.error-summary.enabled`
  - `ai.import.mapping-suggestion.enabled`
  - `ai.import.fix-recommendation.enabled`
- the three read endpoints are not part of that persisted flag set, so stored history and aggregate usage-summary visibility remain available when generation is gated off
- the two adjacent workflow endpoints above are gated separately by persisted workflow flags `workflow.import.selective-replay-proposal.enabled` and `workflow.ticket.comment-proposal.enabled`
- the usage-summary endpoint returns aggregate counts plus usage/cost totals and `byInteractionType`, `byStatus`, and `byPromptVersion` breakdowns only; it does not return a cross-entity per-request detail list
- no ticket status change, comment write, approval trigger, replay trigger, or other workflow mutation from the nine public AI endpoints themselves
- no public raw prompt or raw provider response in the response body, and no billing or ledger semantics on the history or tenant-summary responses

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

`POST /api/v1/import-jobs/{id}/ai-fix-recommendation` returns a minimal suggestion shape:

- `importJobId`
- `summary`
- `recommendedFixes`
- `confidenceNotes`
- `recommendedOperatorChecks`
- `promptVersion`
- `modelId`
- `generatedAt`
- `latencyMs`
- `requestId`

Each `recommendedFixes[]` record includes:

- `errorCode`
- `recommendedAction`
- `reasoning`
- `reviewRequired`
- `affectedRowsEstimate`

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "importJobId": 1201,
    "summary": "The import is mostly blocked by tenant role validation, with a smaller duplicate-username tail that should be handled as a separate cleanup step before replay.",
    "recommendedFixes": [
      {
        "errorCode": "UNKNOWN_ROLE",
        "recommendedAction": "Verify that the referenced role codes exist in the current tenant and normalize the source role-code format before preparing replay input.",
        "reasoning": "The grouped failures point to tenant role validation rather than CSV shape corruption.",
        "reviewRequired": true,
        "affectedRowsEstimate": 7
      },
      {
        "errorCode": "DUPLICATE_USERNAME",
        "recommendedAction": "Review the source usernames against current-tenant users and prepare unique replacements outside the AI response before replay.",
        "reasoning": "The grouped failures indicate a uniqueness conflict that needs an operator-reviewed edit.",
        "reviewRequired": true,
        "affectedRowsEstimate": 2
      }
    ],
    "confidenceNotes": [
      "The recommendations are grounded in row-level error groups, so operators should still confirm tenant-specific business rules before reuse."
    ],
    "recommendedOperatorChecks": [
      "Confirm which error-code group is the highest-volume cleanup target before editing replay input.",
      "Review the affected rows in /errors so value changes can be prepared outside the AI response."
    ],
    "promptVersion": "import-fix-recommendation-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-28T11:20:15",
    "latencyMs": 548,
    "requestId": "import-ai-fix-recommendation-req-1"
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

`GET /api/v1/import-jobs/{id}/ai-interactions` returns the same narrowed page shape over stored import AI history:

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

Current import history query behavior stays narrow:

- tenant scope is enforced by the existing import read path, so cross-tenant or missing jobs still return `404`
- filters are exact-match canonical values only, including `ERROR_SUMMARY`, `MAPPING_SUGGESTION`, and `FIX_RECOMMENDATION`
- ordering is stable `createdAt DESC, id DESC`
- runtime usage/cost fields are returned as `null` when a stored row is failed or otherwise unmetered
- raw prompt text, raw provider payload, replay helpers, and billing or ledger semantics are still excluded

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 9103,
        "interactionType": "FIX_RECOMMENDATION",
        "status": "INVALID_RESPONSE",
        "outputSummary": null,
        "promptVersion": "import-fix-recommendation-v1",
        "modelId": "gpt-4.1-mini",
        "latencyMs": 251,
        "requestId": "import-ai-fix-recommendation-invalid-response-1",
        "usagePromptTokens": null,
        "usageCompletionTokens": null,
        "usageTotalTokens": null,
        "usageCostMicros": null,
        "createdAt": "2026-03-28T10:45:00"
      },
      {
        "id": 9102,
        "interactionType": "MAPPING_SUGGESTION",
        "status": "SUCCEEDED",
        "outputSummary": "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.",
        "promptVersion": "import-mapping-suggestion-v1",
        "modelId": "gpt-4.1-mini",
        "latencyMs": 544,
        "requestId": "import-ai-mapping-suggestion-req-1",
        "usagePromptTokens": 141,
        "usageCompletionTokens": 71,
        "usageTotalTokens": 212,
        "usageCostMicros": null,
        "createdAt": "2026-03-28T10:45:00"
      }
    ],
    "page": 0,
    "size": 2,
    "total": 4,
    "totalPages": 2
  }
}
```

`GET /api/v1/ai-interactions/usage-summary` returns a narrowed tenant-scoped aggregate shape over stored AI runtime metadata:

- `from`
- `to`
- `totalInteractions`
- `succeededCount`
- `failedCount`
- `totalPromptTokens`
- `totalCompletionTokens`
- `totalTokens`
- `totalCostMicros`
- `byInteractionType[]`
- `byStatus[]`
- `byPromptVersion[]`

Each `byInteractionType[]` record includes:

- `interactionType`
- `count`
- `succeededCount`
- `failedCount`
- `totalTokens`
- `totalCostMicros`

Each `byStatus[]` record includes:

- `status`
- `count`
- `totalTokens`
- `totalCostMicros`

Each `byPromptVersion[]` record includes:

- `promptVersion`
- `count`
- `succeededCount`
- `failedCount`
- `totalTokens`
- `totalCostMicros`

Current usage-summary query behavior stays narrow:

- tenant scope is enforced directly on stored `ai_interaction_record` rows, so no cross-tenant aggregate leakage is allowed
- `from` and `to` are optional inclusive ISO-8601 `LocalDateTime` filters; `from > to` returns `400`
- `entityType` is optional and limited to exact-match trimmed `TICKET` or `IMPORT_JOB`
- `interactionType` and `status` are optional exact-match trimmed filters over stored canonical values such as `SUMMARY`, `ERROR_SUMMARY`, `SUCCEEDED`, and `INVALID_RESPONSE`
- `failedCount` is every non-`SUCCEEDED` row, while `succeededCount` is `status=SUCCEEDED`
- `byInteractionType[]` ordering is `count DESC, interactionType ASC`
- `byStatus[]` ordering is `count DESC, status ASC`
- `byPromptVersion[]` ordering is `count DESC, promptVersion ASC`
- null token and cost fields still count toward interaction totals but contribute zero to aggregate sums
- request-level fields such as `requestId`, `outputSummary`, and `modelId` are intentionally excluded; `promptVersion` is exposed only through aggregate `byPromptVersion[]` buckets
- raw prompt text, raw provider payload, cross-entity per-request detail listing, and billing or ledger semantics are still excluded

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "from": "2026-04-01T00:00:00",
    "to": "2026-04-05T23:59:59",
    "totalInteractions": 6,
    "succeededCount": 4,
    "failedCount": 2,
    "totalPromptTokens": 530,
    "totalCompletionTokens": 263,
    "totalTokens": 793,
    "totalCostMicros": 6200,
    "byInteractionType": [
      {
        "interactionType": "ERROR_SUMMARY",
        "count": 2,
        "succeededCount": 1,
        "failedCount": 1,
        "totalTokens": 232,
        "totalCostMicros": 0
      },
      {
        "interactionType": "MAPPING_SUGGESTION",
        "count": 1,
        "succeededCount": 1,
        "failedCount": 0,
        "totalTokens": 161,
        "totalCostMicros": 2200
      },
      {
        "interactionType": "REPLY_DRAFT",
        "count": 1,
        "succeededCount": 1,
        "failedCount": 0,
        "totalTokens": 228,
        "totalCostMicros": 2100
      },
      {
        "interactionType": "SUMMARY",
        "count": 1,
        "succeededCount": 1,
        "failedCount": 0,
        "totalTokens": 172,
        "totalCostMicros": 1900
      },
      {
        "interactionType": "TRIAGE",
        "count": 1,
        "succeededCount": 0,
        "failedCount": 1,
        "totalTokens": 0,
        "totalCostMicros": 0
      }
    ],
    "byStatus": [
      {
        "status": "SUCCEEDED",
        "count": 4,
        "totalTokens": 773,
        "totalCostMicros": 6200
      },
      {
        "status": "INVALID_RESPONSE",
        "count": 1,
        "totalTokens": 0,
        "totalCostMicros": 0
      },
      {
        "status": "PROVIDER_TIMEOUT",
        "count": 1,
        "totalTokens": 20,
        "totalCostMicros": 0
      }
    ],
    "byPromptVersion": [
      {
        "promptVersion": "import-error-summary-v1",
        "count": 2,
        "succeededCount": 1,
        "failedCount": 1,
        "totalTokens": 232,
        "totalCostMicros": 0
      },
      {
        "promptVersion": "import-mapping-suggestion-v1",
        "count": 1,
        "succeededCount": 1,
        "failedCount": 0,
        "totalTokens": 161,
        "totalCostMicros": 2200
      },
      {
        "promptVersion": "ticket-reply-draft-v1",
        "count": 1,
        "succeededCount": 1,
        "failedCount": 0,
        "totalTokens": 228,
        "totalCostMicros": 2100
      },
      {
        "promptVersion": "ticket-summary-v1",
        "count": 1,
        "succeededCount": 1,
        "failedCount": 0,
        "totalTokens": 172,
        "totalCostMicros": 1900
      },
      {
        "promptVersion": "ticket-triage-v1",
        "count": 1,
        "succeededCount": 0,
        "failedCount": 1,
        "totalTokens": 0,
        "totalCostMicros": 0
      }
    ]
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

The current import error-summary, mapping-suggestion, and fix-recommendation prompts are built only from the target import job in the current tenant:

- import job core fields and counters from `ImportJobDetail`
- aggregated `errorCodeCounts`
- sanitized header/global parse-error signal derived from existing `rowNumber=null` item errors when present
- the first 20 failure rows from `GET /errors`-equivalent query ordering
- one local structural summary per failed row containing only presence/count signals such as `columnCount`, `usernamePresent`, `displayNamePresent`, `emailPresent`, `passwordPresent`, `roleCodesPresent`, and `roleCodeCount`

Current import prompt shaping stays intentionally narrow:

- the server sanitizes raw failed-row CSV locally before prompt assembly and never forwards raw `itemErrors.rawPayload`
- the mapping-suggestion slice forwards only normalized header names, header positions, header-column count, and bounded structural row summaries; it does not rescan the source file or infer from raw row values
- the fix-recommendation slice forwards only grounded row-level `errorCode` groups, bounded sample error messages, bounded sample rows, and structural row summaries; it does not send raw row values or direct replacement values
- the prompt includes `rowNumber`, `errorCode`, and `errorMessage`, but not raw username, displayName, email, password, or role-code text
- if raw payload parsing fails, the prompt keeps only structural fallback metadata rather than sending the original row text
- fix recommendation performs an extra local output-policy check after provider return and rejects responses that echo raw CSV-like strings or sensitive local row values as `INVALID_RESPONSE`

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
- an import-fix-recommendation-specific prompt builder and output validator
- a shared AI interaction execution support layer for feature gating, request-id normalization, failure mapping, and `ai_interaction_record` persistence across ticket and import workflows
- a shared provider-normalized structured-output client
- instance-level provider configuration under `merchantops.ai.*`
- a config-level `merchantops.ai.enabled` master switch plus one persisted tenant-scoped feature flag per generation endpoint
- provider-configuration guard
- timeout-based controlled degradation

The current provider-normalized client supports three internal transport paths across two providers:

- `OPENAI + RAW_HTTP`: direct `POST /v1/responses` with strict `json_schema`
- `OPENAI + SPRING_AI`: Spring AI OpenAI chat-completions transport to `POST /v1/chat/completions` with `response_format=json_schema`
- `DEEPSEEK`: `POST /chat/completions` with `response_format={type=json_object}` plus provider-aware JSON-only instructions and a minimal example JSON payload

OpenAI transport selection stays internal behind `StructuredOutputAiClient`:

- `merchantops.ai.openai-runtime=RAW_HTTP` is the default rollback-safe path
- `merchantops.ai.openai-runtime=SPRING_AI` changes only the OpenAI transport layer
- the six workflow providers, prompt builders, governance checks, persisted feature flags, history records, and public response contracts stay unchanged
- runtime ownership remains under `merchantops.ai.*`; this slice does not introduce `spring.ai.*` as a second configuration source

Endpoint-specific output policy remains strict across both providers:

- summary requires `summary`
- triage requires `classification`, `priority`, and `reasoning`
- reply draft requires `opening`, `body`, `nextStep`, and `closing`
- import error summary requires `summary`, `topErrorPatterns`, and `recommendedNextSteps`
- import mapping suggestion requires `summary`, `suggestedFieldMappings`, `confidenceNotes`, and `recommendedOperatorChecks`
- import fix recommendation requires `summary`, `recommendedFixes`, `confidenceNotes`, and `recommendedOperatorChecks`
- request tests lock `Authorization`, `X-Client-Request-Id`, model id, system or user roles, and provider-specific structured-output wiring for both OpenAI transport variants plus the DeepSeek path
- OpenAI response parsing scans all `output[].content[]` parts, concatenates later `output_text` fragments in order, and ignores earlier non-text parts when valid text exists
- Spring AI OpenAI response parsing extracts the first assistant text from `ChatResponse`, then applies the same workflow-local JSON validation and failure mapping as the existing OpenAI provider path
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

This record is still the governance-facing source of truth. The public ticket and import history endpoints expose the same narrowed read shape over their stored rows, including runtime usage/cost metadata when available, while the tenant-scoped usage-summary endpoint exposes an aggregate read over the same stored rows. All three read surfaces still exclude raw prompt text and raw provider payloads and still remain outside billing or ledger semantics. The import history endpoint and tenant usage-summary endpoint both read the same `entityType=IMPORT_JOB` rows already written by the import error-summary, mapping-suggestion, and fix-recommendation slices.

## Evaluation Baseline

The current public AI slices establish a minimal eval and visibility path:

- explicit prompt versioning through `ticket-summary-v1`, `ticket-triage-v1`, `ticket-reply-draft-v1`, `import-error-summary-v1`, `import-mapping-suggestion-v1`, and `import-fix-recommendation-v1`
- a shared executable workflow catalog in `merchantops-api/src/main/java/com/renda/merchantops/api/ai/core/AiGenerationWorkflow.java` plus an intentionally paired but independently maintained eval inventory in `merchantops-api/src/test/java/com/renda/merchantops/api/ai/eval/AiWorkflowEvalInventory.java`, so unreviewed prompt-version bumps fail the governance baseline until both sides are updated on purpose
- checked-in golden, failure, and policy datasets under `merchantops-api/src/test/resources/ai/<workflow>/`
- checked-in provider-response fixtures per workflow that drive the real provider parser and service path in automated golden tests
- a shared comparator pass in `merchantops-api/src/test/java/com/renda/merchantops/api/ai/eval/AiWorkflowEvalComparatorTest.java` that reports checked workflows, prompt versions, sample counts, and failing assertions
- thin per-workflow golden tests that now reuse the shared evaluator logic instead of maintaining independent assertion paths
- focused automated tests for generation-endpoint happy path, permission failure, tenant isolation, symmetric degraded-mode coverage, provider-normalized request-contract assertions, endpoint-specific required-field validation, import prompt sanitization, import no-side-effect assertions, import `400` eligibility checks for no-failure, unsupported-import-type, no-header-signal, and no-row-signal jobs, sensitive-output rejection for fix recommendation, and ticket plus import history-endpoint filter/sort/non-leakage coverage including import read-after-write visibility after real generation calls
- the local provider live smoke path in [../runbooks/ai-live-smoke-test.md](../runbooks/ai-live-smoke-test.md)
- the operational checklist in [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md)

## Failure And Safety Expectations

Current non-happy-path behavior:

- missing `TICKET_READ` remains `403`
- missing `USER_READ` remains `403` for import AI error summary, mapping suggestion, and fix recommendation
- missing `USER_READ` remains `403` for `GET /api/v1/ai-interactions/usage-summary`
- cross-tenant or missing tickets remain `404`
- cross-tenant or missing import jobs remain `404`
- `GET /api/v1/tickets/{id}/ai-interactions` is read-only and does not create new interaction rows or mutate ticket workflow state
- `GET /api/v1/import-jobs/{id}/ai-interactions` is read-only and does not create new interaction rows or mutate import job state, import error rows, replay lineage, approvals, or business audit state
- `GET /api/v1/ai-interactions/usage-summary` is read-only and does not create new interaction rows or mutate ticket state, import state, approvals, or business audit state
- `GET /api/v1/ai-interactions/usage-summary` returns `400 BAD_REQUEST` when `from > to` or when `entityType` is outside `TICKET` and `IMPORT_JOB`
- `POST /api/v1/tickets/{id}/ai-reply-draft` remains read-only even though the current workflow now includes a separate approval-backed ticket comment proposal endpoint outside the AI endpoint set
- `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` remain read-only even though the current workflow now includes a separate approval-backed selective replay proposal path outside the AI endpoint set
- disabled AI generation returns controlled `503 SERVICE_UNAVAILABLE` messages such as `ticket ai summary is disabled`, `ticket ai triage is disabled`, or `ticket ai reply draft is disabled` whether the gate is closed by `merchantops.ai.enabled=false` or by the matching persisted feature flag
- disabled import AI generation returns controlled `503 SERVICE_UNAVAILABLE` messages such as `import ai error summary is disabled`, `import ai mapping suggestion is disabled`, or `import ai fix recommendation is disabled` whether the gate is closed by `merchantops.ai.enabled=false` or by the matching persisted feature flag
- missing provider configuration or provider failure returns controlled `503 SERVICE_UNAVAILABLE`
- mapping suggestion returns controlled `400 BAD_REQUEST` when the job has no failure signal or no sanitized header signal
- fix recommendation returns controlled `400 BAD_REQUEST` when the job has no failure signal, is not `USER_CSV`, or has no sanitized row-level signal
- import AI prompt context never includes raw `itemErrors.rawPayload` text or raw `USER_CSV` password, email, username, display-name, or role-code values
- import AI error-summary, mapping-suggestion, and fix-recommendation calls do not mutate `import_job`, `import_job_item_error`, replay lineage, approvals, or business `audit_event`
- raw provider exceptions are not exposed to API consumers
- the rest of the ticket workflow remains usable even when AI is unavailable
- the rest of the import workflow remains usable even when AI is unavailable
- the separate workflow proposal bridges remain independently controllable through `workflow.import.selective-replay-proposal.enabled` and `workflow.ticket.comment-proposal.enabled` without widening the nine public AI endpoints

## Current Limitations

The current public AI slices are intentionally narrower than the Week 6 long-range plan.

Not implemented yet:
- any direct AI-driven ticket or import write-back from the nine public AI endpoints themselves
- any AI endpoint that directly executes approval or replay
- broader approval-integrated execution beyond the current Week 8 import selective replay and ticket comment proposal bridges
- tenant-level BYOK
- broader tenant-scoped runtime reporting beyond the current aggregate usage summary, such as per-request cross-entity listings or trend-oriented reporting
- tenant billing, ledger, or invoice-style AI usage/cost semantics
- attachments or external-system context enrichment

## Planned Next Workflow Areas

The current public AI baseline should stay stable while Week 10 delivery hardening lands on top of the completed Week 9 governance and evaluation layer:

- keep the completed Week 6 ticket AI read surface, the completed Week 7 import AI read surface, and the completed Week 9 tenant usage-summary read surface stable instead of widening them into broader reporting too quickly
- treat the completed Week 8 import selective replay proposal flow and the completed Week 8 ticket comment proposal flow as the first two human-reviewed execution bridges from suggestion-only AI guidance into approval-bounded workflow execution
- treat the completed Week 9 Slice A executable eval baseline plus the completed Week 9 Slice B plus Slice C tenant-scoped usage-summary read as the current governance visibility baseline on top of stored `ai_interaction_record` rows
- treat the completed Week 10 Slice A persisted feature-flag baseline as the current rollout-safety control for the six generation endpoints and the two workflow bridges
- future approval-aware write-back should build on those separate proposal/approval/execution patterns instead of bypassing them or widening the nine public AI endpoints directly

Later roadmap areas remain:

- broader tenant-scoped AI governance and reporting beyond the current aggregate usage summary, still without billing or ledger semantics
- later human-reviewed workflow expansion only after the current Week 8 execution bridges and approval hardening baseline are proven stable

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
