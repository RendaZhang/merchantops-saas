# Regression Checklist

Last updated: 2026-03-12

> Maintenance note: keep this page as a broad sign-off checklist for release, merge, or phase-close verification. Keep items short, checkable, and outcome-oriented. Do not turn this page into a step-by-step execution guide, troubleshooting log, or duplicate copy of [automated-tests.md](automated-tests.md) or [local-smoke-test.md](local-smoke-test.md); put commands and detailed flows there instead.

Use this checklist after foundation-level changes, security changes, environment changes, user-management API changes, ticket-workflow API changes, audit/approval API changes, or import-job API changes.

## Automated

- [ ] `.\mvnw.cmd -pl merchantops-api -am test` passes
- [ ] `AuthSecurityIntegrationTest`, `UserQueryServiceTest`, `UserCommandServiceTest`, and `UserManagementControllerTest` cover the code path you changed
- [ ] for import-job changes, `ImportJobControllerTest`, `ImportJobCommandServiceTest`, `ImportJobQueryServiceTest`, `ImportJobWorkerTest`, `ImportJobIntegrationTest`, and `ImportJobMigrationTest` cover the staged path
- [ ] if repository signatures changed, tests were run with `-am` rather than `-pl merchantops-api test` only
- [ ] if H2 native SQL tests changed, `@AutoConfigureTestDatabase(replace = NONE)` is still preserved and MySQL-mode assertions still verify the runtime mode

## Infra

- [ ] `docker compose up -d` works
- [ ] MySQL is reachable
- [ ] Redis `PING` returns `PONG`
- [ ] RabbitMQ UI is accessible

## Application

- [ ] application starts successfully
- [ ] if live smoke was needed after module-signature changes, `.\mvnw.cmd -pl merchantops-api -am install -DskipTests` was run before `spring-boot:run`
- [ ] `/health` returns `UP`
- [ ] `/actuator/health` shows `db`, `redis`, and `rabbit` as `UP`
- [ ] Swagger UI is accessible
- [ ] Swagger UI shows documented business endpoints and actuator health coverage
- [ ] Swagger UI preserves Bearer authorization across requests after login

## Database

- [ ] Flyway migrations run automatically
- [ ] core tables exist
- [ ] seed data exists

## Auth

- [ ] admin login works
- [ ] ops login works
- [ ] viewer login works
- [ ] wrong password returns a unified error
- [ ] wrong tenant returns a unified error

## JWT

- [ ] `/api/v1/user/me` requires a token
- [ ] a valid token returns the current user
- [ ] an invalid token returns `401`

## Tenant Context

- [ ] `/api/v1/context` returns tenant and user info
- [ ] request context is available after authentication

## RBAC

- [ ] viewer can read users
- [ ] viewer cannot manage users
- [ ] ops can read users
- [ ] ops cannot manage feature flags
- [ ] admin can access all RBAC demo endpoints

## Tenant Isolation

- [ ] `/api/v1/users` returns only current-tenant users
- [ ] `/api/v1/tickets` returns only current-tenant tickets
- [ ] `/api/v1/tickets/{id}` returns `404` for a ticket outside the current tenant

## User Management

- [ ] Swagger `User Management` tag shows `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `PUT /api/v1/users/{id}/roles`, and `POST /api/v1/users/{id}/disable-requests`
- [ ] Swagger `Approval Requests` tag shows `GET /api/v1/approval-requests`, `GET /api/v1/approval-requests/{id}`, `POST /api/v1/approval-requests/{id}/approve`, and `POST /api/v1/approval-requests/{id}/reject`
- [ ] Swagger `Role Management` tag shows `GET /api/v1/roles`
- [ ] `GET /api/v1/users` returns a page object rather than a bare array
- [ ] `GET /api/v1/users/{id}` returns one current-tenant user and includes `roleCodes`
- [ ] `GET /api/v1/roles` returns only current-tenant role options
- [ ] `GET /api/v1/users?page=0&size=10` works
- [ ] `GET /api/v1/users?username=ad` filters by username
- [ ] `GET /api/v1/users?status=ACTIVE` filters by status
- [ ] `GET /api/v1/users?roleCode=TENANT_ADMIN` filters by role code
- [ ] each `/api/v1/users` item includes `id`, `username`, `displayName`, `email`, and `status`
- [ ] `/api/v1/users` response includes `items`, `page`, `size`, `total`, and `totalPages`
- [ ] `GET /api/v1/users` returns the seeded `admin`, `ops`, and `viewer` users for tenant `demo-shop` when filters allow
- [ ] `POST /api/v1/users` succeeds with an `admin` token and returns `ACTIVE`
- [ ] `POST /api/v1/users` with a `viewer` token returns `403`
- [ ] `POST /api/v1/users` rejects duplicate usernames in the same tenant
- [ ] `POST /api/v1/users` rejects role codes outside the current tenant
- [ ] the created user's password is usable for `POST /api/v1/auth/login`
- [ ] `PUT /api/v1/users/{id}` succeeds with an `admin` token and updates only `displayName` and `email`
- [ ] `PUT /api/v1/users/{id}` with an `ops` or `viewer` token returns `403`
- [ ] `PATCH /api/v1/users/{id}/status` accepts only `ACTIVE` and `DISABLED`
- [ ] `PATCH /api/v1/users/{id}/status` with an `ops` or `viewer` token returns `403`
- [ ] `POST /api/v1/users/{id}/disable-requests` creates a `PENDING` request and leaves user status unchanged
- [ ] duplicate pending disable request creation for same user returns `400`
- [ ] requester cannot approve/reject own disable request (`403`)
- [ ] cross-tenant token cannot read or approve another tenant approval request (`404`)
- [ ] `GET /api/v1/approval-requests?page=0&size=10` returns a page object with tenant-scoped items only
- [ ] `GET /api/v1/approval-requests?status=PENDING` returns only pending items
- [ ] `GET /api/v1/approval-requests?actionType=USER_STATUS_DISABLE` filters by action type
- [ ] `GET /api/v1/approval-requests?requestedBy=<userId>` filters by requester
- [ ] approval queue ordering is stable: `createdAt DESC, id DESC`
- [ ] approve path writes approval audit events and executes user disable (`USER_STATUS_UPDATED`)
- [ ] a `DISABLED` user is rejected by `POST /api/v1/auth/login`
- [ ] a token issued before a user was disabled is rejected on protected endpoints with `403` / `user is not active`
- [ ] `PUT /api/v1/users/{id}/roles` replaces the old role set rather than appending to it
- [ ] `PUT /api/v1/users/{id}/roles` rejects role codes outside the current tenant
- [ ] a token issued before role or permission changes is rejected on protected endpoints with `403` / `token claims are stale, please login again`
- [ ] after role reassignment, the affected user can log in again and the new token reflects the new RBAC access
- [ ] password edge cases, especially leading and trailing whitespace, behave consistently between create and login
- [ ] manual smoke users were cleaned from the local database after verification

## Ticket Workflow

- [ ] Swagger `Ticket Workflow` tag shows `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments`
- [ ] `GET /api/v1/tickets?page=0&size=10` returns a page object rather than a bare array
- [ ] `GET /api/v1/tickets?status=OPEN` filters by ticket status
- [ ] `GET /api/v1/tickets/{id}` returns comments and workflow-level `operationLogs`
- [ ] `POST /api/v1/tickets` succeeds with an `admin` or `ops` token and returns status `OPEN`
- [ ] `POST /api/v1/tickets` with a `viewer` token returns `403`
- [ ] `PATCH /api/v1/tickets/{id}/assignee` rejects assignees outside the current tenant
- [ ] `PATCH /api/v1/tickets/{id}/assignee` rejects disabled assignees
- [ ] `PATCH /api/v1/tickets/{id}/status` accepts `OPEN`, `IN_PROGRESS`, and `CLOSED`
- [ ] `PATCH /api/v1/tickets/{id}/status` allows reopen (`CLOSED -> OPEN`) and still rejects illegal transitions such as `IN_PROGRESS -> OPEN` or no-op `CLOSED -> CLOSED`
- [ ] `POST /api/v1/tickets/{id}/comments` appends a comment that appears in ticket detail
- [ ] create, assign, status, comment, and close flows write `ticket_operation_log` rows
- [ ] after promoting `viewer` to a role with `TICKET_WRITE`, the old token is rejected as stale and the refreshed token can write tickets

## Audit And Approval

- [ ] Swagger `Audit Events` tag shows `GET /api/v1/audit-events`
- [ ] `GET /api/v1/audit-events?entityType=TICKET&entityId=<ticketId>` returns only current-tenant rows
- [ ] `GET /api/v1/audit-events?entityType=ticket&entityId=<ticketId>` also works because the current implementation normalizes `entityType`
- [ ] recent user-management writes generate `audit_event` rows if you inspect the database directly
- [ ] recent ticket writes generate `audit_event` rows without replacing `ticket_operation_log`

## Import Jobs

- [ ] Swagger `Import Jobs` tag shows `POST /api/v1/import-jobs`, `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/edited`, and `GET /api/v1/import-jobs/{id}/errors`
- [ ] `POST /api/v1/import-jobs` accepts multipart `request` + `file` and returns an initial `QUEUED` job
- [ ] `POST /api/v1/import-jobs` with a `viewer` token returns `403`
- [ ] `GET /api/v1/import-jobs?page=0&size=10` returns a page object ordered by `createdAt DESC, id DESC`
- [ ] `GET /api/v1/import-jobs?status=FAILED` filters by exact status
- [ ] `GET /api/v1/import-jobs?importType=USER_CSV` filters by exact import type
- [ ] `GET /api/v1/import-jobs?requestedBy=<userId>` filters by requester within the current tenant only
- [ ] `GET /api/v1/import-jobs?hasFailuresOnly=true` returns only jobs whose `failureCount > 0`, including partial-success `SUCCEEDED` jobs
- [ ] `GET /api/v1/import-jobs/{id}` returns only a current-tenant job and includes nullable `sourceJobId`, `errorCodeCounts`, and backward-compatible `itemErrors`
- [ ] for a multi-chunk job, `GET /api/v1/import-jobs/{id}` can show `status=PROCESSING` with incrementing `totalCount` / `successCount` / `failureCount` before the terminal state is written
- [ ] `POST /api/v1/import-jobs/{id}/replay-failures` rejects clean-success, non-terminal, cross-tenant, and unsupported-import-type source jobs
- [ ] `POST /api/v1/import-jobs/{id}/replay-failures` returns a new `QUEUED` derived job whose detail keeps `sourceJobId=<source job id>`
- [ ] `POST /api/v1/import-jobs/{id}/replay-failures/selective` rejects empty `errorCodes`, unknown selected codes, non-terminal, cross-tenant, and unsupported-import-type source jobs
- [ ] `POST /api/v1/import-jobs/{id}/replay-failures/selective` returns a new `QUEUED` derived job whose execution only replays rows whose `errorCode` exactly matches the request
- [ ] `POST /api/v1/import-jobs/{id}/replay-failures/edited` rejects empty `items`, duplicate `errorId`, cross-job or cross-tenant `errorId`, header/global errors, and unsupported-import-type source jobs
- [ ] `POST /api/v1/import-jobs/{id}/replay-failures/edited` returns a new `QUEUED` derived job whose execution uses only the caller-provided full replacement rows keyed by the requested replayable `errorId`
- [ ] `GET /api/v1/import-jobs/{id}/errors?page=0&size=10` returns a page object ordered by null `rowNumber` first, then `rowNumber ASC, id ASC`
- [ ] `GET /api/v1/import-jobs/{id}/errors?errorCode=<code>` filters failure items by exact error code within the current tenant job only
- [ ] import list items expose `requestedBy` and derived `hasFailures`
- [ ] detail `errorCodeCounts` and `/errors` results stay consistent for partial-success and full-failure jobs
- [ ] worker processing advances jobs through `QUEUED -> PROCESSING -> SUCCEEDED/FAILED`
- [ ] replay-derived jobs go through the same worker path and only include the source job's failed rows, not the rows that already succeeded
- [ ] invalid CSV row shapes create `import_job_item_error` rows and surface them through job detail
- [ ] business-row failures such as duplicate username, unknown role, invalid email, or invalid password also surface through `itemErrors` without blocking valid rows in the same job
- [ ] files that exceed the configured row cap fail with `status=FAILED`, `errorSummary="import job exceeded max row limit"`, and a queryable `MAX_ROWS_EXCEEDED` error row
- [ ] successful jobs keep `errorSummary = null`, partial-success jobs keep `status=SUCCEEDED` with `errorSummary = "completed with some row errors"`, and full failures keep `status=FAILED`
- [ ] import create/process flow writes `IMPORT_JOB_CREATED`, `IMPORT_JOB_PROCESSING_STARTED`, and a terminal `IMPORT_JOB_COMPLETED` or `IMPORT_JOB_FAILED` audit event
- [ ] failed-row replay also writes `IMPORT_JOB_REPLAY_REQUESTED` on the source job and keeps `sourceJobId` in the replay job's `IMPORT_JOB_CREATED` audit snapshot
- [ ] selective replay keeps the same audit event types and adds `selectedErrorCodes` to both the source-job replay-requested snapshot and the replay-job created snapshot
- [ ] edited replay keeps the same audit event types and adds `editedErrorIds`, `editedRowCount`, and `editedFields` without persisting replacement row values

## Tools

- Use [automated-tests.md](automated-tests.md) for the fastest regression command and coverage scope
- Use [../../api-demo.http](../../api-demo.http) for the main request flow
- Compare the current user-list behavior against [../reference/user-management.md](../reference/user-management.md)
- Use [local-smoke-test.md](local-smoke-test.md) when you want a shorter step-by-step validation path
