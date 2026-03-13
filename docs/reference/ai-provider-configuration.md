# AI Provider Configuration

Last updated: 2026-03-10

## Purpose

This page records the intended ownership and rollout model for future AI provider configuration.

It answers three practical questions before AI APIs are public:

- who should own the provider key
- how usage cost should be understood
- why the project starts with instance-level configuration instead of tenant BYOK

## Current Boundary

As of today:

- no AI endpoints are public in Swagger
- no provider configuration contract is active in code yet
- this document is architectural guidance so later implementation stays aligned with tenant isolation, RBAC, and the project's open-source goals

Do not document concrete environment-variable names, admin screens, or public setup steps as active behavior until they exist in code.

## Recommended Initial Model

Start with instance-level provider configuration.

Definition:

- one MerchantOps deployment owns one provider configuration for its AI features
- the configuration is managed by the operator of that deployment, not by ordinary tenant users

Typical owner by deployment style:

- self-hosted or open-source deployment: the person or team operating that MerchantOps instance
- internal company deployment: the platform admin or service owner responsible for the shared MerchantOps environment

## Why Start Here

Instance-level configuration is the best fit for the first AI rollout because it:

- keeps secret management simple
- matches open-source self-hosted usage well
- centralizes cost control, rate limiting, and fallback behavior
- avoids adding tenant-by-tenant key storage, encryption, rotation, and quota UI before the first AI workflows are stable
- keeps the first AI release focused on workflow value instead of secret-management plumbing

## Future Model: Tenant-Level BYOK

Tenant-level BYOK should be treated as a later extension, not the starting point.

Definition:

- each tenant can supply its own model-provider credentials
- those credentials are used only for that tenant's AI requests

If this model is added later, the key owner should be:

- the tenant admin or an equivalent tenant-scoped administrator
- not an ordinary operator, viewer, or end user

Tenant-level BYOK becomes more reasonable only after:

- public AI workflows exist and are stable enough to justify per-tenant configuration
- secrets can be encrypted, rotated, audited, and revoked safely
- the system can track per-tenant usage, quotas, and failure states clearly

## Cost Model

Hosted model providers usually charge by token or another usage-based unit.

That means:

- with instance-level configuration, the deployment operator pays the provider bill
- with tenant-level BYOK, each tenant pays its own provider bill through its own provider account
- with self-hosted model infrastructure, provider token billing may disappear, but infrastructure or GPU cost replaces it

The project should treat cost visibility as part of the AI governance layer regardless of which provider model is used.

## Security And Operations Expectations

The first implementation should assume:

- provider secrets stay out of source control
- persisted secrets are encrypted at rest
- only the deployment operator or the appropriate admin role can update provider settings
- logs may record provider choice, request metadata, latency, and usage, but never the raw secret
- AI calls support timeout, rate limiting, degraded mode, and feature-flag control

## Open-Source Fit

For an open-source-first rollout, instance-level configuration is the most practical default because:

- a self-hosting user can point the project at a supported provider without reworking tenant models
- the repository can stay vendor-neutral through provider adapters
- AI can remain optional, so the project is still usable even when no provider key is configured

This also keeps the open-source version honest: the public repository demonstrates the workflow architecture and guardrails first, then grows into more advanced secret-management options later.

## Documentation Rule

Until the code exists:

- keep this page at the architecture and rollout-policy level
- avoid inventing environment-variable names or admin UI flows
- only publish concrete setup steps once implementation is visible in code and reflected in the relevant reference docs or Swagger surface

## Related Documents

- [ai-integration.md](ai-integration.md): workflow placement, RBAC boundaries, and AI rollout guardrails
- [../project-plan.md](../project-plan.md): 10-week plan with the open-source and AI delivery path
- [../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md](../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md): formal decision to start with deployment-owned provider keys
