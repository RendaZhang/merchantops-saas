# Automated Tests

Last updated: 2026-03-21

> Maintenance note: keep this page focused on the current default regression entry point, the current automated coverage boundary, and the remaining manual-only checks. Do not grow it into a historical per-slice changelog; when suites expand or narrow, fold the new reality into the main coverage sections and keep [project-status.md](../project-status.md) aligned.

Use this runbook when you want a fast regression signal before doing manual API verification.

## Recommended Commands

Preferred command for the current workflow + governance + import-row execution work:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

Why this is the default:

- `merchantops-api` currently depends on in-repo changes from `merchantops-infra`
- `-am` (`--also-make`) ensures dependent modules are rebuilt in the same reactor
- this avoids false failures caused by `merchantops-api` compiling against stale jars in the local Maven cache

Use the full reactor only when you want the broader baseline:

```powershell
.\mvnw.cmd test
```

## Coverage Baseline

Current automated coverage is centered on the completed Week 2-5 public workflow baseline plus the active Week 6 ticket AI summary, ticket AI triage, and ticket AI reply-draft paths. Today that means:

- auth and permission checks for the current public user-management, ticket, AI summary, AI triage, AI reply-draft, audit, approval, and import-job endpoints
- controller binding and request-scoped forwarding for the current public workflow surface, including the AI summary, AI triage, and AI reply-draft endpoints
- tenant-scoped query and command service behavior for users, tickets, approvals, and import jobs
- repository-backed user list SQL behavior in `merchantops-infra`
- import authz enforcement for create and replay endpoints, after-commit queue publication, scheduled queued-job recovery, stale-processing redelivery handling, sequential chunked worker execution, processing-progress counters, `MAX_ROWS_EXCEEDED` guardrails, failed-row replay, whole-file replay for full-failure jobs, selective failed-row replay by exact `errorCode`, edited failed-row replay by exact `errorId`, derived-job lineage, filtered queue reads, paged error reporting, row-level failure isolation, error-code summary reporting, and import-specific migration protection
- AI summary, AI triage, and AI reply-draft prompt-version and golden-sample regression coverage
- stale-token rejection after status, role, or permission changes

## Suite Map

### `merchantops-api` tests

- `AuthSecurityIntegrationTest`
  - real `POST /api/v1/auth/login` success and wrong-password failure paths
  - JWT claim generation and parsing for tenant, role, and permission data
  - real `SecurityConfig` + `JwtAuthenticationFilter` + `RequirePermissionInterceptor` behavior for `GET /api/v1/roles`, `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles`
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
  - minimal approval flow coverage for disable requests, including duplicate-pending-request rejection
  - approval queue coverage for tenant isolation, `status` filter, `actionType` filter, `requestedBy` filter, and stable ordering by `createdAt DESC, id DESC`
  - rejection of a pre-disable token on protected endpoints after the user becomes `DISABLED`
  - successful role-reassignment flow for `PUT /api/v1/users/{id}/roles` with refreshed `updated_by`
  - rejection of a pre-change token after role or permission claims become stale
  - successful re-login after role reassignment with new RBAC access
  - user writes emit `audit_event` rows when `X-Request-Id` is present
  - permission seed alignment for new `TICKET_READ` / `TICKET_WRITE` claims
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
  - ticket writes emit `audit_event` rows while preserving workflow-level `ticket_operation_log`
  - `GET /api/v1/audit-events` returns only current-tenant rows and accepts case-insensitive `entityType`
  - ticket write access changing only after role reassignment plus re-login
- `TicketAiSummaryIntegrationTest`
  - real `POST /api/v1/tickets/{id}/ai-summary` happy path for a `TICKET_READ` user with database assertions on `ai_interaction_record`
  - `403` when `TICKET_READ` is missing
  - `404` for cross-tenant ticket access
  - `503` when AI is disabled or the provider times out, with controlled persisted status values
- `TicketAiTriageIntegrationTest`
  - real `POST /api/v1/tickets/{id}/ai-triage` happy path for a `TICKET_READ` user with database assertions on `ai_interaction_record`
  - `403` when `TICKET_READ` is missing
  - `404` for cross-tenant ticket access
  - `503` when AI is disabled or the provider times out, with controlled persisted status values
  - no-side-effect assertions for ticket fields, comments, workflow logs, and audit rows
- `TicketAiReplyDraftIntegrationTest`
  - real `POST /api/v1/tickets/{id}/ai-reply-draft` happy path for a `TICKET_READ` user with database assertions on `ai_interaction_record`
  - `403` when `TICKET_READ` is missing
  - `404` for cross-tenant ticket access
  - `503` when AI is disabled, not configured, unavailable, or the provider times out, with controlled persisted status values
  - invalid-response coverage for oversize assembled drafts against the current comment length limit
  - no-side-effect assertions for ticket fields, comments, workflow logs, and audit rows
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
  - HTTP request binding for `GET /api/v1/tickets` and `GET /api/v1/tickets/{id}`
  - HTTP request binding for `POST /api/v1/tickets`
  - HTTP request binding for `PATCH /api/v1/tickets/{id}/assignee` and `PATCH /api/v1/tickets/{id}/status`
  - HTTP request binding for `POST /api/v1/tickets/{id}/comments`
  - request-scoped forwarding of `tenantId`, `operatorId`, and `requestId`
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
  - stable stubbed summary formatting against checked-in ticket golden samples
- `TicketTriageGoldenSampleTest`
  - stable stubbed triage formatting against checked-in ticket golden samples
- `TicketReplyDraftGoldenSampleTest`
  - stable stubbed reply-draft formatting against checked-in ticket golden samples, including assembled `draftText`
- `OpenAiTicketReplyDraftProviderTest`
  - unsupported content, refusal, and invalid JSON provider-payload failures for the reply-draft adapter
- `TicketQueryServiceTest`
  - page defaulting, filter normalization, and invalid filter-combination rejection for ticket list
  - ticket detail mapping for assignee, comments, and workflow logs
  - ticket detail `NOT_FOUND` path
- `TicketCommandServiceTest`
  - create-ticket persistence with `OPEN` default status and `CREATED` log
  - cross-tenant assignee rejection
  - assignment persistence with `ASSIGNED` log
  - invalid status-transition rejection
  - comment persistence plus `COMMENTED` log and ticket `updatedAt` refresh
- `ImportJobControllerTest`
  - `POST /api/v1/import-jobs`, `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-file`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/edited`, and `GET /api/v1/import-jobs/{id}/errors` request binding, auth failure, permission failure, tenant-context forwarding, replay request validation, and import query binding for `status`, `importType`, `requestedBy`, `hasFailuresOnly`, and `errorCode`
- `ImportJobAuthzIntegrationTest`
  - real authz enforcement for import create plus all current replay write endpoints, including tenant-scoped persistence for authorized writes and `403` rejection for read-only callers
- `ApprovalRequestServiceTest`
  - disable-request creation locks the target user before writing a pending request
  - duplicate pending disable requests are rejected
  - approve/reject paths lock the pending request before updating review state
  - approval queue query normalization with stable `createdAt DESC, id DESC` ordering
  - approval execution delegates to the existing tenant-scoped user status update flow
- `ImportJobCommandServiceTest`
  - queued import-job persistence, failed-row replay as a new derived job, whole-file replay from `FAILED` zero-success sources, selective replay request normalization, edited replay request normalization, replay lineage/audit emission including `replayMode=WHOLE_FILE`, `selectedErrorCodes`, plus `editedErrorIds` / `editedRowCount` / `editedFields`, and after-commit import event publication
  - invalid `importType` rejection
- `ImportJobQueryServiceTest`
  - import-job page normalization, error-page normalization, filter trimming, `requestedBy` / `hasFailures` list mapping, detail `sourceJobId`, `errorCodeCounts`, and item-error hydration
- `ImportJobIntegrationTest`
  - create/list/detail/error-page worker flow with tenant isolation, queue filters, stable ordering, exact `errorCode` filtering, `requestedBy` / `hasFailures` / `errorCodeCounts` reporting, business-row user creation, quoted CSV field persistence, row-level failure isolation, per-chunk counter visibility during `PROCESSING`, `MAX_ROWS_EXCEEDED` guardrails, stale `PROCESSING` redelivery handling, failed-row replay as a derived job, whole-file replay for full-failure jobs, rejection when a source job already has successful rows, selective replay by exact `errorCode`, edited replay by exact `errorId`, replay rejection cases, summary semantics, and import audit events including replay-scope metadata
  - RabbitMQ publish happens only after transaction commit and is suppressed on rollback
  - scheduled queue recovery can republish aged `QUEUED` jobs when the original after-commit publish failed
- `ImportJobWorkerTest`
  - worker reads source files through `ImportFileStorageService` instead of binding directly to the local storage implementation
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
- `RoleControllerTest`
  - `GET /api/v1/roles` unauthorized / forbidden / success paths
  - tenant resolution through request-scoped context and forwarding to `RoleQueryService`

### `merchantops-infra` tests

- `UserRepositoryTest`
  - tenant-scoped native page query
  - `username`, `status`, and `roleCode` filtering
  - `DISTINCT` deduplication across joined role rows
  - pagination ordering and count stability

## What Still Needs Manual Verification

These areas still need manual verification even when the automated suite passes:

- authenticated behavior of endpoints outside the covered login + `/api/v1/roles` + `/api/v1/users` + `/api/v1/tickets` + `/api/v1/tickets/{id}/ai-summary` + `/api/v1/tickets/{id}/ai-triage` + `/api/v1/tickets/{id}/ai-reply-draft` + `/api/v1/import-jobs` + `/api/v1/audit-events` + approval path, such as `/api/v1/user/me`, `/api/v1/context`, the RBAC demo endpoints, and live provider wiring for AI summary, AI triage, and AI reply draft
- Swagger/OpenAPI documentation rendering
- real infra health (`MySQL`, `Redis`, `RabbitMQ`)

Use [local-smoke-test.md](local-smoke-test.md) and [regression-checklist.md](regression-checklist.md) for those checks.

## Recommended Workflow

1. Run `.\mvnw.cmd -pl merchantops-api -am test`
2. If that passes and the change touches public API flow, security wiring, SQL, or migrations, run the relevant path from [local-smoke-test.md](local-smoke-test.md)
3. If the change also affects docs, environment setup, seeded data assumptions, or broader workflow contracts, run [regression-checklist.md](regression-checklist.md)

## Known Pitfalls

- Keep `.\mvnw.cmd -pl merchantops-api -am test` as the default regression entry. Running only `-pl merchantops-api test` can hide sibling-module signature changes behind stale local Maven artifacts.
- For live smoke tests after changing JPA entities, repositories, or API-module dependencies, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests` first, then start the app from the `merchantops-api` module with `..\mvnw.cmd spring-boot:run`. The `spring-boot:run` classpath resolves sibling modules from the local Maven repository, not from uninstalled reactor outputs.
- Do not treat `merchantops-api/target/merchantops-api-0.0.1-SNAPSHOT.jar` as the default local smoke-test entry point. The current packaging does not produce a fat jar that is ready for `java -jar`.
- For H2-based native SQL tests that rely on `MODE=MySQL`, keep `@AutoConfigureTestDatabase(replace = NONE)` and verify the mode through `INFORMATION_SCHEMA.SETTINGS`. `DatabaseMetaData#getURL()` does not reliably echo the `MODE=...` parameter.
- If a change adds or edits a Flyway migration, do at least one real MySQL verification pass after `spring-boot:run`. The current H2 and manually-created integration-test schemas do not prove that Flyway applied the new migration exactly as intended.
- If Flyway reports a checksum mismatch in a real local run, do not edit an already-applied migration to make the error disappear. Create a new `Vx__...sql` migration instead.
- Treat password edge cases as an explicit regression item. If create-user or login password handling changes, verify that leading and trailing whitespace behavior is consistent across both flows before documenting a final business rule.

## Troubleshooting

- If `-pl merchantops-api test` fails with missing repository methods or stale signatures, rerun with `-am`
- If internal SNAPSHOT dependencies are missing during local verification, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests` from the repository root first
- If Maven says `No plugin found for prefix 'spring-boot'`, run from the API module as documented in [local-smoke-test.md](local-smoke-test.md) or call the wrapper with `-f merchantops-api/pom.xml`
- If `spring-boot:run` fails after module-signature changes, install the reactor modules first with `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`
- If automated `/api/v1/users` coverage passes but live verification fails, focus next on runtime config differences such as real JWT secrets, external infra, or deployment-only filters
- If the page contract changes, update both the tests and the user-management docs in the same change
