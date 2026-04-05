# AI Regression Checklist

Last updated: 2026-04-05

> Maintenance note: keep this page focused on AI-specific safety, audit, eval, and provider behavior. Do not duplicate the normal non-AI API sign-off items from [regression-checklist.md](regression-checklist.md); link there when a change spans both the AI slice and the broader public business surface.

Use this checklist when any of the following change:

- prompt templates
- model selection
- provider adapter logic
- AI feature gating or timeout behavior
- AI-related logging, usage, or error handling
- the public AI interaction-history, ticket summary, ticket triage, ticket reply-draft, or import AI error-summary / mapping-suggestion / fix-recommendation response shape or Swagger examples

## Current Public Boundary

The AI checklist is now active because public AI endpoints exist:

- `POST /api/v1/tickets/{id}/ai-summary`
- `POST /api/v1/tickets/{id}/ai-triage`
- `POST /api/v1/tickets/{id}/ai-reply-draft`
- `POST /api/v1/import-jobs/{id}/ai-error-summary`
- `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`
- `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`
- `GET /api/v1/tickets/{id}/ai-interactions`
- `GET /api/v1/import-jobs/{id}/ai-interactions`
- suggestion-only ticket summary, triage, internal reply-draft, and import error-summary plus mapping-suggestion plus fix-recommendation results plus narrowed interaction-history visibility for one current-tenant ticket or import job
- read permission inherited from `TICKET_READ`
- import error summary, mapping suggestion, and fix recommendation inherit import read permission from `USER_READ`
- no write-back, no comment creation, and no approval execution from the eight public AI endpoints themselves
- adjacent Week 8 workflow endpoints now exist outside the AI endpoint set:
  - `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`
  - `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`
- those workflow endpoints may reference successful AI interactions as provenance, but the public AI endpoints themselves remain suggestion-only
- interaction-history reads expose stored `outputSummary`, `promptVersion`, `modelId`, `latencyMs`, `requestId`, `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`, `usageCostMicros`, and `createdAt`, but do not expose raw prompt or raw provider payload and do not establish billing semantics

## Environment And Control

- [ ] provider credentials and model configuration are loaded from the expected `merchantops.ai.*` keys or the documented local `.env` aliases
- [ ] AI features can be disabled cleanly via `merchantops.ai.enabled`
- [ ] degraded mode behavior is verified when the provider is unavailable or not configured
- [ ] request timeout behavior is explicit and covered by a simulated timeout path

## Live Provider Smoke

- [ ] local `.env` is prepared through [ai-live-smoke-test.md](ai-live-smoke-test.md) with provider-neutral `MERCHANTOPS_AI_*` keys or the documented DeepSeek aliases
- [ ] the first real provider probe is one `POST /api/v1/tickets/{id}/ai-summary` call against a fresh ticket with enough context
- [ ] if the live summary fails, `ai-triage` and `ai-reply-draft` are not called in the same session
- [ ] after a successful live summary, `GET /api/v1/tickets/{id}/ai-interactions` confirms a matching `SUMMARY` row with the expected `requestId`, `status`, and `modelId`
- [ ] after summary succeeds, the approved second-stage live path is `ai-triage` plus `GET /ai-interactions?interactionType=TRIAGE`, then `ai-reply-draft` plus `GET /ai-interactions?interactionType=REPLY_DRAFT`
- [ ] if the change affects import AI or shared provider/runtime behavior, one live `POST /api/v1/import-jobs/{id}/ai-error-summary` call and, when eligible signal exists, one live `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` call plus one live `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` call are run against known failed import jobs after the ticket summary-first path is proven locally
- [ ] a successful live import error summary returns non-blank `summary`, non-empty `topErrorPatterns`, non-empty `recommendedNextSteps`, and the expected `requestId`
- [ ] a successful live import mapping suggestion returns non-blank `summary`, exactly five canonical `suggestedFieldMappings`, non-empty `confidenceNotes`, non-empty `recommendedOperatorChecks`, and the expected `requestId`
- [ ] a successful live import fix recommendation returns non-blank `summary`, non-empty `recommendedFixes`, non-empty `confidenceNotes`, non-empty `recommendedOperatorChecks`, grounded `errorCode` values only, and the expected `requestId`
- [ ] after each successful live import AI generation call, `GET /api/v1/import-jobs/{id}/ai-interactions` confirms the matching `interactionType`, `requestId`, `status`, and `modelId`
- [ ] a successful live triage returns non-blank `classification`, valid `priority`, non-blank `reasoning`, and a matching `TRIAGE/SUCCEEDED` history row
- [ ] a successful live reply draft returns non-blank `opening`, `body`, `nextStep`, `closing`, `draftText`, and a matching `REPLY_DRAFT/SUCCEEDED` history row
- [ ] if triage fails in the second-stage live pass, reply-draft is not called in the same session

## Generation Endpoint Checks

- [ ] `POST /api/v1/tickets/{id}/ai-summary` covers feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response as controlled `503` paths with specific `ai_interaction_record.status`
- [ ] `POST /api/v1/tickets/{id}/ai-triage` covers feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response as controlled `503` paths with specific `ai_interaction_record.status`
- [ ] `POST /api/v1/tickets/{id}/ai-reply-draft` covers feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response as controlled `503` paths with specific `ai_interaction_record.status`
- [ ] `POST /api/v1/import-jobs/{id}/ai-error-summary` covers feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response as controlled `503` paths with specific `ai_interaction_record.status`
- [ ] `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` covers `400` no-failure-signal, `400` no-sanitized-header-signal, feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response with specific `ai_interaction_record.status` only on the `503` paths
- [ ] `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` covers `400` no-failure-signal, `400` unsupported-import-type, `400` no-sanitized-row-signal, feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response with specific `ai_interaction_record.status` only on the `503` paths
- [ ] summary, triage, reply-draft, import error-summary, and import mapping-suggestion adapter tests all cover request-contract assertions, unsupported content, refusal, invalid JSON payload handling, later-part `output_text` parsing, and `408` or `504` timeout classification
- [ ] summary adapter rejects missing `summary`; triage adapter rejects missing `classification`, missing `reasoning`, missing `priority`, and invalid `priority`
- [ ] reply-draft adapter rejects missing `opening`, missing `body`, missing `nextStep`, and missing `closing`
- [ ] import error-summary adapter rejects missing `summary`, missing `topErrorPatterns`, missing `recommendedNextSteps`, and blank array items
- [ ] import mapping-suggestion adapter rejects missing `summary`, missing or empty `suggestedFieldMappings`, blank mapping fields, and invalid `headerPosition`
- [ ] import fix-recommendation adapter rejects missing `summary`, missing or empty `recommendedFixes`, blank recommendation fields, and invalid `reviewRequired`

## Tenant And Permission Safety

- [ ] AI input data is limited to the current tenant
- [ ] AI requests do not combine records across tenants
- [ ] permission failures still return normal application errors such as `403`
- [ ] cross-tenant or missing tickets still return normal business `404`
- [ ] cross-tenant or missing import jobs still return normal business `404`
- [ ] the interaction-history, summary, triage, and reply-draft endpoints do not bypass the existing ticket read boundary
- [ ] the import error-summary, mapping-suggestion, and fix-recommendation endpoints do not bypass the existing import read boundary

## Audit And Traceability

- [ ] AI requests are linked to `tenantId`, `userId`, and `requestId`
- [ ] `promptVersion` is captured
- [ ] `modelId` is captured
- [ ] `status` is captured in `ai_interaction_record`
- [ ] latency is recorded
- [ ] usage or cost metrics are recorded when available from the provider
- [ ] raw provider errors are not leaked directly to API consumers
- [ ] read-only AI calls are recorded without pretending they are normal business write `audit_event` rows
- [ ] import error-summary persistence uses `entityType=IMPORT_JOB` and `interactionType=ERROR_SUMMARY`
- [ ] import mapping-suggestion persistence uses `entityType=IMPORT_JOB` and `interactionType=MAPPING_SUGGESTION`
- [ ] import fix-recommendation persistence uses `entityType=IMPORT_JOB` and `interactionType=FIX_RECOMMENDATION`

## Interaction History Read Surface

- [ ] `GET /api/v1/tickets/{id}/ai-interactions` returns `403` when `TICKET_READ` is missing
- [ ] `GET /api/v1/tickets/{id}/ai-interactions` returns `404` for cross-tenant or missing tickets
- [ ] `GET /api/v1/import-jobs/{id}/ai-interactions` returns `403` when `USER_READ` is missing
- [ ] `GET /api/v1/import-jobs/{id}/ai-interactions` returns `404` for cross-tenant or missing import jobs
- [ ] `interactionType` filtering is exact-match on stored canonical values such as `SUMMARY`, `TRIAGE`, and `REPLY_DRAFT`
- [ ] import-history `interactionType` filtering is exact-match on stored canonical values such as `ERROR_SUMMARY`, `MAPPING_SUGGESTION`, and `FIX_RECOMMENDATION`
- [ ] `status` filtering is exact-match on stored canonical values such as `SUCCEEDED`, `FEATURE_DISABLED`, `PROVIDER_NOT_CONFIGURED`, `PROVIDER_TIMEOUT`, `PROVIDER_UNAVAILABLE`, and `INVALID_RESPONSE`
- [ ] history results are ordered by `createdAt DESC, id DESC`, including stable same-timestamp tie breaks
- [ ] history responses stay paged and return `id`, `interactionType`, `status`, `outputSummary`, `promptVersion`, `modelId`, `latencyMs`, `requestId`, `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`, `usageCostMicros`, and `createdAt`
- [ ] history responses expose usage/cost values only as runtime metadata, return `null` when unavailable, and do not leak raw prompt text or raw provider payload
- [ ] history reads do not create new `ai_interaction_record` rows or mutate ticket workflow state, approvals, or business `audit_event`
- [ ] import history reads do not create new `ai_interaction_record` rows or mutate import job state, import error rows, replay lineage, approvals, or business `audit_event`

## Output Quality

- [ ] golden ticket-summary samples still produce the expected stable shape
- [ ] golden ticket-triage samples still produce the expected stable shape
- [ ] golden ticket-reply-draft samples still produce the expected stable shape
- [ ] golden import error-summary samples still produce the expected stable shape
- [ ] golden import mapping-suggestion samples still produce the expected stable shape
- [ ] golden import fix-recommendation samples still produce the expected stable shape
- [ ] the shared comparator pass in `merchantops-api/src/test/java/com/renda/merchantops/api/ai/eval/AiWorkflowEvalComparatorTest.java` stays green in the default Maven suite
- [ ] the comparator inventory stays aligned with the six active generation workflows and their prompt versions
- [ ] the failure and policy sample baselines still cover invalid-response cases, ambiguous or noisy ticket cases, import no-failure or no-header or no-row eligibility failures where relevant, and grounded or sensitive-output import guardrails
- [ ] golden tests use checked-in provider-response fixtures and the real provider/service parsing path rather than echoing expected output fields back from the sample file
- [ ] summary output still includes issue, current state, latest meaningful signal, and next human follow-up
- [ ] summary output remains non-blank and does not expose raw prompt text or raw provider payload
- [ ] triage output still includes non-blank `classification`, `priority`, and concise `reasoning`
- [ ] triage `priority` remains restricted to `LOW`, `MEDIUM`, or `HIGH`, and the slice stays suggestion-only without assignee suggestions
- [ ] reply-draft output still includes `opening`, `body`, `nextStep`, `closing`, and server-assembled `draftText`
- [ ] the public summary response shape remains stable for `ticketId`, `summary`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public triage response shape remains stable for `ticketId`, `classification`, `priority`, `reasoning`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public reply-draft response shape remains stable for `ticketId`, `draftText`, `opening`, `body`, `nextStep`, `closing`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public import error-summary response shape remains stable for `importJobId`, `summary`, `topErrorPatterns`, `recommendedNextSteps`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public import mapping-suggestion response shape remains stable for `importJobId`, `summary`, `suggestedFieldMappings`, `confidenceNotes`, `recommendedOperatorChecks`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public import fix-recommendation response shape remains stable for `importJobId`, `summary`, `recommendedFixes`, `confidenceNotes`, `recommendedOperatorChecks`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public interaction-history response shape remains stable for `items`, `page`, `size`, `total`, `totalPages`, and item fields `id`, `interactionType`, `status`, `outputSummary`, `promptVersion`, `modelId`, `latencyMs`, `requestId`, `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`, `usageCostMicros`, and `createdAt`
- [ ] the reply-draft `draftText` still equals `opening + "\n\n" + body + "\n\nNext step: " + nextStep + "\n\n" + closing`
- [ ] assembled reply drafts still fit the current ticket comment length limit
- [ ] the summary, triage, and reply-draft slices stay suggestion-only and do not imply unsupported automatic execution
- [ ] prompt or model changes are reviewed against both happy-path and known-risk samples when those samples exist
- [ ] AI prompt context stays capped to the recent ticket history window and marks omitted older history when the window truncates
- [ ] import AI prompt context stays capped to the allowed sanitized import context, does not include raw `itemErrors.rawPayload`, raw header lines, raw username, raw email, raw password, or raw role-code values, and does not rescan the source file
- [ ] import fix recommendation stays grounded in local row-level `errorCode` groups and rejects provider output that echoes raw CSV-like strings or sensitive row values

## Workflow Safety

- [ ] AI summary, triage, and reply-draft calls do not mutate ticket status, assignee, comments, workflow logs, or approvals
- [ ] `POST /api/v1/tickets/{id}/ai-reply-draft` does not create a ticket comment or approval request directly even though a separate ticket comment proposal endpoint now exists outside the AI endpoint set
- [ ] import AI error-summary calls do not mutate `import_job`, `import_job_item_error`, replay lineage, approvals, or business `audit_event`
- [ ] import AI mapping-suggestion calls do not mutate `import_job`, `import_job_item_error`, replay lineage, approvals, or business `audit_event`
- [ ] import AI fix-recommendation calls do not mutate `import_job`, `import_job_item_error`, replay lineage, approvals, or business `audit_event`
- [ ] import AI fix-recommendation does not create a selective replay proposal directly even though a separate proposal endpoint now exists outside the AI endpoint set
- [ ] AI summary, triage, and reply-draft calls do not create business `audit_event` rows
- [ ] AI interaction-history reads do not mutate ticket status, assignee, comments, workflow logs, approvals, or business `audit_event` rows
- [ ] import AI interaction-history reads do not mutate import job state, import error rows, replay lineage, approvals, or business `audit_event` rows
- [ ] operators can continue the ticket workflow manually when AI is unavailable
- [ ] ticket detail, ticket workflow log, and business audit behavior remain unchanged by AI summary or triage calls
- [ ] import detail, import error pages, and replay behavior remain unchanged by import AI error-summary calls

## API And Documentation Alignment

- [ ] the public AI endpoints appear in Swagger
- [ ] Swagger examples match real request and response shapes
- [ ] AI reference docs are updated in [../reference/ai-integration.md](../reference/ai-integration.md)
- [ ] import AI reference docs are updated in [../reference/import-jobs.md](../reference/import-jobs.md)
- [ ] ticket workflow docs are updated in [../reference/ticket-workflow.md](../reference/ticket-workflow.md)
- [ ] approval and governance docs are updated in [../reference/audit-approval.md](../reference/audit-approval.md) when a workflow-adjacent AI proposal bridge changes
- [ ] request examples are updated in `api-demo.http`
- [ ] provider configuration docs stay aligned with active runtime keys
- [ ] architecture notes stay aligned with the governing ADRs

## Suggested Minimal Test Pass

For the current public AI slices, at minimum run:

1. one authorized happy-path request for each affected AI endpoint with a `TICKET_READ` user
2. one permission-denied request
3. one tenant-isolation or not-found check
4. one feature-disabled check
5. one provider-not-configured or provider-unavailable simulation
6. one provider-timeout simulation
7. one invalid-response simulation
8. one prompt-context non-leakage or sensitive-output-rejection check when the affected workflow includes import AI error summary, mapping suggestion, or fix recommendation
9. one interaction-history filter, ordering, and non-leakage check when the history surface is affected
10. one shared comparator regression pass plus one golden-sample regression check for each affected AI generation workflow when the comparator itself is not the only changed surface
11. when live provider wiring changed, one local summary-first provider smoke plus the matching interaction-history read through [ai-live-smoke-test.md](ai-live-smoke-test.md)
12. after summary succeeds and the change still needs real-vendor verification, one second-stage live triage pass plus one second-stage live reply-draft pass, each followed by the matching interaction-history read, plus one import error-summary live pass and, when eligible, one import mapping-suggestion live pass plus one import fix-recommendation live pass, each followed by the matching import interaction-history read, when the change touches import AI or shared AI runtime behavior

## Related Documents

- [../reference/ai-integration.md](../reference/ai-integration.md): current AI workflow guardrails and public contract
- [../reference/ai-provider-configuration.md](../reference/ai-provider-configuration.md): active provider configuration model and keys
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): AI audit and eval baseline decision
- [../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md](../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md): separation rule for AI runtime records
