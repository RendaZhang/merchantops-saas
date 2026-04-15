# Automated Tests

Last updated: 2026-04-15

> Maintenance note: keep this page focused on the current default regression entry point, the current automated coverage boundary, and the remaining manual-only checks. Do not grow it into a historical per-slice changelog; when suites expand or narrow, fold the new reality into the main coverage sections and keep [project-status.md](../project-status.md) aligned.

Use this runbook when you want a fast regression signal before doing manual API verification.

Latest local default regression result on 2026-04-15:

- `BUILD SUCCESS`
- `Tests run: 416, Failures: 0, Errors: 0, Skipped: 1`

Latest CI-parity Docker build result on 2026-04-11:

- `docker build -t merchantops-api:ci .` completed successfully

Latest Productization Baseline frontend workspace result on 2026-04-15:

- `npm run typecheck` from `merchantops-admin-web` completed successfully
- `npm run lint` from `merchantops-admin-web` completed successfully
- `npm run build` from `merchantops-admin-web` completed successfully

Latest focused Productization Baseline Slice B auth regression on 2026-04-15:

- `.\mvnw.cmd -pl merchantops-api -am -Dtest=AuthSecurityIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` completed successfully
- `Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`

## Recommended Commands

Preferred command for the current workflow + governance + import-row execution work:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

Why this is the default:

- `merchantops-api` currently depends on in-repo changes from `merchantops-infra`
- `-am` (`--also-make`) ensures dependent modules are rebuilt in the same reactor
- this avoids false failures caused by `merchantops-api` compiling against stale jars in the local Maven cache

GitHub Actions runs the Linux equivalent of the same default regression:

```bash
./mvnw -pl merchantops-api -am test
```

The CI quality gate also verifies the root Docker image build:

```bash
docker build -t merchantops-api:ci .
```

That CI Docker check only proves image construction. It does not start the API container, run compose-managed infrastructure, run Dockerized API live smoke, call OpenAI or DeepSeek, run live AI smoke, or enable the opt-in real MySQL migration suite.

When a change touches the admin console, also run the frontend workspace checks:

```powershell
cd merchantops-admin-web
npm run typecheck
npm run lint
npm run build
```

The current GitHub Actions quality gate does not run these frontend checks yet.

Use the full reactor only when you want the broader baseline:

```powershell
.\mvnw.cmd test
```

For the Week 10 Slice B delivery baseline, add this manual container verification after the Maven suite:

```powershell
docker build -t merchantops-api:local .
Copy-Item .env.example .env -ErrorAction SilentlyContinue
docker compose up -d
docker run --rm --name merchantops-api-local `
  --env-file .env `
  --network merchantops-infra `
  -p 8080:8080 `
  -e MYSQL_HOST=mysql `
  -e REDIS_HOST=redis `
  -e RABBITMQ_HOST=rabbitmq `
  merchantops-api:local
```

If the same change also touches AI provider wiring or live vendor compatibility, keep that same Dockerized API path running and add the summary-first pass from [ai-live-smoke-test.md](ai-live-smoke-test.md). For containerized AI smoke, provide `MERCHANTOPS_AI_*` through the local `.env` or explicit `-e` flags; the container does not auto-load the repository-root `.env`.

## Coverage Baseline

Current automated coverage remains centered on the completed Week 2-6 public workflow baseline, the completed Week 7 import AI read baseline, the current two Week 8 human-reviewed execution bridges, the completed Week 9 tenant-scoped AI governance read baseline, the completed Week 10 Slice A persisted feature-flag hardening baseline, and the Productization Baseline auth-session/logout foundation. Week 10 Slice C now runs that same Maven baseline in GitHub Actions and adds Docker image build as a no-secret CI quality gate. Productization Baseline frontend checks are separate from GitHub Actions for now. Today that means:

- auth and permission checks for login, server-side auth-session creation, required JWT `sid`, logout revocation, revoked/sidless/expired session `401` behavior, and the current public user-management, feature-flag, ticket, AI interaction-history, tenant AI usage-summary, AI summary, AI triage, AI reply-draft, audit, approval, and import-job endpoints
- controller binding and request-scoped forwarding for the current public workflow surface, including the AI interaction-history, tenant AI usage-summary, AI summary, AI triage, and AI reply-draft endpoints
- real feature-flag list/update contract coverage for `GET /api/v1/feature-flags` and `PUT /api/v1/feature-flags/{key}`, including `FEATURE_FLAG_MANAGE` happy path, `403`, `404`, `enabled=null` validation, audit snapshots, idempotent no-op updates, and concurrent already-applied updates that return the final persisted row without duplicate audit
- tenant-scoped query and command service behavior for users, tickets, ticket AI interaction history, tenant AI usage-summary, approvals, and import jobs
- repository-backed user list SQL behavior in `merchantops-infra`
- import authz enforcement for create and replay endpoints, after-commit queue publication, scheduled queued-job recovery, scheduled stale-processing recovery, fresh `PROCESSING` duplicate-delivery acknowledgement, stale-processing restart-or-fail handling, late-chunk quiet-stop when a job is no longer active, sequential chunked worker execution, processing-progress counters, handled-row progress persistence before terminal runtime failure, `MAX_ROWS_EXCEEDED` guardrails, failed-row replay, whole-file replay for full-failure jobs, selective failed-row replay by exact `errorCode`, edited failed-row replay by exact `errorId`, derived-job lineage, filtered queue reads, paged error reporting, row-level failure isolation, error-code summary reporting, import-specific migration protection, and approval-request migration protection for pending-request-key hardening across disable, import replay proposal, and ticket comment proposal flows
- Week 8 import selective replay proposal coverage for `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, including `USER_WRITE` enforcement, cross-tenant/missing source-job handling, invalid `errorCodes`, invalid `sourceInteractionId`, safe approval payload persistence, duplicate suppression on canonical `errorCodes`, reproposal after `REJECTED` or `APPROVED`, near-concurrent duplicate create collapse to one `PENDING` row, self-approval guard, repeated review rejection once resolved, synchronous approve-time selective replay execution, reject-without-execution behavior, and no-regression on the existing `USER_STATUS_DISABLE` approval path
- Week 8 ticket comment proposal coverage for `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`, including `TICKET_WRITE` enforcement, cross-tenant/missing ticket handling, blank and overlong comment rejection, invalid same-ticket `sourceInteractionId` rejection, safe approval payload persistence, duplicate suppression on trimmed `commentContent`, reproposal after `REJECTED` or `APPROVED`, near-concurrent duplicate create collapse to one `PENDING` row, self-approval guard, repeated review rejection once resolved, synchronous approve-time single-comment execution, reject-without-execution behavior, and no-regression on the mixed-action approval queue
- AI summary, AI triage, AI reply-draft, and import AI error-summary plus mapping-suggestion plus fix-recommendation prompt-version, AI-context-window, and golden-sample regression coverage through checked-in provider-response fixtures plus the real provider/service parsing path
- a shared Week 9 governance comparator pass across all six generation workflows, with executable inventory plus golden, failure, and policy datasets in the default regression suite
- provider-normalized structured-output coverage for OpenAI Responses `RAW_HTTP`, OpenAI Spring AI chat-completions `SPRING_AI`, and DeepSeek Chat Completions, including request-contract assertions, multi-part `output_text` parsing where applicable, `408` or `504` timeout classification, unsupported content, refusal, invalid JSON, and endpoint-specific required-field validation
- `.env` bootstrap and AI provider-resolution coverage for search order, quote trimming, provider-neutral overrides, legacy OpenAI fallback, DeepSeek alias fallback, and not-configured detection
- shared AI interaction execution support coverage for feature gating, request-id normalization, failure mapping, and `ai_interaction_record` persistence across ticket and import entity types
- persisted feature-flag gate coverage across all six AI generation endpoints plus both Week 8 workflow proposal bridges, including no cross-flag leakage between unrelated endpoints
- domain and adapter coverage for tenant AI usage-summary normalization, exact-match trimmed filter semantics, `from <= to` validation, aggregate query delegation, and nullable metering aggregation with zero-filled totals
- symmetrical degraded-mode persistence coverage for AI summary, AI triage, AI reply-draft, and import AI error summary plus mapping suggestion plus fix recommendation across feature-disabled, provider-not-configured, provider-unavailable, provider-timeout, and invalid-response paths
- explicit no-business-side-effect assertions for AI summary, AI triage, and AI reply-draft against ticket fields, comments, workflow logs, approvals, and business audit rows
- explicit no-business-side-effect assertions for import AI interaction history, error summary, mapping suggestion, and fix recommendation against `import_job`, `import_job_item_error`, replay lineage, approvals, and business audit rows plus prompt non-leakage assertions against raw `USER_CSV` values and sensitive-output rejection for fix recommendation
- ticket AI interaction-history coverage for tenant-scoped ticket existence, `interactionType` and `status` exact-match filters, pagination, stable `createdAt DESC, id DESC` ordering, widened response mapping for usage/cost metadata, and non-leakage of raw prompt and raw provider payload fields
- explicit read-only assertions for `GET /api/v1/tickets/{id}/ai-interactions` against ticket fields, workflow logs, approvals, business audit rows, and `ai_interaction_record` row counts
- tenant AI usage-summary coverage for `USER_READ` enforcement, tenant isolation, inclusive `from/to` filtering, exact-match trimmed `entityType` / `interactionType` / `status` filters, totals math, stable breakdown ordering, null metering rows, non-leakage of request-level fields and raw provider data, and read-only assertions
- import AI interaction-history coverage for import-scoped existence checks through the existing read path, `interactionType` and `status` exact-match filters, pagination, stable `createdAt DESC, id DESC` ordering, widened response mapping for usage/cost metadata, non-leakage of raw prompt and raw provider payload fields, and read-after-write visibility after real import AI generation calls
- explicit read-only assertions for `GET /api/v1/import-jobs/{id}/ai-interactions` against import job fields, import error rows, replay lineage, approvals, business audit rows, and `ai_interaction_record` row counts
- stale-token rejection after tenant status, user status, role, or permission changes

## Suite Map

### `merchantops-api` tests

- `AuthSecurityIntegrationTest`
  - real `POST /api/v1/auth/login` success and wrong-password failure paths
  - JWT claim generation and parsing for tenant, role, permission, and required `sid` data
  - server-side `auth_session` creation, future expiry, bad-credential no-session behavior, active-session `/api/v1/context`, current-session logout revocation, same-token-after-logout `401`, sidless signed token `401`, manually expired session `401`, and independent multi-session behavior
  - real `SecurityConfig` + `JwtAuthenticationFilter` + `RequirePermissionInterceptor` behavior for `GET /api/v1/roles`, `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `PUT /api/v1/users/{id}/roles`, and `GET /api/v1/feature-flags`
  - `401` when Bearer token is missing or invalid
  - `403` when login succeeds but `USER_READ` is absent
  - `403` when login succeeds but `USER_WRITE` is absent
  - `200` for a valid admin token on `GET /api/v1/roles`, including tenant-only role visibility
  - `200` for a valid read-only user token on `GET /api/v1/users`, including tenant-only user visibility
  - `200` for a valid read-only user token on `GET /api/v1/users/{id}`, including tenant-only role-code visibility
  - `400` when create requests try to bind role codes outside the current tenant
  - successful create-user flow with BCrypt password persistence, immediate login, and `created_by` / `updated_by` attribution
  - successful profile-update flow for `PUT /api/v1/users/{id}` with tenant-scoped persistence and refreshed `updated_by`
  - successful disable-user flow for `PATCH /api/v1/users/{id}/status` with refreshed `updated_by`, followed by login rejection for `DISABLED`
  - minimal approval flow coverage for disable requests, including duplicate-pending-request rejection and the database-level pending-disable uniqueness key
  - action-aware approval queue coverage for tenant isolation, `status` filter, `actionType` filter, `requestedBy` filter, stable ordering by `createdAt DESC, id DESC`, and permission-based visibility over mixed action types
  - rejection of a pre-disable token on protected endpoints after the tenant becomes inactive
  - rejection of a pre-disable token on protected endpoints after the user becomes `DISABLED`
  - successful role-reassignment flow for `PUT /api/v1/users/{id}/roles` with refreshed `updated_by`
  - rejection of a pre-change token after role or permission claims become stale
  - successful re-login after role reassignment with new RBAC access including `FEATURE_FLAG_MANAGE`
  - user writes emit `audit_event` rows when `X-Request-Id` is present
  - permission seed alignment for new `TICKET_READ` / `TICKET_WRITE` claims
- `FeatureFlagIntegrationTest`
- real `GET /api/v1/feature-flags` happy path with stable `key ASC` ordering and the current tenant's fixed eight-row contract
- real `PUT /api/v1/feature-flags/{key}` happy path with persisted state change plus `FEATURE_FLAG_UPDATED` audit snapshot
- `403` when `FEATURE_FLAG_MANAGE` is missing and `404` for an unknown key
- `400` when `enabled` is `null`, including the previously disabled-flag path where idempotent short-circuit no longer hides invalid input
- concurrent already-applied update behavior returns the final persisted row and emits no duplicate `FEATURE_FLAG_UPDATED` audit row
- tenant-scoped feature-flag isolation so one tenant's update does not alter another tenant's stored flag rows or runtime AI gate behavior
  - idempotent no-op update behavior with unchanged `updatedAt` and no extra audit row
- `FeatureFlagCommandServiceTest`
  - API service uses the domain-provided `after` snapshot and skips audit when the domain layer reports no mutation after a concurrent already-applied update
- `FeatureFlagCommandDomainServiceTest`
  - domain write results preserve explicit `before` / `after` snapshots and distinguish true mutations from no-change outcomes without forcing a redundant save
- `ImportSelectiveReplayApprovalIntegrationTest`
  - real `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals` happy path for a `USER_WRITE` user with approval-row and audit-row assertions
  - `403` when `USER_WRITE` is missing
  - `404` for cross-tenant or missing import jobs
  - `503` when persisted feature flag `workflow.import.selective-replay-proposal.enabled` is disabled, with no approval-row or audit-row side effects
  - `400` for non-replayable `errorCodes`
  - `400` for invalid `sourceInteractionId`
  - `400` for duplicate pending proposals with the same canonical `errorCodes` even when caller order differs
  - safe approval payload and approval-audit snapshot assertions that exclude raw CSV rows, emails, passwords, and replacement values
  - self-approval rejection on the new import replay approval path
  - reproposal allowed after `REJECTED` and after `APPROVED` when normal selective replay eligibility still passes
  - near-concurrent duplicate create leaves exactly one `PENDING` row
  - approve path creates exactly one derived selective replay job and reuses the existing selective replay execution chain
  - reject path leaves the source job untouched and creates no replay job
  - repeated approve or reject after resolution returns `400`
- `TicketWorkflowIntegrationTest`
  - real `GET /api/v1/tickets` and `GET /api/v1/tickets/{id}` auth + tenant-isolation behavior
  - `GET /api/v1/tickets` queue filters for `status + assigneeId`, `unassignedOnly`, and `keyword` (including title-only hit, description-only hit, and tenant isolation)
  - `assigneeId` filtering does not leak cross-tenant assignee IDs
  - `400` for `assigneeId` + `unassignedOnly=true` invalid query combination
  - `403` when `viewer` attempts ticket write operations
  - `400` when assignment tries to use an assignee outside the current tenant
  - `400` when ticket status transition rules are violated (including no-op transitions)
  - `200` for `CLOSED -> OPEN` reopen with status/detail verification, `updated_at` refresh, and appended `STATUS_CHANGED` log
  - real create -> assign -> status -> comment -> close loop with database assertions on `ticket_operation_log`
  - real `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft` happy path with safe payload and approval-audit assertions
  - `403` for missing `TICKET_WRITE`, `404` for missing or cross-tenant tickets, `503` for disabled persisted workflow flag, and `400` for blank, overlong, or invalid-provenance proposal input
  - `400` for duplicate pending proposals with the same trimmed `commentContent`
  - approve path for `TICKET_COMMENT_CREATE` creates exactly one comment and reject path creates none
  - reproposal allowed after `REJECTED` and after `APPROVED`
  - near-concurrent duplicate create leaves exactly one `PENDING` row
  - repeated approve or reject after resolution returns `400`
  - mixed-action approval queue visibility and review behavior for ticket comment proposals under action-specific read/review permissions
  - ticket writes emit `audit_event` rows while preserving workflow-level `ticket_operation_log`
  - `GET /api/v1/audit-events` returns only current-tenant rows and accepts case-insensitive `entityType`
  - ticket write access changing only after role reassignment plus re-login
- `TicketAiSummaryIntegrationTest`
  - real `POST /api/v1/tickets/{id}/ai-summary` happy path for a `TICKET_READ` user with database assertions on `ai_interaction_record`
  - `403` when `TICKET_READ` is missing
  - `404` for cross-tenant ticket access
  - `503` when AI is disabled, not configured, unavailable, times out, or returns an invalid response, with controlled persisted status values
  - no-side-effect assertions for ticket fields, comments, workflow logs, approvals, and audit rows
- `TicketAiTriageIntegrationTest`
  - real `POST /api/v1/tickets/{id}/ai-triage` happy path for a `TICKET_READ` user with database assertions on `ai_interaction_record`
  - `403` when `TICKET_READ` is missing
  - `404` for cross-tenant ticket access
  - `503` when AI is disabled, not configured, unavailable, times out, or returns an invalid response, with controlled persisted status values
  - no-side-effect assertions for ticket fields, comments, workflow logs, approvals, and audit rows
- `TicketAiReplyDraftIntegrationTest`
  - real `POST /api/v1/tickets/{id}/ai-reply-draft` happy path for a `TICKET_READ` user with database assertions on `ai_interaction_record`
  - `403` when `TICKET_READ` is missing
  - `404` for cross-tenant ticket access
  - `503` when AI is disabled, not configured, unavailable, times out, or returns an invalid response, with controlled persisted status values
  - invalid-response coverage for both provider-thrown invalid responses and oversize assembled drafts against the current comment length limit
  - no-side-effect assertions for ticket fields, comments, workflow logs, approvals, and audit rows
- `TicketAiInteractionHistoryIntegrationTest`
  - real `GET /api/v1/tickets/{id}/ai-interactions` happy path for a `TICKET_READ` user with seeded `ai_interaction_record` rows
  - `403` when `TICKET_READ` is missing
  - `404` for cross-tenant or missing ticket access
  - exact-match `interactionType` and `status` filters plus pagination
  - stable `createdAt DESC, id DESC` ordering, including same-timestamp tie breaks
  - widened response shape with correct usage/cost visibility, nullability for failed rows, and no leakage of raw prompt or raw provider payload
  - read-only assertions for ticket fields, workflow logs, approvals, business audit rows, and `ai_interaction_record` row counts
- `AiInteractionUsageSummaryIntegrationTest`
  - real `GET /api/v1/ai-interactions/usage-summary` happy path for a `USER_READ` user with seeded cross-entity `ai_interaction_record` rows
  - `403` when `USER_READ` is missing
  - tenant isolation over aggregate totals and breakdowns
  - inclusive `from` and `to` filtering plus exact-match trimmed `entityType`, `interactionType`, and `status` filters
  - aggregate totals math, stable `byInteractionType`, `byStatus`, and `byPromptVersion` ordering, and zero-filled sums for null token or cost rows
  - non-leakage of raw prompt or raw provider payload plus request-level fields such as `requestId`, `outputSummary`, and `modelId`, while still allowing aggregate `byPromptVersion[*].promptVersion`
  - read-only assertions for `ai_interaction_record` row counts, business audit rows, ticket state, import state, and approval state
- `UserQueryServiceTest`
  - page defaulting and max-size normalization
  - filter trimming for `username`, `status`, and `roleCode`
  - page result mapping into `UserPageResponse`
  - list and status-filtered list mapping
  - username existence delegation
  - detail lookup success path with role-code hydration
  - detail lookup `NOT_FOUND` path
- `UserCommandServiceTest`
  - duplicate username rejection
  - duplicate username rejection with `excludeUserId`
  - role-code rejection when requested roles are not available in the current tenant
  - create-user persistence with `ACTIVE` default status, BCrypt hashing, `user_role` writes, and operator attribution
  - profile update persistence for mutable fields only plus operator attribution
  - status update persistence for `ACTIVE` and `DISABLED` plus operator attribution
  - invalid status rejection
  - role reassignment with clear-then-write `user_role` semantics plus operator attribution
  - role reassignment rejection when requested role codes are not available in the current tenant
  - tenant-scoped missing-user rejection
  - current password-update placeholder returning unified `BIZ_ERROR` rather than an uncaught runtime exception
- `UserManagementControllerTest`
  - HTTP request binding for `page`, `size`, `username`, `status`, and `roleCode`
  - HTTP request binding for `GET /api/v1/users/{id}`
  - HTTP request binding for `POST /api/v1/users`
  - HTTP request binding for `PUT /api/v1/users/{id}` and `PATCH /api/v1/users/{id}/status`
  - HTTP request binding for `PUT /api/v1/users/{id}/roles`
  - `401` when authentication is missing
  - `403` when `USER_READ` or `USER_WRITE` is missing
  - `401` when authentication exists but tenant context is missing
  - tenant resolution through request-scoped context and forwarding to `UserQueryService`
  - tenant resolution through request-scoped context and forwarding to `UserCommandService`
  - wrapping successful responses with `ApiResponse.success(...)`
- `TicketManagementControllerTest`
  - HTTP request binding for `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, and `GET /api/v1/tickets/{id}/ai-interactions`
  - HTTP request binding for `POST /api/v1/tickets`
  - HTTP request binding for `PATCH /api/v1/tickets/{id}/assignee` and `PATCH /api/v1/tickets/{id}/status`
  - HTTP request binding for `POST /api/v1/tickets/{id}/comments`
  - AI interaction-history query binding for `page`, `size`, `interactionType`, and `status`
  - request-scoped forwarding of `tenantId`, `operatorId`, and `requestId`
- `AiInteractionUsageSummaryControllerTest`
  - HTTP request binding for `GET /api/v1/ai-interactions/usage-summary`
  - `401` when authentication is missing and `403` when `USER_READ` is missing
  - request-scoped tenant resolution plus forwarding of `from`, `to`, `entityType`, `interactionType`, and `status`
- `AiInteractionUsageSummaryQueryServiceTest`
  - mapping from HTTP query DTO to domain usage-summary criteria with exact-value forwarding
  - response mapping for totals plus `byInteractionType`, `byStatus`, and `byPromptVersion`
- `TicketAiSummaryControllerTest`
  - HTTP request binding for `POST /api/v1/tickets/{id}/ai-summary`
  - `401` when authentication is missing and `403` when `TICKET_READ` is missing
  - request-scoped forwarding of `tenantId`, `userId`, and `requestId` to `TicketAiSummaryService`
- `TicketAiTriageControllerTest`
  - HTTP request binding for `POST /api/v1/tickets/{id}/ai-triage`
  - `401` when authentication is missing and `403` when `TICKET_READ` is missing
  - request-scoped forwarding of `tenantId`, `userId`, and `requestId` to `TicketAiTriageService`
- `TicketAiReplyDraftControllerTest`
  - HTTP request binding for `POST /api/v1/tickets/{id}/ai-reply-draft`
  - `401` when authentication is missing and `403` when `TICKET_READ` is missing
  - request-scoped forwarding of `tenantId`, `userId`, and `requestId` to `TicketAiReplyDraftService`
- `TicketSummaryGoldenSampleTest`
  - thin wrapper over the shared eval runner for the checked-in ticket-summary golden dataset and real provider/service parsing path
- `TicketTriageGoldenSampleTest`
  - thin wrapper over the shared eval runner for the checked-in ticket-triage golden dataset and real provider/service parsing path
- `TicketReplyDraftGoldenSampleTest`
  - thin wrapper over the shared eval runner for the checked-in ticket reply-draft golden dataset and real provider/service parsing path, including assembled `draftText`
- `AiInteractionExecutionSupportTest`
  - shared AI interaction execution support behavior for feature-disabled, provider-not-configured, failure mapping, and persisted ticket/import entity metadata
- `AiWorkflowEvalComparatorTest`
  - shared Week 9 governance pass across the six generation workflows with executable inventory, prompt-version assertions, golden datasets, failure datasets, policy datasets, and comparator summary reporting
- `AiPropertiesTest`
  - provider-neutral AI config resolution, legacy OpenAI fallback, DeepSeek alias fallback, defaults, and not-configured detection
- `DotenvBootstrapTest`
  - repository-root detection, dev-profile gating, comment handling, quote trimming, and non-overwrite behavior for already-set properties
- `OpenAiResponsesStructuredOutputAiClientTest`
  - `POST /v1/responses` request contract, strict `json_schema` wiring, OpenAI output parsing, timeout classification, and rollback-path coverage for `merchantops.ai.openai-runtime=RAW_HTTP`
- `SpringAiOpenAiStructuredOutputAiClientTest`
  - `POST /v1/chat/completions` request contract, `response_format=json_schema` wiring, `X-Client-Request-Id` forwarding, OpenAI usage and model metadata extraction, blank-content invalid-response handling, and timeout/unavailable classification for `merchantops.ai.openai-runtime=SPRING_AI`
- `DeepSeekChatCompletionsStructuredOutputAiClientTest`
  - `POST /chat/completions` request contract, `response_format=json_object`, JSON-only instruction and example wiring, DeepSeek message-content parsing, and timeout classification
- `OpenAiTicketSummaryProviderTest`
  - summary-schema, example JSON, provider-response parsing, and missing `summary` provider-payload failures for the summary adapter
- `OpenAiTicketTriageProviderTest`
  - triage-schema, example JSON, provider-response parsing, missing `classification`, missing `reasoning`, missing `priority`, and invalid `priority` provider-payload failures for the triage adapter
- `OpenAiTicketReplyDraftProviderTest`
  - reply-draft schema, example JSON, provider-response parsing, and missing `opening`, `body`, `nextStep`, or `closing` provider-payload failures for the reply-draft adapter
- `OpenAiImportJobErrorSummaryProviderTest`
  - import error-summary schema, example JSON, provider-response parsing, and missing required import fields for the import adapter
- `OpenAiImportJobMappingSuggestionProviderTest`
  - import mapping-suggestion schema, example JSON, provider-response parsing, empty-array handling, blank-field rejection, and invalid header-position handling for the import adapter
- `TicketQueryServiceTest`
  - page defaulting, filter normalization, and invalid filter-combination rejection for ticket list
  - public ticket detail mapping for assignee, comments, and workflow logs
  - ticket AI interaction-history page normalization, exact filter forwarding, required ticket existence, stable `createdAt DESC, id DESC` sorting, and narrowed list-item mapping
  - AI prompt-context windowing to the most recent comments and workflow logs, restored ascending order, omission markers, and prompt truncation guards
  - ticket detail `NOT_FOUND` path
- `TicketCommandServiceTest`
  - create-ticket persistence with `OPEN` default status and `CREATED` log
  - cross-tenant assignee rejection
  - assignment persistence with `ASSIGNED` log
  - invalid status-transition rejection
  - comment persistence plus `COMMENTED` log and ticket `updatedAt` refresh
- `ImportJobControllerTest`
  - `POST /api/v1/import-jobs`, `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-file`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, `POST /api/v1/import-jobs/{id}/replay-failures/edited`, and `GET /api/v1/import-jobs/{id}/errors` request binding, auth failure, permission failure, tenant-context forwarding, replay request validation, proposal request validation, and import query binding for `status`, `importType`, `requestedBy`, `hasFailuresOnly`, and `errorCode`
- `ImportJobAiControllerTest`
  - HTTP request binding for `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`
  - `401` when authentication is missing and `403` when `USER_READ` is missing
  - request-scoped forwarding of `tenantId` plus history query params to `ImportJobQueryService`
  - request-scoped forwarding of `tenantId`, `userId`, and `requestId` to `ImportJobAiErrorSummaryService`, `ImportJobAiMappingSuggestionService`, and `ImportJobAiFixRecommendationService`
- `ImportJobAuthzIntegrationTest`
  - real authz enforcement for import create plus all current replay write endpoints, including tenant-scoped persistence for authorized writes and `403` rejection for read-only callers
- `ApprovalRequestDomainServiceTest`
  - domain-level pending-request-key precheck rejects sequential duplicate ticket comment proposals before save, keeping the duplicate-pending `400` path controlled without relying on database uniqueness translation for normal repeats
- `ApprovalRequestServiceTest`
  - disable-request creation locks the target user before writing a pending request
  - import selective replay proposal creation delegates the normalized approval command and records approval audit
  - ticket comment proposal creation delegates the normalized approval command and records approval audit
  - duplicate pending disable requests are rejected
  - approve/reject paths lock the pending request before updating review state
  - approval queue query normalization with stable `createdAt DESC, id DESC` ordering plus visible-action filtering
  - approval detail hides same-tenant rows whose action type is outside the caller's readable action set
  - approval execution delegates to the existing tenant-scoped user status update flow for `USER_STATUS_DISABLE`, to the existing selective replay flow for `IMPORT_JOB_SELECTIVE_REPLAY`, and to the existing ticket comment write flow for `TICKET_COMMENT_CREATE`
- `ImportJobCommandServiceTest`
  - queued import-job persistence, failed-row replay as a new derived job, whole-file replay from `FAILED` zero-success sources, selective replay request normalization, edited replay request normalization, replay lineage/audit emission including `replayMode=WHOLE_FILE`, `selectedErrorCodes`, plus `editedErrorIds` / `editedRowCount` / `editedFields`, and after-commit import event publication
  - invalid `importType` rejection
- `ImportJobQueryServiceTest`
  - import-job page normalization, import AI interaction-history page forwarding, error-page normalization, filter trimming, `requestedBy` / `hasFailures` list mapping, detail `sourceJobId`, `errorCodeCounts`, and item-error hydration
- `ImportJobAiInteractionHistoryIntegrationTest`
  - real `GET /api/v1/import-jobs/{id}/ai-interactions` happy path for a `USER_READ` user with seeded `ai_interaction_record` rows
  - `403` when `USER_READ` is missing
  - `404` for cross-tenant or missing import jobs
  - exact-match `interactionType` and `status` filters plus pagination
  - stable `createdAt DESC, id DESC` ordering, including same-timestamp tie breaks
  - widened response shape with correct usage/cost visibility, nullability for failed or unmetered rows, and no leakage of raw prompt or raw provider payload
  - read-after-write visibility after real import AI error-summary, mapping-suggestion, and fix-recommendation calls
  - read-only assertions for import job state, import error rows, replay lineage, approvals, business audit rows, and `ai_interaction_record` row counts
- `ImportJobAiErrorSummaryServiceTest`
  - import prompt-version fallback, prompt sanitization against raw `USER_CSV` values, and `INVALID_RESPONSE` persistence semantics for provider-policy failures
- `ImportJobAiMappingSuggestionServiceTest`
  - import prompt-version fallback, sanitized header-signal prompt assembly, `400` rejection for no-failure and no-header-signal jobs, canonical-field normalization, and `INVALID_RESPONSE` persistence semantics for provider-policy failures
- `ImportJobAiFixRecommendationServiceTest`
  - import prompt-version fallback, grounded row-level error-group prompt assembly, `400` rejection for no-failure, unsupported-import-type, and no-row-signal jobs, local affected-row estimate fill, and `INVALID_RESPONSE` persistence semantics for unknown error codes and sensitive-output policy failures
- `ImportJobIntegrationTest`
  - create/list/detail/error-page worker flow with tenant isolation, queue filters, stable ordering, exact `errorCode` filtering, `requestedBy` / `hasFailures` / `errorCodeCounts` reporting, business-row user creation, quoted CSV field persistence, row-level failure isolation, per-chunk counter visibility during `PROCESSING`, `MAX_ROWS_EXCEEDED` guardrails, fresh `PROCESSING` duplicate-delivery acknowledgement, stale `PROCESSING` redelivery handling, late-chunk stop when a job is externally marked terminal between chunk flushes, unexpected row-crash handling that preserves handled progress plus `PROCESSING_ERROR`, failed-row replay as a derived job, whole-file replay for full-failure jobs, rejection when a source job already has successful rows, selective replay by exact `errorCode`, edited replay by exact `errorId`, replay rejection cases, summary semantics, and import audit events including replay-scope metadata
  - RabbitMQ publish happens only after transaction commit and is suppressed on rollback
  - scheduled recovery can republish aged `QUEUED` jobs when the original after-commit publish failed and can republish stale `PROCESSING` jobs without writing a duplicate processing-started audit during republish
- `ImportJobAiErrorSummaryIntegrationTest`
  - real `POST /api/v1/import-jobs/{id}/ai-error-summary` happy path for a `USER_READ` user with database assertions on `ai_interaction_record`
  - `403` when `USER_READ` is missing
  - `404` for cross-tenant import jobs
  - `503` when AI is disabled, not configured, unavailable, times out, or returns an invalid response, with controlled persisted status values
  - prompt-context non-leakage assertions for `rawPayload`, username, email, and password plus no-side-effect assertions for import job state, error rows, replay lineage, approvals, and business audit rows
- `ImportJobErrorSummaryGoldenSampleTest`
  - thin wrapper over the shared eval runner for the checked-in import error-summary golden dataset and real provider/service parsing path
- `ImportJobAiMappingSuggestionIntegrationTest`
  - real `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` happy path for a `USER_READ` user with database assertions on `ai_interaction_record`
  - `403` when `USER_READ` is missing
  - `404` for cross-tenant import jobs
  - `400` when the job has no failure signal or no sanitized header signal
  - `503` when AI is disabled, not configured, unavailable, times out, or returns an invalid response, with controlled persisted status values
  - prompt-context non-leakage assertions for raw header lines, raw row values, username, email, and password plus no-side-effect assertions for import job state, error rows, replay lineage, approvals, and business audit rows
- `ImportJobMappingSuggestionGoldenSampleTest`
  - thin wrapper over the shared eval runner for the checked-in import mapping-suggestion golden dataset and real provider/service parsing path
- `ImportJobAiFixRecommendationIntegrationTest`
  - real `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` happy path for a `USER_READ` user with database assertions on `ai_interaction_record`
  - `403` when `USER_READ` is missing
  - `404` for cross-tenant import jobs
  - `400` when the job has no failure signal, is not `USER_CSV`, or has no sanitized row-level signal
  - `503` when AI is disabled, not configured, unavailable, times out, or returns an invalid response, with controlled persisted status values
  - prompt-context non-leakage assertions for raw row values, username, email, password, and role codes plus no-side-effect assertions for import job state, error rows, replay lineage, approvals, and business audit rows
- `ImportJobFixRecommendationGoldenSampleTest`
  - thin wrapper over the shared eval runner for the checked-in import fix-recommendation golden dataset and real provider/service parsing path
- `OpenAiImportJobFixRecommendationProviderTest`
  - import fix-recommendation schema, example JSON, provider-response parsing, empty-array handling, blank-field rejection, and invalid `reviewRequired` handling for the import adapter
- `ImportJobExecutionServiceTest`
  - fresh `PROCESSING` redelivery returns `REQUEUE` without duplicate processing-started side effects
  - `processChunk` persists handled success/failure counters plus saved row errors before rethrowing an unexpected runtime failure
  - `processChunk`, `completeJob`, and `failJob` become no-ops when the job is no longer `PROCESSING`
- `ImportJobWorkerTest`
  - worker reads source files through `ImportFileStorageService` instead of binding directly to the local storage implementation
  - fresh `PROCESSING` duplicate delivery is acknowledged without duplicating local execution
  - worker stops further chunk flushes when chunk processing reports that execution is no longer active
  - internal sequential chunk boundaries follow the configured chunk size without changing public contract shape
  - quoted commas, escaped quotes, embedded newlines, and UTF-8 BOM headers still normalize into correct row payloads before chunk execution
  - the worker flushes pending rows before failing `MAX_ROWS_EXCEEDED`
- `UserCsvImportProcessorTest`
  - row-level import validation for duplicate usernames, invalid email/password rules, missing role codes, and row-suffixed request-id propagation
- `RequestIdFilterTest`
  - oversized `X-Request-Id` headers are normalized before the value is echoed back to the response and placed in MDC
- `ImportJobMigrationTest`
  - `V9__add_import_job_backbone.sql` rejects `import_job_item_error` rows whose `tenant_id` does not match the parent import job
  - `V10__add_import_job_replay_lineage.sql` rejects cross-tenant `source_job_id` lineage on replay-derived jobs
- `ApprovalRequestMigrationTest`
  - `V12__enforce_pending_disable_uniqueness.sql` converts superseded historical duplicate pending disable rows to `REJECTED`, stamps `reviewed_at` from legacy row time when backfilling that resolution, keeps only the canonical newest pending row keyed per tenant user, preserves same-timestamp tie-breaking by highest `id`, and keeps the unique index usable for a later fresh pending disable request after the canonical row is resolved
  - `V13__harden_pending_proposal_uniqueness.java` backfills canonical pending keys for import replay proposals and ticket comment proposals, clears stale keys from resolved rows, preserves pending-disable uniqueness, stamps migrated duplicate proposal rejections with a deterministic `reviewed_at`, and collapses superseded historical duplicate pending proposal rows to one surviving `PENDING` row per executable key
- `ApprovalRequestRealMysqlMigrationTest`
  - opt-in real MySQL Flyway verification, gated by `MERCHANTOPS_RUN_REAL_MYSQL_MIGRATION_TESTS=true`, migrates a fresh schema through `V12` and `V13`, seeds legacy duplicate approval rows, and proves the fresh-install path plus pending-request-key hardening against the real engine instead of only H2 `MODE=MySQL`
- `RoleControllerTest`
  - `GET /api/v1/roles` unauthorized / forbidden / success paths
  - tenant resolution through request-scoped context and forwarding to `RoleQueryService`

### `merchantops-infra` tests

- `UserRepositoryTest`
  - tenant-scoped native page query
  - `username`, `status`, and `roleCode` filtering
  - `DISTINCT` deduplication across joined role rows
  - pagination ordering and count stability
- `JpaAiInteractionUsageSummaryAdapterTest`
  - aggregate repository delegation for tenant-scoped totals plus `byInteractionType` and `byStatus`
  - zero-filled mapping for nullable token or cost sums
- `JpaApprovalRequestAdapterTest`
  - persists action-aware pending-request keys for `USER_STATUS_DISABLE`, `IMPORT_JOB_SELECTIVE_REPLAY`, and `TICKET_COMMENT_CREATE`
  - translates duplicate-key violations back into the existing `BAD_REQUEST` duplicate-disable behavior plus the new duplicate proposal `400` responses for import and ticket
  - applies visible-action filtering to approval-page queries and short-circuits empty visible-action sets
- `JpaImportJobAdapterTest`
  - import AI interaction-history repository delegation with exact tenant/entity filters
  - stable `createdAt DESC, id DESC` sort wiring
  - nullable usage/cost mapping for failed or unmetered import AI rows

## What Still Needs Manual Verification

These areas still need manual verification even when the automated suite passes:

- authenticated behavior of endpoints outside the covered login + server-side auth-session + `/api/v1/context` + `/api/v1/auth/logout` + `/api/v1/roles` + `/api/v1/users` + `/api/v1/feature-flags` + `/api/v1/tickets` + `/api/v1/tickets/{id}/ai-interactions` + `/api/v1/tickets/{id}/ai-summary` + `/api/v1/tickets/{id}/ai-triage` + `/api/v1/tickets/{id}/ai-reply-draft` + `/api/v1/tickets/{id}/comments/proposals/ai-reply-draft` + `/api/v1/import-jobs` + `/api/v1/import-jobs/{id}/ai-interactions` + `/api/v1/import-jobs/{id}/ai-error-summary` + `/api/v1/import-jobs/{id}/ai-mapping-suggestion` + `/api/v1/import-jobs/{id}/ai-fix-recommendation` + `/api/v1/ai-interactions/usage-summary` + `/api/v1/audit-events` + approval path, such as `/api/v1/user/me`, the remaining RBAC demo endpoints, and real provider wiring through [ai-live-smoke-test.md](ai-live-smoke-test.md)
- Swagger/OpenAPI documentation rendering
- Dockerized API live container smoke; CI verifies image build only
- real infra health (`MySQL`, `Redis`, `RabbitMQ`)

Use [local-smoke-test.md](local-smoke-test.md), [ai-live-smoke-test.md](ai-live-smoke-test.md), and [regression-checklist.md](regression-checklist.md) for those checks.

## Recommended Workflow

1. Run `.\mvnw.cmd -pl merchantops-api -am test`, which matches the CI Maven gate through the Linux equivalent `./mvnw -pl merchantops-api -am test`
2. If that passes and the change touches public API flow, security wiring, SQL, or migrations, run the relevant path from [local-smoke-test.md](local-smoke-test.md)
3. If the change touches AI provider wiring, `.env` loading, or live vendor compatibility, add the summary-first path from [ai-live-smoke-test.md](ai-live-smoke-test.md)
4. If the change also affects docs, environment setup, seeded data assumptions, or broader workflow contracts, run [regression-checklist.md](regression-checklist.md)

## Known Pitfalls

- Keep `.\mvnw.cmd -pl merchantops-api -am test` as the default regression entry. Running only `-pl merchantops-api test` can hide sibling-module signature changes behind stale local Maven artifacts.
- Treat a successful CI Docker build as image-construction evidence only. It does not prove runtime configuration, compose-managed infrastructure connectivity, or authenticated API behavior inside a running container.
- For live smoke tests after changing JPA entities, repositories, or API-module dependencies, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests` first. Use `..\mvnw.cmd spring-boot:run` when the goal is the default local dev path; use the documented Dockerized API command when the verification also needs to prove container delivery or explicit env injection.
- The packaged Spring Boot jar is now valid for Docker delivery and other explicit `java -jar` entrypoints. Local smoke still defaults to `spring-boot:run`, but Dockerized API startup is the correct path when the test target includes the image/runtime contract itself.
- For H2-based native SQL tests that rely on `MODE=MySQL`, keep `@AutoConfigureTestDatabase(replace = NONE)` and verify the mode through `INFORMATION_SCHEMA.SETTINGS`. `DatabaseMetaData#getURL()` does not reliably echo the `MODE=...` parameter.
- If a change adds or edits a Flyway migration, do at least one real MySQL verification pass after `spring-boot:run`. The current H2 and manually-created integration-test schemas do not prove that Flyway applied the new migration exactly as intended.
- If Flyway reports a checksum mismatch in a real local run, do not rewrite a tagged or shared migration just to make the error disappear. The current in-place `V12` repair is a narrow untagged pre-release exception; for already-shared baselines, create a new `Vx__...sql` migration instead.
- If a local development schema already applied the pre-fix `V12`, prefer recreating that schema before the next verification pass. If preserving the schema is unavoidable, repair the stored Flyway checksum only after confirming the rewritten `V12` keeps the same business effect.
- Treat password edge cases as an explicit regression item. If create-user or login password handling changes, verify that leading and trailing whitespace behavior is consistent across both flows before documenting a final business rule.

## Troubleshooting

- If `-pl merchantops-api test` fails with missing repository methods or stale signatures, rerun with `-am`
- If internal SNAPSHOT dependencies are missing during local verification, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests` from the repository root first
- If Maven says `No plugin found for prefix 'spring-boot'`, run from the API module as documented in [local-smoke-test.md](local-smoke-test.md) or call the wrapper with `-f merchantops-api/pom.xml`
- If `spring-boot:run` fails after module-signature changes, install the reactor modules first with `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`
- If automated `/api/v1/users` coverage passes but live verification fails, focus next on runtime config differences such as real JWT secrets, external infra, or deployment-only filters
- If the page contract changes, update both the tests and the user-management docs in the same change
