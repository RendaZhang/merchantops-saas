# ADR-0007: Embed AI Into Tenant-Scoped Workflows With Human Oversight

- Status: Accepted
- Date: 2026-03-10

## Context

The project is evolving from a multi-tenant SaaS foundation into a workflow-first vertical application. Current market expectations are shifting away from generic SaaS CRUD plus a separate chatbot and toward software that combines system-of-record data, workflow execution, and AI assistance inside real operator flows.

This project is also multi-tenant and RBAC-protected. That means AI cannot be added as an unconstrained overlay. If AI reads cross-tenant data, bypasses write permissions, or executes high-risk actions without approval, it would directly undermine the architecture decisions already made around tenant isolation and endpoint permissions.

## Decision

AI capabilities in this project will be embedded into tenant-scoped business workflows, starting with ticket operations and import/data-quality flows, rather than introduced as a generic standalone assistant.

The default operating model is:

- AI is suggestion-first, not execution-first
- tenant scope and RBAC remain mandatory for AI inputs and follow-up actions
- higher-risk AI-assisted actions require explicit human approval
- AI interactions must be auditable and later evaluable, including prompt/model versioning and basic cost or latency tracking

## Consequences

- AI features become easier to justify as real business value because they are attached to existing workflows
- tenant isolation and permission boundaries remain coherent with the rest of the backend architecture
- the project gains a clearer path to future copilot and agent workflows without normalizing unsafe autonomous actions too early
- implementation becomes more opinionated and slower than simply exposing a generic chat endpoint
- workflow modules, audit patterns, and evaluation assets become prerequisites for meaningful AI rollout
