# AI Integration

Last updated: 2026-03-10

## Purpose

This project treats AI as an embedded workflow layer, not as a standalone chatbot feature.

The target direction is:

- tenant-scoped AI Copilot inside real business workflows
- human-in-the-loop approval for higher-risk actions
- auditable and evaluable AI outputs
- clear RBAC boundaries around AI-assisted actions

## Current Boundary

As of today:

- no AI endpoints are exposed in Swagger yet
- no AI controller routes are public HTTP API yet
- the current public business surface is still centered on Week 2 user management
- AI design is being documented before implementation so workflow, audit, and permission boundaries are explicit

Do not document future AI endpoints as callable until they are published in controllers and visible in OpenAPI.

## Planned AI Workflow Areas

### 1. Ticket Operations

Planned Week 6 AI Copilot capabilities:

- ticket summary
- ticket classification suggestion
- priority suggestion
- assignee suggestion
- reply draft generation
- operation-log summary

Typical shape:

1. load ticket data within tenant scope
2. generate AI suggestion
3. present suggestion to operator
4. require manual approval for any write-back action
5. record the AI request, output summary, and approval result

### 2. Import and Data Quality

Planned Week 7 AI Copilot capabilities:

- import error summary
- error clustering
- field-mapping suggestion
- fix recommendation
- failed-row summary

Typical shape:

1. load import job and error records within tenant scope
2. generate AI interpretation or recommendation
3. let operator accept, reject, or edit the output
4. write follow-up actions through normal permission-checked business APIs

## Core Integration Rules

### Tenant Scope

- AI input must be limited to the current tenant
- prompt assembly must not mix data across tenants
- any cached AI artifacts must remain tenant-scoped

### RBAC

- AI read access inherits the same permission boundary as the source business data
- AI-assisted write actions must still pass normal write permissions
- AI tooling must never bypass `@RequirePermission` or equivalent service-layer checks

### Human Oversight

- AI output is suggestion-first by default
- high-risk actions must require explicit human approval
- auto-execution, if ever introduced, should be limited to low-risk and reversible actions

### Auditability

At minimum, AI-related logs should capture:

- tenantId
- userId
- requestId
- business entity type and id
- prompt version
- model identifier
- output summary
- approval status
- latency
- token or cost metrics when available

### Evaluation

Before AI features are treated as stable, the project should keep:

- a small golden dataset
- representative failure samples
- a prompt or model regression checklist
- basic quality and latency review for major changes

## Provider Ownership Direction

- the initial rollout should assume instance-level provider configuration owned by the MerchantOps deployment operator or platform admin
- tenant-level BYOK is a later extension and should be limited to tenant admins, not ordinary end users
- hosted model integrations will usually be usage- or token-metered, while self-hosted models shift cost from provider billing to infrastructure cost
- concrete environment-variable names or config keys should only be documented once they exist in code

## Recommended Future API Shape

These are planned examples, not current public endpoints.
Permission labels below are illustrative placeholders, not current code-level permission names:

| Method | Path | Purpose | Suggested Permission Pattern |
| --- | --- | --- | --- |
| `POST` | `/api/v1/tickets/{ticketId}/ai-summary` | Summarize a ticket | ticket read permission |
| `POST` | `/api/v1/tickets/{ticketId}/ai-triage` | Suggest classification / priority | ticket read permission |
| `POST` | `/api/v1/tickets/{ticketId}/ai-reply-draft` | Draft a reply | ticket write permission |
| `POST` | `/api/v1/import-jobs/{jobId}/ai-error-summary` | Summarize import failures | import read permission |
| `POST` | `/api/v1/import-jobs/{jobId}/ai-mapping-suggestion` | Suggest field mapping | import write permission |
| `POST` | `/api/v1/import-jobs/{jobId}/ai-fix-recommendation` | Suggest follow-up fixes | import write permission |

These paths should only move into Swagger after the underlying workflow, auth, and audit behavior exist in code.

## Implementation Expectations

When AI endpoints are introduced, each one should have:

- OpenAPI examples
- a matching request in `api-demo.http` or a dedicated AI demo HTTP file
- reference documentation with real request and response examples
- regression or smoke-test coverage in `docs/runbooks/`

## Failure and Safety Expectations

Expected non-happy-path handling:

- model failure should return a controlled application error, not raw provider output
- permission failure should remain `403`
- tenant scope failure should behave like business data not found or forbidden access
- degraded mode should allow operators to continue the business workflow without AI

## Related Documents

- [../project-plan.md](../project-plan.md): 10-week AI-enhanced roadmap
- [authentication-and-rbac.md](authentication-and-rbac.md): auth and permission boundaries
- [user-management.md](user-management.md): current Week 2 business-loop boundary
- [../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md](../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md): architecture decision for AI workflow placement and governance
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): minimum audit and eval baseline for future AI APIs
- [ai-provider-configuration.md](ai-provider-configuration.md): provider-key ownership, cost model, and rollout strategy for future AI features
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): rollout checklist for future AI endpoint changes
