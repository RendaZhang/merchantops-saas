# Configuration

## Active Profile

`application.yml` defaults to:

- `SPRING_PROFILES_ACTIVE=dev`

Override example:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw -f merchantops-api/pom.xml spring-boot:run
```

## `application-dev.yml`

The `dev` profile contains local integration settings for:

- MySQL
- Redis
- RabbitMQ
- JWT
- local import storage root

## `application.yml`

Shared application configuration currently includes:

- `spring.application.name=merchantops-saas`
- `SPRING_PROFILES_ACTIVE=dev` by default
- `springdoc.show-actuator=true`
- Swagger UI operation sorting: alphabetical
- Swagger UI tag sorting: alphabetical
- Swagger authorization persistence enabled
- Swagger try-it-out enabled by default
- AI runtime defaults:
  - `merchantops.ai.enabled=false`
  - `merchantops.ai.prompt-version=ticket-summary-v1`
  - `merchantops.ai.triage-prompt-version=ticket-triage-v1`
  - `merchantops.ai.reply-draft-prompt-version=ticket-reply-draft-v1`
  - `merchantops.ai.import-error-summary-prompt-version=import-error-summary-v1`
  - `merchantops.ai.import-mapping-suggestion-prompt-version=import-mapping-suggestion-v1`
  - `merchantops.ai.import-fix-recommendation-prompt-version=import-fix-recommendation-v1`
  - `merchantops.ai.provider=OPENAI`
  - `merchantops.ai.base-url=`
  - `merchantops.ai.api-key=`
  - `merchantops.ai.model-id=`
  - `merchantops.ai.timeout-ms=15000`
  - `merchantops.ai.openai.base-url=`
  - `merchantops.ai.openai.api-key=`
- import processing defaults:
  - `merchantops.import.processing.chunk-size=100`
  - `merchantops.import.processing.max-rows-per-job=1000`
  - `merchantops.import.processing.stale-processing-threshold-seconds=300`
  - `merchantops.import.processing.enqueue-recovery-batch-size=100`
  - `merchantops.import.processing.enqueue-recovery-delay-ms=300000`
  - `merchantops.import.processing.enqueue-recovery-min-age-seconds=60`

## Supported Environment Variable Overrides

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_DEFAULT_USER`
- `RABBITMQ_DEFAULT_PASS`
- `RABBITMQ_VHOST`
- `JWT_SECRET`
- `JWT_EXPIRE_SECONDS`
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
- `IMPORT_STORAGE_LOCAL_DIR`
- `IMPORT_PROCESSING_CHUNK_SIZE`
- `IMPORT_PROCESSING_MAX_ROWS_PER_JOB`
- `IMPORT_PROCESSING_STALE_THRESHOLD_SECONDS`
- `IMPORT_PROCESSING_ENQUEUE_RECOVERY_BATCH_SIZE`
- `IMPORT_PROCESSING_ENQUEUE_RECOVERY_DELAY_MS`
- `IMPORT_PROCESSING_ENQUEUE_RECOVERY_MIN_AGE_SECONDS`

## Local `.env` Bootstrap

- During local dev-profile `spring-boot:run`, `MerchantOpsApplication` auto-loads the repository-root `.env` before Spring Boot starts.
- That bootstrap ignores blank lines and comments, trims simple quotes, and does not override already-set system properties or OS environment variables.
- That bootstrap does not search outside the repository root and is skipped for non-dev profile startup.
- This local bootstrap is intentionally limited to the main app entrypoint and does not change `@SpringBootTest` behavior.

## AI Provider Controls

- `merchantops.ai.enabled` is the config-level master switch for the six public AI generation endpoints: ticket `ai-summary`, `ai-triage`, `ai-reply-draft`, plus import `ai-error-summary`, `ai-mapping-suggestion`, and `ai-fix-recommendation`. When `false`, those endpoints return controlled `503 SERVICE_UNAVAILABLE` responses such as `ticket ai summary is disabled` or `import ai error summary is disabled`.
- `merchantops.ai.prompt-version`, `merchantops.ai.triage-prompt-version`, `merchantops.ai.reply-draft-prompt-version`, `merchantops.ai.import-error-summary-prompt-version`, `merchantops.ai.import-mapping-suggestion-prompt-version`, and `merchantops.ai.import-fix-recommendation-prompt-version` are the explicit prompt identifiers stored into `ai_interaction_record` and returned in the public AI responses.
- `merchantops.ai.provider` selects the active provider path. Current supported values are `OPENAI` and `DEEPSEEK`.
- `merchantops.ai.base-url`, `merchantops.ai.api-key`, and `merchantops.ai.model-id` are the provider-neutral runtime keys. Use these first for new local and deployed setups.
- `merchantops.ai.timeout-ms` controls provider connect and read timeouts for the provider-normalized structured-output clients.
- `merchantops.ai.openai.base-url` and `merchantops.ai.openai.api-key` remain supported as legacy OpenAI compatibility keys.
- `DEEPSEEK_API_KEY`, `DEEPSEEK_BASE_URL`, and `DEEPSEEK_MODEL` are supported as local convenience aliases only when `merchantops.ai.provider=DEEPSEEK` and the provider-neutral key is blank.
- Runtime resolution order is: provider-neutral `merchantops.ai.*`, then provider-specific compatibility keys, then provider defaults.
- Provider defaults are:
  - `OPENAI`: base URL defaults to `https://api.openai.com`
  - `DEEPSEEK`: base URL defaults to `https://api.deepseek.com` and model defaults to `deepseek-chat`
- Leaving the required provider model or credentials blank keeps the rest of the application usable; only the current public AI generation endpoints degrade with controlled `503` responses.

## Persisted Feature Flag Controls

Current tenant-scoped persisted flags are managed through `GET /api/v1/feature-flags` and `PUT /api/v1/feature-flags/{key}` rather than `application.yml`.

Current AI generation flags:

- `ai.ticket.summary.enabled`
- `ai.ticket.triage.enabled`
- `ai.ticket.reply-draft.enabled`
- `ai.import.error-summary.enabled`
- `ai.import.mapping-suggestion.enabled`
- `ai.import.fix-recommendation.enabled`

Current workflow bridge flags:

- `workflow.import.selective-replay-proposal.enabled`
- `workflow.ticket.comment-proposal.enabled`

Current flag behavior:

- the six AI generation endpoints require both `merchantops.ai.enabled=true` and the matching persisted feature flag
- `GET /api/v1/tickets/{id}/ai-interactions`, `GET /api/v1/import-jobs/{id}/ai-interactions`, and `GET /api/v1/ai-interactions/usage-summary` are not gated by the persisted flag set
- the two Week 8 workflow proposal bridges use only their matching persisted workflow flag; they do not read `merchantops.ai.enabled`

## Import Processing Controls

- `merchantops.import.storage.local-dir` controls the local filesystem root used by the current storage implementation.
- `merchantops.import.processing.chunk-size` controls how many parsed data rows the worker processes before it persists progress back to `import_job`.
- `merchantops.import.processing.max-rows-per-job` is a hard guardrail; when a file exceeds the configured data-row limit, the worker fails the job with `MAX_ROWS_EXCEEDED`.
- `merchantops.import.processing.stale-processing-threshold-seconds` controls when a `PROCESSING` job becomes eligible for recovery republish and recovery-time stale handling. A stale job with no recorded progress restarts from `PROCESSING`; a stale job that already has `totalCount`, `successCount`, or `failureCount` progress is failed with audit error `PROCESSING_STALE` and job summary `import job processing expired after partial progress`.
- `merchantops.import.processing.enqueue-recovery-batch-size` controls how many aged `QUEUED` jobs and stale `PROCESSING` jobs the scheduled recovery loop republishes in one pass for each recovery query.
- `merchantops.import.processing.enqueue-recovery-delay-ms` controls the fixed delay and initial delay for the scheduled import-job recovery loop.
- `merchantops.import.processing.enqueue-recovery-min-age-seconds` controls how old a `QUEUED` job must be before it is eligible for best-effort republish by the recovery loop.
- Current defaults are intentionally conservative for Week 5: `chunk-size=100`, `max-rows-per-job=1000`, `stale-processing-threshold-seconds=300`, `enqueue-recovery-batch-size=100`, `enqueue-recovery-delay-ms=300000`, and `enqueue-recovery-min-age-seconds=60`.

## Logging

- Console logs include `requestId` from MDC.
- Local log levels are tuned down for AMQP, Redis, and Hikari internals.
