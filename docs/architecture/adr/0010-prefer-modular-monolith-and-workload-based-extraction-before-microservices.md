# ADR-0010: Prefer Modular Monolith And Workload-Based Extraction Before Microservices

- Status: Accepted
- Date: 2026-03-11

## Context

MerchantOps SaaS is being built to support three goals in sequence:

1. a credible portfolio-quality backend
2. an open-source reference implementation
3. later commercial exploration if the workflow, AI, and governance layers become credible

The current repository uses a multi-module Maven structure to separate API, domain, and infrastructure concerns. Shared business errors, use cases, and ports now live in `merchantops-domain`, while HTTP response-envelope concerns stay in `merchantops-api`. The next phases will add ticket workflow, audit and approval patterns, async import, and AI-assisted operations on top of shared tenant isolation, RBAC, request tracing, and audit expectations.

At this stage, a service-per-entity split such as `user-service`, `ticket-service`, or similar microservices would introduce distributed authentication, cross-service authorization, data-consistency, deployment, and observability complexity before the workflow boundaries have been proven by real business modules.

Kubernetes deployment does not require microservices. A modular monolith plus worker processes can already be deployed cleanly on Kubernetes while keeping the project operationally simpler and the architecture story easier to understand.

## Decision

MerchantOps SaaS will use a workflow-first modular monolith as its primary architecture path during the current roadmap.

That means:

- keep a single core application as the primary business API surface
- keep tenant, user, role, permission, and cross-cutting governance rules inside the core application boundary
- continue expressing boundaries through modules and bounded contexts inside the repository rather than through network hops too early
- treat Kubernetes as a deployment target for clear runtime units, not as a reason to fragment the codebase prematurely

If deployment or scaling separation becomes useful, the project should first split by workload or capability, not by generic entity ownership.

Preferred early extraction candidates:

- async import processing workers
- AI orchestration or model-calling workers
- notification or background-job execution
- later, if justified, a workflow module such as ticket operations

Avoid as an early extraction pattern:

- a `user-service` or other service that mainly centralizes identity, tenant, or RBAC data needed across the whole platform

Microservice extraction should be considered only when one or more of these conditions are true:

- a capability needs independent scaling characteristics
- the runtime model differs materially from the core API, such as long-running workers
- ownership boundaries are stable enough that separate release cadence is beneficial
- failure isolation or blast-radius reduction clearly outweighs the added system complexity
- the data ownership boundary is clear enough to avoid constant cross-service joins or coordination

## Consequences

- the current multi-module structure remains a useful foundation rather than a temporary throwaway
- Week 3 through Week 5 can focus on proving workflow value instead of absorbing distributed-system overhead
- tenant isolation, RBAC, request identity, and later audit rules stay easier to enforce consistently
- Kubernetes rollout can stay straightforward at first, for example one API deployment plus one or more worker deployments
- later service extraction remains possible, but it becomes an optimization based on proven boundaries instead of a speculative starting point
- the project gains a clearer portfolio and open-source story: strong boundaries first, selective operational decomposition later
