# AI Regression Checklist

Last updated: 2026-03-10

Use this checklist when AI-assisted workflow endpoints are introduced or when any of the following change:

- prompt templates
- model selection
- tool-calling logic
- approval flow behavior
- AI-related logging, cost tracking, or error handling

## Current Boundary

As of today:

- no AI endpoints are public in Swagger
- no AI regression run is required for the current Week 2 user-management-only API surface
- this checklist is the baseline to use once Week 6+ AI features begin landing

## Environment and Control

- [ ] provider credentials and model configuration are loaded from expected environment variables
- [ ] AI features can be disabled cleanly via configuration or feature flag
- [ ] degraded mode behavior is defined when the model provider is unavailable
- [ ] request timeouts and retry behavior are explicit rather than accidental defaults

## Tenant and Permission Safety

- [ ] AI input data is limited to the current tenant
- [ ] AI requests do not combine records across tenants
- [ ] AI-assisted write paths still enforce normal business permissions
- [ ] permission failures still return normal application errors such as `403`
- [ ] approval-gated actions cannot execute without an authorized operator

## Audit and Traceability

- [ ] AI requests are linked to `tenantId`, `userId`, and `requestId`
- [ ] prompt or workflow version is captured
- [ ] model identifier is captured
- [ ] approval or rejection outcome is captured where relevant
- [ ] latency is recorded
- [ ] usage or cost metrics are recorded when available from the provider
- [ ] raw provider errors are not leaked directly to API consumers

## Output Quality

- [ ] golden-set samples still produce acceptable outputs
- [ ] representative failure samples are reviewed after prompt or model changes
- [ ] changes in output shape do not break API contracts or downstream workflow expectations
- [ ] AI suggestions remain understandable enough for operator review
- [ ] AI output is still suggestion-first unless a low-risk approved automation path explicitly exists

## Workflow Safety

- [ ] rejected AI suggestions do not mutate business state
- [ ] accepted AI suggestions write through normal service paths
- [ ] approval and execution events remain distinguishable in logs
- [ ] failed execution after approval leaves the workflow in a recoverable state
- [ ] operators can continue the workflow manually without AI

## API and Documentation Alignment

- [ ] any public AI endpoint appears in Swagger
- [ ] Swagger examples match real request and response shapes
- [ ] AI reference docs are updated in [../reference/ai-integration.md](../reference/ai-integration.md)
- [ ] request examples are added to `api-demo.http` or a dedicated AI HTTP demo file
- [ ] architecture notes stay aligned with the governing ADRs

## Suggested Minimal Test Pass

When the first AI endpoint lands, at minimum run:

1. one happy-path request with an authorized user
2. one permission-denied request
3. one tenant-scope isolation check
4. one provider-failure or timeout simulation
5. one manual approval / rejection flow if the endpoint supports downstream write actions

## Related Documents

- [../reference/ai-integration.md](../reference/ai-integration.md): AI workflow guardrails and planned endpoint shapes
- [../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md](../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md): AI placement and approval model
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): AI audit and eval baseline decision
