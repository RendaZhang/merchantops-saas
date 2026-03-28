# AI Provider Configuration

Last updated: 2026-03-28

## Purpose

This page records the active ownership and rollout model for AI provider configuration.

It answers three practical questions for the current public AI slices:

- who owns the provider configuration
- which config keys are active in code today
- how runtime selection and degradation behave across the supported providers

## Current Active Model

MerchantOps currently uses instance-level provider configuration.

Definition:

- one MerchantOps deployment owns one provider configuration for its public AI features
- the configuration is managed by the operator of that deployment, not by ordinary tenant users
- the current provider-driven AI generation surface is `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets/{id}/ai-reply-draft`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`
- the ticket AI interaction history endpoint `GET /api/v1/tickets/{id}/ai-interactions` reuses stored records and does not trigger a provider call
- the import AI interaction history endpoint `GET /api/v1/import-jobs/{id}/ai-interactions` also reuses stored records and does not trigger a provider call

This means the current public AI slices do not support tenant-specific model keys or tenant-managed provider setup.

## Active Config Keys

Current Spring configuration keys:

- `merchantops.ai.enabled`
- `merchantops.ai.prompt-version`
- `merchantops.ai.triage-prompt-version`
- `merchantops.ai.reply-draft-prompt-version`
- `merchantops.ai.import-error-summary-prompt-version`
- `merchantops.ai.import-mapping-suggestion-prompt-version`
- `merchantops.ai.import-fix-recommendation-prompt-version`
- `merchantops.ai.provider`
- `merchantops.ai.base-url`
- `merchantops.ai.api-key`
- `merchantops.ai.model-id`
- `merchantops.ai.timeout-ms`
- `merchantops.ai.openai.base-url`
- `merchantops.ai.openai.api-key`

Current environment-variable overrides:

- `MERCHANTOPS_AI_ENABLED`
- `MERCHANTOPS_AI_PROMPT_VERSION`
- `MERCHANTOPS_AI_TRIAGE_PROMPT_VERSION`
- `MERCHANTOPS_AI_REPLY_DRAFT_PROMPT_VERSION`
- `MERCHANTOPS_AI_IMPORT_ERROR_SUMMARY_PROMPT_VERSION`
- `MERCHANTOPS_AI_IMPORT_MAPPING_SUGGESTION_PROMPT_VERSION`
- `MERCHANTOPS_AI_IMPORT_FIX_RECOMMENDATION_PROMPT_VERSION`
- `MERCHANTOPS_AI_PROVIDER`
- `MERCHANTOPS_AI_BASE_URL`
- `MERCHANTOPS_AI_API_KEY`
- `MERCHANTOPS_AI_MODEL_ID`
- `MERCHANTOPS_AI_TIMEOUT_MS`
- `MERCHANTOPS_AI_OPENAI_BASE_URL`
- `MERCHANTOPS_AI_OPENAI_API_KEY`
- `DEEPSEEK_API_KEY`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`

Current defaults in `application.yml` keep AI optional:

- `enabled=false`
- `prompt-version=ticket-summary-v1`
- `triage-prompt-version=ticket-triage-v1`
- `reply-draft-prompt-version=ticket-reply-draft-v1`
- `import-error-summary-prompt-version=import-error-summary-v1`
- `import-mapping-suggestion-prompt-version=import-mapping-suggestion-v1`
- `import-fix-recommendation-prompt-version=import-fix-recommendation-v1`
- `provider=OPENAI`
- `timeout-ms=5000`
- provider-neutral `base-url`, `api-key`, and `model-id` blank until the deployment operator supplies them
- the legacy OpenAI compatibility keys remain blank until needed

Provider defaults applied at runtime are:

- `OPENAI`: base URL defaults to `https://api.openai.com`
- `DEEPSEEK`: base URL defaults to `https://api.deepseek.com` and model defaults to `deepseek-chat`

## Resolution Order

Runtime selection is provider-normalized rather than hard-wired to one vendor.

For the active `merchantops.ai.provider`, the runtime resolves the effective values in this order:

1. provider-neutral keys under `merchantops.ai.base-url`, `merchantops.ai.api-key`, and `merchantops.ai.model-id`
2. provider-specific compatibility keys
3. provider defaults

Current compatibility rules are:

- `OPENAI` falls back to `merchantops.ai.openai.base-url` and `merchantops.ai.openai.api-key`
- `DEEPSEEK` falls back to `DEEPSEEK_BASE_URL`, `DEEPSEEK_API_KEY`, and `DEEPSEEK_MODEL`
- the DeepSeek aliases are considered only when `merchantops.ai.provider=DEEPSEEK` and the provider-neutral key is blank

## Minimum Current Setup

To enable the public ticket summary, ticket triage, ticket reply-draft, and import AI error-summary plus mapping-suggestion plus fix-recommendation slices, the deployment must provide all of:

- `merchantops.ai.enabled=true`
- `merchantops.ai.provider=OPENAI` or `merchantops.ai.provider=DEEPSEEK`
- a non-blank effective model id
- a non-blank effective API key
- a reachable effective base URL

Typical minimal OpenAI setup:

- `merchantops.ai.enabled=true`
- `merchantops.ai.provider=OPENAI`
- `merchantops.ai.model-id=<openai-model>`
- `merchantops.ai.api-key=<openai-key>`

Typical minimal DeepSeek setup:

- `merchantops.ai.enabled=true`
- `merchantops.ai.provider=DEEPSEEK`
- `merchantops.ai.api-key=<deepseek-key>` or local `DEEPSEEK_API_KEY=<deepseek-key>`
- optional `merchantops.ai.model-id=<deepseek-model>` or local `DEEPSEEK_MODEL=<deepseek-model>`

If any required provider setting is missing, the rest of the application still works and only the public AI generation endpoints degrade with controlled `503 SERVICE_UNAVAILABLE` responses.

## Current Runtime Behavior

The current AI runtime expectation is:

- provider secrets stay out of source control
- the deployment operator chooses the provider path, base URL, and model id
- AI requests apply the configured timeout rather than relying on accidental client defaults
- provider failures do not leak raw provider exceptions to API consumers
- the ticket and import workflows remain manually operable when AI is disabled or unavailable

Current protocol paths are:

- `OPENAI`: `POST /v1/responses` with strict `json_schema`
- `DEEPSEEK`: `POST /chat/completions` with `response_format={type=json_object}` plus provider-aware JSON-only instructions and a minimal example JSON payload

Both paths still normalize:

- `X-Client-Request-Id` forwarding
- timeout versus unavailable classification
- resolved `modelId`
- usage token fields when the provider returns them
- raw JSON text extraction before endpoint-specific validation

## Local Development Convenience

For local dev-profile `spring-boot:run`, the main app entrypoint auto-loads the repository-root `.env` before Spring Boot starts.

This is intended for local operator-owned testing only:

- keep real provider keys only in local `.env`
- prefer provider-neutral `MERCHANTOPS_AI_*` keys for new setups
- use `DEEPSEEK_*` aliases only as local convenience when testing DeepSeek compatibility
- the bootstrap does not search outside the repository root and does not run for non-dev profile startup

## Ownership And Cost Model

Typical owner by deployment style:

- self-hosted or open-source deployment: the person or team operating that MerchantOps instance
- internal company deployment: the platform admin or service owner responsible for the shared MerchantOps environment

Current cost model expectation:

- with instance-level configuration, the deployment operator owns provider usage and billing
- token and cost fields can be recorded internally in `ai_interaction_record` when the provider exposes them
- the current public responses do not expose token or cost breakdowns

## Why The Project Still Starts Here

Instance-level configuration remains the right initial fit because it:

- keeps the early public AI slices focused on workflow value
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

The current ticket summary, ticket triage, ticket reply-draft, and import error-summary plus mapping-suggestion plus fix-recommendation slices do not implement any of those pieces yet.

## Related Documents

- [ai-integration.md](ai-integration.md): current AI workflow boundary and public contract
- [configuration.md](configuration.md): active runtime keys and overrides
- [../runbooks/ai-live-smoke-test.md](../runbooks/ai-live-smoke-test.md): local provider live smoke path and `.env` setup sequence
- [../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md](../architecture/adr/0009-start-with-instance-level-ai-provider-keys-before-tenant-byok.md): formal decision to start with deployment-owned provider keys
