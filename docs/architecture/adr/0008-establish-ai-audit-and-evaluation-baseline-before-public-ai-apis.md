# ADR-0008: Establish AI Audit And Evaluation Baseline Before Public AI APIs

- Status: Accepted
- Date: 2026-03-10

## Context

The project roadmap now includes AI Copilot and agent-assisted workflows. Those features can create convincing demos quickly, but they are also the easiest place to accumulate hidden risk: weak prompt change control, no audit trail, unclear approval outcomes, rising model cost, and no way to tell whether output quality is improving or regressing.

Because this project is positioned as a realistic AI-enhanced vertical SaaS system, public AI API rollout should not begin with only model invocation and response rendering. It should begin with enough governance to make the feature explainable, reviewable, and supportable.

## Decision

Before public AI endpoints are treated as stable business APIs, the project will establish a minimum AI audit and evaluation baseline.

That baseline includes:

- prompt or workflow version identification
- model identification
- tenantId, userId, and requestId linkage
- approval or rejection outcome for AI-assisted actions where applicable
- basic latency and usage or cost tracking when available
- a small golden set and a lightweight regression checklist for prompt or model changes
- a degraded-path expectation when AI is unavailable or rejected

## Consequences

- public AI capabilities will launch more slowly than a simple provider pass-through integration
- the project gains a credible story for AI governance, supportability, and production-readiness
- prompt changes become easier to review because they are tied to explicit regression expectations
- audit and eval documentation become part of the delivery definition for AI features
- some early AI demos may feel narrower, but they will better match real enterprise constraints
