# AI Regression Checklist

Last updated: 2026-03-21

> Maintenance note: keep this page focused on AI-specific safety, audit, eval, and provider behavior. Do not duplicate the normal non-AI API sign-off items from [regression-checklist.md](regression-checklist.md); link there when a change spans both the AI slice and the broader public business surface.

Use this checklist when any of the following change:

- prompt templates
- model selection
- provider adapter logic
- AI feature gating or timeout behavior
- AI-related logging, usage, or error handling
- the public AI summary, ticket triage, or ticket reply-draft response shape or Swagger examples

## Current Public Boundary

The AI checklist is now active because public AI endpoints exist:

- `POST /api/v1/tickets/{id}/ai-summary`
- `POST /api/v1/tickets/{id}/ai-triage`
- `POST /api/v1/tickets/{id}/ai-reply-draft`
- suggestion-only summary, triage, and internal reply-draft results for one current-tenant ticket
- read permission inherited from `TICKET_READ`
- no write-back, no comment creation, and no approval execution in the current slice

## Environment And Control

- [ ] provider credentials and model configuration are loaded from the expected `merchantops.ai.*` keys
- [ ] AI features can be disabled cleanly via `merchantops.ai.enabled`
- [ ] degraded mode behavior is verified when the provider is unavailable or not configured
- [ ] request timeout behavior is explicit and covered by a simulated timeout path

## Tenant And Permission Safety

- [ ] AI input data is limited to the current tenant
- [ ] AI requests do not combine records across tenants
- [ ] permission failures still return normal application errors such as `403`
- [ ] cross-tenant or missing tickets still return normal business `404`
- [ ] the summary, triage, and reply-draft endpoints do not bypass the existing ticket read boundary

## Audit And Traceability

- [ ] AI requests are linked to `tenantId`, `userId`, and `requestId`
- [ ] `promptVersion` is captured
- [ ] `modelId` is captured
- [ ] `status` is captured in `ai_interaction_record`
- [ ] latency is recorded
- [ ] usage or cost metrics are recorded when available from the provider
- [ ] raw provider errors are not leaked directly to API consumers
- [ ] read-only AI calls are recorded without pretending they are normal business write `audit_event` rows

## Output Quality

- [ ] golden ticket-summary samples still produce the expected stable shape
- [ ] golden ticket-triage samples still produce the expected stable shape
- [ ] golden ticket-reply-draft samples still produce the expected stable shape
- [ ] summary output still includes issue, current state, latest meaningful signal, and next human follow-up
- [ ] triage output still includes `classification`, `priority`, and concise `reasoning`
- [ ] reply-draft output still includes `opening`, `body`, `nextStep`, `closing`, and server-assembled `draftText`
- [ ] the public summary response shape remains stable for `ticketId`, `summary`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public triage response shape remains stable for `ticketId`, `classification`, `priority`, `reasoning`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the public reply-draft response shape remains stable for `ticketId`, `draftText`, `opening`, `body`, `nextStep`, `closing`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`
- [ ] the reply-draft `draftText` still equals `opening + "\n\n" + body + "\n\nNext step: " + nextStep + "\n\n" + closing`
- [ ] assembled reply drafts still fit the current ticket comment length limit
- [ ] the summary, triage, and reply-draft slices stay suggestion-only and do not imply unsupported automatic execution
- [ ] prompt or model changes are reviewed against both happy-path and known-risk samples when those samples exist

## Workflow Safety

- [ ] AI summary, triage, and reply-draft calls do not mutate ticket status, assignee, comments, workflow logs, or approvals
- [ ] operators can continue the ticket workflow manually when AI is unavailable
- [ ] ticket detail, ticket workflow log, and business audit behavior remain unchanged by AI summary or triage calls

## API And Documentation Alignment

- [ ] the public AI endpoint appears in Swagger
- [ ] Swagger examples match real request and response shapes
- [ ] AI reference docs are updated in [../reference/ai-integration.md](../reference/ai-integration.md)
- [ ] request examples are updated in `api-demo.http`
- [ ] provider configuration docs stay aligned with active runtime keys
- [ ] architecture notes stay aligned with the governing ADRs

## Suggested Minimal Test Pass

For the current Week 6 ticket AI slices, at minimum run:

1. one authorized happy-path request for each affected AI endpoint with a `TICKET_READ` user
2. one permission-denied request
3. one tenant-isolation or not-found check
4. one feature-disabled check
5. one provider-timeout or provider-unavailable simulation
6. one golden-sample regression check for each affected AI workflow

## Related Documents

- [../reference/ai-integration.md](../reference/ai-integration.md): current AI workflow guardrails and public contract
- [../reference/ai-provider-configuration.md](../reference/ai-provider-configuration.md): active provider configuration model and keys
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): AI audit and eval baseline decision
- [../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md](../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md): separation rule for AI runtime records
