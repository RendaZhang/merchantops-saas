# ADR-0009: Start With Instance-Level AI Provider Keys Before Tenant BYOK

- Status: Accepted
- Date: 2026-03-10

## Context

The project is now explicitly aiming to evolve from a portfolio-quality backend into an open-source reference implementation and only later evaluate commercial opportunities. Planned AI features will be introduced after the workflow, approval, and audit foundations are credible.

At that stage, the project will need some way to call external model providers. The most flexible model would eventually be tenant-level BYOK, but starting there would immediately expand scope into per-tenant secret storage, encryption, rotation, quota handling, UI or admin APIs, cost attribution, and support complexity.

That added scope would slow down the first useful AI rollout and would fit the project's current stage poorly, especially for self-hosted and open-source deployments where one operator usually owns the whole installation.

## Decision

The first AI implementation will start with instance-level provider configuration owned by the MerchantOps deployment operator or platform admin.

That means:

- one deployment configures the provider account used by its AI features
- ordinary tenant users do not manage provider keys
- tenant-level BYOK is deferred until later
- if tenant-level BYOK is introduced in the future, the key owner should be a tenant admin or equivalent tenant-scoped administrator

## Consequences

- the first AI rollout remains simpler and better aligned with the project's open-source self-hosting path
- provider cost, rate limiting, fallback behavior, and outage handling stay centralized at the deployment level
- the initial implementation does not need tenant-scoped secret-management UX or APIs
- per-tenant provider choice and direct tenant cost ownership are deferred
- provider integration should still be implemented behind adapters so the project does not become permanently tied to a single vendor
