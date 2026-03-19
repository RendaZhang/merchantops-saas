# AI Provider Configuration

Last updated: 2026-03-19

## Purpose

This page records the active ownership and rollout model for AI provider configuration.

It answers three practical questions for the current Week 6 slice:

- who owns the provider configuration
- which config keys are active in code today
- how the system degrades when AI is not configured or not available

## Current Active Model

MerchantOps currently uses instance-level provider configuration.

Definition:

- one MerchantOps deployment owns one provider configuration for its public AI features
- the configuration is managed by the operator of that deployment, not by ordinary tenant users
- the current public AI surface is `POST /api/v1/tickets/{id}/ai-summary`

This means the current Week 6 slice does not support tenant-specific model keys or tenant-managed provider setup.

## Active Config Keys

Current Spring configuration keys:

- `merchantops.ai.enabled`
- `merchantops.ai.prompt-version`
- `merchantops.ai.model-id`
- `merchantops.ai.timeout-ms`
- `merchantops.ai.openai.base-url`
- `merchantops.ai.openai.api-key`

Current environment-variable overrides:

- `MERCHANTOPS_AI_ENABLED`
- `MERCHANTOPS_AI_PROMPT_VERSION`
- `MERCHANTOPS_AI_MODEL_ID`
- `MERCHANTOPS_AI_TIMEOUT_MS`
- `MERCHANTOPS_AI_OPENAI_BASE_URL`
- `MERCHANTOPS_AI_OPENAI_API_KEY`

Current defaults in `application.yml` keep AI optional:

- `enabled=false`
- `prompt-version=ticket-summary-v1`
- `timeout-ms=5000`
- `openai.base-url=https://api.openai.com`
- `model-id` and `api-key` blank until the deployment operator supplies them

## Minimum Current Setup

To enable the public ticket summary slice, the deployment must provide all of:

- `merchantops.ai.enabled=true`
- a non-blank `merchantops.ai.model-id`
- a non-blank `merchantops.ai.openai.api-key`
- a reachable `merchantops.ai.openai.base-url`

If any required provider setting is missing, the rest of the application still works and only the AI summary endpoint degrades with a controlled `503 SERVICE_UNAVAILABLE` response.

## Current Runtime Behavior

The current AI runtime expectation is:

- provider secrets stay out of source control
- the deployment operator chooses the provider base URL and model id
- AI requests apply the configured timeout rather than relying on accidental client defaults
- provider failures do not leak raw provider exceptions to API consumers
- the ticket workflow remains manually operable when AI is disabled or unavailable

## Ownership And Cost Model

Typical owner by deployment style:

- self-hosted or open-source deployment: the person or team operating that MerchantOps instance
- internal company deployment: the platform admin or service owner responsible for the shared MerchantOps environment

Current cost model expectation:

- with instance-level configuration, the deployment operator owns provider usage and billing
- token and cost fields can be recorded internally in `ai_interaction_record` when the provider exposes them
- the current public response does not expose token or cost breakdowns

## Why The Project Still Starts Here

Instance-level configuration remains the right initial fit because it:

- keeps the first public AI slice focused on workflow value
- avoids tenant-by-tenant key storage, encryption, rotation, and quota UI before the AI path is stable
- matches open-source self-hosting better than immediate tenant BYOK
- keeps degraded-mode behavior centralized and easier to reason about

## Deferred Model: Tenant-Level BYOK

Tenant-level BYOK remains a later extension, not current behavior.

If that model is introduced later, it should add all of the following before becoming public:

- tenant-admin-owned credential management
- encrypted secret storage and rotation
- tenant-level usage and failure visibility
- explicit quota and support boundaries

The current Week 6 ticket summary slice does not implement any of those pieces yet.

## Related Documents

- [ai-integration.md](ai-integration.md): current AI workflow boundary and public contract
- [configuration.md](configuration.md): active runtime keys and overrides
- [../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md](../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md): formal decision to start with deployment-owned provider keys
