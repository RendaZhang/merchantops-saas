# AI Regression Checklist

Last updated: 2026-03-22

> Maintenance note: keep this page focused on AI-specific safety, audit, eval, and provider behavior. Do not duplicate the normal non-AI API sign-off items from [regression-checklist.md](regression-checklist.md); link there when a change spans both the AI slice and the broader public business surface.

Use this checklist when any of the following change:

- prompt templates
- model selection
- provider adapter logic
- AI feature gating or timeout behavior
- AI-related logging, usage, or error handling
- the public AI interaction-history, ticket summary, ticket triage, or ticket reply-draft response shape or Swagger examples

## Current Public Boundary

The AI checklist is now active because public AI endpoints exist:

- `POST /api/v1/tickets/{id}/ai-summary`
- `POST /api/v1/tickets/{id}/ai-triage`
- `POST /api/v1/tickets/{id}/ai-reply-draft`
- `GET /api/v1/tickets/{id}/ai-interactions`
- suggestion-only summary, triage, internal reply-draft results, and narrowed interaction-history visibility for one current-tenant ticket
- read permission inherited from `TICKET_READ`
- no write-back, no comment creation, and no approval execution in the current slice
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
- [ ] a successful live triage returns non-blank `classification`, valid `priority`, non-blank `reasoning`, and a matching `TRIAGE/SUCCEEDED` history row
- [ ] a successful live reply draft returns non-blank `opening`, `body`, `nextStep`, `closing`, `draftText`, and a matching `REPLY_DRAFT/SUCCEEDED` history row
- [ ] if triage fails in the second-stage live pass, reply-draft is not called in the same session

## Three-Endpoint Symmetry Checks

- [ ] `POST /api/v1/tickets/{id}/ai-summary` covers feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response as controlled `503` paths with specific `ai_interaction_record.status`
- [ ] `POST /api/v1/tickets/{id}/ai-triage` covers feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response as controlled `503` paths with specific `ai_interaction_record.status`
- [ ] `POST /api/v1/tickets/{id}/ai-reply-draft` covers feature disabled, provider not configured, provider unavailable, provider timeout, and invalid response as controlled `503` paths with specific `ai_interaction_record.status`
- [ ] summary, triage, and reply-draft adapter tests all cover request-contract assertions, unsupported content, refusal, invalid JSON payload handling, later-part `output_text` parsing, and `408` or `504` timeout classification
- [ ] summary adapter rejects missing `summary`; triage adapter rejects missing `classification`, missing `reasoning`, missing `priority`, and invalid `priority`
- [ ] reply-draft adapter rejects missing `opening`, missing `body`, missing `nextStep`, and missing `closing`

## Tenant And Permission Safety

- [ ] AI input data is limited to the current tenant
- [ ] AI requests do not combine records across tenants
- [ ] permission failures still return normal application errors such as `403`
- [ ] cross-tenant or missing tickets still return normal business `404`
- [ ] the interaction-history, summary, triage, and reply-draft endpoints do not bypass the existing ticket read boundary

## Audit And Traceability

- [ ] AI requests are linked to `tenantId`, `userId`, and `requestId`
- [ ] `promptVersion` is captured
- [ ] `modelId` is captured
- [ ] `status` is captured in `ai_interaction_record`
- [ ] latency is recorded
- [ ] usage or cost metrics are recorded when available from the provider
- [ ] raw provider errors are not leaked directly to API consumers
- [ ] read-only AI calls are recorded without pretending they are normal business write `audit_event` rows

## Interaction History Read Surface

- [ ] `GET /api/v1/tickets/{id}/ai-interactions` returns `403` when `TICKET_READ` is missing
- [ ] `GET /api/v1/tickets/{id}/ai-interactions` returns `404` for cross-tenant or missing tickets
- [ ] `interactionType` filtering is exact-match on stored canonical values such as `SUMMARY`, `TRIAGE`, and `REPLY_DRAFT`
- [ ] `status` filtering is exact-match on stored canonical values such as `SUCCEEDED`, `FEATURE_DISABLED`, `PROVIDER_NOT_CONFIGURED`, `PROVIDER_TIMEOUT`, `PROVIDER_UNAVAILABLE`, and `INVALID_RESPONSE`
- [ ] history results are ordered by `createdAt DESC, id DESC`, including stable same-timestamp tie breaks
- [ ] history responses stay paged and return `id`, `interactionType`, `status`, `outputSummary`, `promptVersion`, `modelId`, `latencyMs`, `requestId`, `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`, `usageCostMicros`, and `createdAt`
- [ ] history responses expose usage/cost values only as ticket-scoped runtime metadata, return `null` when unavailable, and do not leak raw prompt text or raw provider payload
- [ ] history reads do not create new `ai_interaction_record` rows or mutate ticket workflow state, approvals, or business `audit_event`

## Output Quality

- [ ] golden ticket-summary samples still produce the expected stable shape
- [ ] golden ticket-triage samples still produce the expected stable shape
- [ ] golden ticket-reply-draft samples still produce the expected stable shape
- [ ] golden tests use checked-in provider-response fixtures and the real provider/service parsing path rather than echoing expected output fields back from the sample file
- [ ] summary output still includes issue, current state, latest meaningful signal, and next human follow-up
- [ ] summary output remains non-blank and does not expose raw prompt text or raw provider payload
- [ ] triage output still includes non-blank `classification`, `priority`, and concise `reasoning`
- [ ] triage `priority` remains restricted to `LOW`, `MEDIUM`, or `HIGH`, and the slice stays suggestion-only without assignee suggestions
- [ ] reply-draft output still includes `opening`, `body`, `nextStep`, `closing`, and server-assembled `draftText`
- [ ] the public summary response shape remains stable for `ticketId`, `summary`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public triage response shape remains stable for `ticketId`, `classification`, `priority`, `reasoning`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public reply-draft response shape remains stable for `ticketId`, `draftText`, `opening`, `body`, `nextStep`, `closing`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public interaction-history response shape remains stable for `items`, `page`, `size`, `total`, `totalPages`, and item fields `id`, `interactionType`, `status`, `outputSummary`, `promptVersion`, `modelId`, `latencyMs`, `requestId`, `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`, `usageCostMicros`, and `createdAt`
- [ ] the reply-draft `draftText` still equals `opening + "\n\n" + body + "\n\nNext step: " + nextStep + "\n\n" + closing`
- [ ] assembled reply drafts still fit the current ticket comment length limit
- [ ] the summary, triage, and reply-draft slices stay suggestion-only and do not imply unsupported automatic execution
- [ ] prompt or model changes are reviewed against both happy-path and known-risk samples when those samples exist
- [ ] AI prompt context stays capped to the recent ticket history window and marks omitted older history when the window truncates

## Workflow Safety

- [ ] AI summary, triage, and reply-draft calls do not mutate ticket status, assignee, comments, workflow logs, or approvals
- [ ] AI summary, triage, and reply-draft calls do not create business `audit_event` rows
- [ ] AI interaction-history reads do not mutate ticket status, assignee, comments, workflow logs, approvals, or business `audit_event` rows
- [ ] operators can continue the ticket workflow manually when AI is unavailable
- [ ] ticket detail, ticket workflow log, and business audit behavior remain unchanged by AI summary or triage calls

## API And Documentation Alignment

- [ ] the public AI endpoints appear in Swagger
- [ ] Swagger examples match real request and response shapes
- [ ] AI reference docs are updated in [../reference/ai-integration.md](../reference/ai-integration.md)
- [ ] ticket workflow docs are updated in [../reference/ticket-workflow.md](../reference/ticket-workflow.md)
- [ ] request examples are updated in `api-demo.http`
- [ ] provider configuration docs stay aligned with active runtime keys
- [ ] architecture notes stay aligned with the governing ADRs

## Suggested Minimal Test Pass

For the current Week 6 ticket AI slices, at minimum run:

1. one authorized happy-path request for each affected AI endpoint with a `TICKET_READ` user
2. one permission-denied request
3. one tenant-isolation or not-found check
4. one feature-disabled check
5. one provider-not-configured or provider-unavailable simulation
6. one provider-timeout simulation
7. one invalid-response simulation
8. one interaction-history filter, ordering, and non-leakage check when the history surface is affected
9. one golden-sample regression check for each affected AI generation workflow
10. when live provider wiring changed, one local summary-first provider smoke plus the matching interaction-history read through [ai-live-smoke-test.md](ai-live-smoke-test.md)
11. after summary succeeds and the change still needs real-vendor verification, one second-stage live triage pass plus one second-stage live reply-draft pass, each followed by the matching interaction-history read

## Related Documents

- [../reference/ai-integration.md](../reference/ai-integration.md): current AI workflow guardrails and public contract
- [../reference/ai-provider-configuration.md](../reference/ai-provider-configuration.md): active provider configuration model and keys
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): AI audit and eval baseline decision
- [../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md](../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md): separation rule for AI runtime records
