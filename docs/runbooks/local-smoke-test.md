# Local Smoke Test

Last updated: 2026-03-12

> Maintenance note: keep this page as the current PowerShell-first live smoke path for the public workflow surface. Keep it step-by-step, happy-path-oriented, and reusable across phases. Broad negative-path coverage and environment-wide checks belong in [automated-tests.md](automated-tests.md) and [regression-checklist.md](regression-checklist.md), not as historical add-ons here.

Use this runbook after the automated suite passes and you want one local end-to-end pass across the current public baseline.

## Scope

This smoke flow covers:

- health and admin login
- current auth/context reads
- tenant-scoped user-management loop
- approval plus audit happy path for user disable
- ticket workflow happy path
- import-job happy path plus error-read surface

Optional RBAC demo endpoint checks stay at the end because they are not part of the main business baseline.

## 1. Run Automated Tests First

Preferred command:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

See [automated-tests.md](automated-tests.md) for the current automated coverage boundary.

## 2. Start The App

If the local infra is not up yet, start it first from a Docker-enabled shell:

```powershell
Copy-Item .env.example .env -ErrorAction SilentlyContinue
docker compose up -d
```

If Docker reports missing variables, confirm `.env` exists in the repository root. If ports `3306`, `6379`, `5672`, or `15672` are already in use, stop the conflicting process or adjust `docker-compose.yml` before continuing.

If controller, repository, entity, migration, or sibling-module signatures changed, refresh the local Maven cache first:

```powershell
.\mvnw.cmd -pl merchantops-api -am install -DskipTests
```

Then start the API from the module directory:

```powershell
Set-Location .\merchantops-api
..\mvnw.cmd spring-boot:run
```

Do not default to `java -jar .\merchantops-api\target\merchantops-api-0.0.1-SNAPSHOT.jar` for local smoke tests. The current build is not packaged as a runnable fat jar.
If port `8080` is already busy, stop the conflicting process or start the app on another port and update `$baseUrl` in the later smoke steps to match.

## 3. Prepare Reusable Variables

Run these commands in a new PowerShell session from the repository root:

```powershell
$baseUrl = "http://localhost:8080"
$tenantCode = "demo-shop"
$adminUsername = "admin"
$adminPassword = "123456"
$smokePrefix = "smoke-{0}" -f [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$smokeUsername = "$smokePrefix-user"
$smokeEmail = "$smokeUsername@demo-shop.local"
$smokePassword = "123456"
$ticketTitle = "$smokePrefix POS register frozen"
$importCsvPath = Join-Path $env:TEMP "$smokePrefix-users.csv"
```

## 4. Health And Admin Login

```powershell
$health = Invoke-RestMethod -Method Get -Uri "$baseUrl/health"
$actuatorHealth = Invoke-RestMethod -Method Get -Uri "$baseUrl/actuator/health"

$adminLogin = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    tenantCode = $tenantCode
    username = $adminUsername
    password = $adminPassword
  } | ConvertTo-Json -Compress)

$token = $adminLogin.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $token" }
```

Expected results:

- `/health` and `/actuator/health` respond successfully
- admin login returns an `accessToken`
- the app is reachable without manual response-body editing before you continue

## 5. Baseline Read Checks

```powershell
$me = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/user/me" -Headers $adminHeaders
$context = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/context" -Headers $adminHeaders
$roleList = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/roles" -Headers $adminHeaders
$userPage = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/users?page=0&size=20" -Headers $adminHeaders
$opsLookup = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/users?page=0&size=20&username=ops" -Headers $adminHeaders
$ticketPage = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/tickets?page=0&size=10" -Headers $adminHeaders

$assigneeId = $opsLookup.data.items[0].id
```

Expected results:

- `/api/v1/user/me` and `/api/v1/context` return the current admin and tenant context
- `/api/v1/roles` returns only current-tenant role options
- `/api/v1/users` and `/api/v1/tickets` return page objects for the current tenant only
- `$assigneeId` resolves from the current tenant user list instead of relying on a hardcoded seeded ID

## 6. User-Management Loop

### 6.1 Create A Smoke User

```powershell
$createUser = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/users" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    username = $smokeUsername
    displayName = "Smoke User"
    email = $smokeEmail
    password = $smokePassword
    roleCodes = @("READ_ONLY")
  } | ConvertTo-Json -Compress)

$newUserId = $createUser.data.id
```

### 6.2 Login As The New User

```powershell
$smokeLogin = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    tenantCode = $tenantCode
    username = $smokeUsername
    password = $smokePassword
  } | ConvertTo-Json -Compress)

$smokeToken = $smokeLogin.data.accessToken
$smokeHeaders = @{ Authorization = "Bearer $smokeToken" }
```

### 6.3 Read Detail And Update The Profile

```powershell
$userDetail = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/users/$newUserId" -Headers $adminHeaders

$updatedUser = Invoke-RestMethod `
  -Method Put `
  -Uri "$baseUrl/api/v1/users/$newUserId" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    displayName = "Smoke User Updated"
    email = "$smokePrefix-updated@demo-shop.local"
  } | ConvertTo-Json -Compress)
```

### 6.4 Replace Roles

```powershell
$roleReplace = Invoke-RestMethod `
  -Method Put `
  -Uri "$baseUrl/api/v1/users/$newUserId/roles" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    roleCodes = @("TENANT_ADMIN")
  } | ConvertTo-Json -Compress)
```

### 6.5 Verify Stale Token Rejection And Refreshed Login

```powershell
curl.exe -i -H "Authorization: Bearer $smokeToken" "$baseUrl/api/v1/context"

$refreshedLogin = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    tenantCode = $tenantCode
    username = $smokeUsername
    password = $smokePassword
  } | ConvertTo-Json -Compress)

$refreshedToken = $refreshedLogin.data.accessToken
$refreshedHeaders = @{ Authorization = "Bearer $refreshedToken" }

curl.exe -i -H "Authorization: Bearer $refreshedToken" "$baseUrl/api/v1/rbac/users/manage"
```

Expected results:

- `POST /api/v1/users` creates an `ACTIVE` user and the new password works immediately for login
- `GET /api/v1/users/{id}` returns one current-tenant user with current `roleCodes`
- `PUT /api/v1/users/{id}` updates only `displayName` and `email`
- `PUT /api/v1/users/{id}/roles` replaces the old role set
- the pre-change token now returns `403` with `token claims are stale, please login again`
- the refreshed login returns a new token whose access matches the new roles

## 7. Approval And User-Disable Loop

### 7.1 Create The Disable Request

```powershell
$disableRequest = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/users/$newUserId/disable-requests" `
  -Headers $refreshedHeaders

$approvalRequestId = $disableRequest.data.id
```

### 7.2 Read Queue, Detail, And Approval Audit

```powershell
$approvalQueue = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/approval-requests?page=0&size=10&status=PENDING&actionType=USER_STATUS_DISABLE&requestedBy=$newUserId" `
  -Headers $adminHeaders

$approvalDetail = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/approval-requests/$approvalRequestId" `
  -Headers $adminHeaders

$approvalAudit = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/audit-events?entityType=APPROVAL_REQUEST&entityId=$approvalRequestId" `
  -Headers $adminHeaders
```

### 7.3 Approve The Request And Verify Disable Behavior

```powershell
$approvalResult = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/approval-requests/$approvalRequestId/approve" `
  -Headers $adminHeaders

curl.exe -i -X POST "$baseUrl/api/v1/auth/login" `
  -H "Content-Type: application/json" `
  -d "{\"tenantCode\":\"$tenantCode\",\"username\":\"$smokeUsername\",\"password\":\"$smokePassword\"}"

curl.exe -i -H "Authorization: Bearer $refreshedToken" "$baseUrl/api/v1/context"

$userAudit = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/audit-events?entityType=USER&entityId=$newUserId" `
  -Headers $adminHeaders
```

Expected results:

- disable-request creation returns a `PENDING` approval request and leaves the target user `ACTIVE` before review
- approval queue and detail are tenant-scoped and show the request created above
- approval audit includes at least `APPROVAL_REQUEST_CREATED`
- approve transitions the request to `APPROVED` and disables the target user
- logging in after approval returns `403` because the user is no longer `ACTIVE`
- the pre-disable token also returns `403` with `user is not active`
- user audit shows the expected create, profile update, role reassignment, and disable write history

## 8. Ticket Workflow Loop

### 8.1 Create And Read A Ticket

```powershell
$ticketCreate = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/tickets" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    title = $ticketTitle
    description = "Register screen froze during checkout."
  } | ConvertTo-Json -Compress)

$ticketId = $ticketCreate.data.id

$ticketDetailBefore = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/tickets/$ticketId" `
  -Headers $adminHeaders
```

### 8.2 Assign, Move, Comment, Close, And Reopen

```powershell
$ticketAssign = Invoke-RestMethod `
  -Method Patch `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/assignee" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    assigneeId = $assigneeId
  } | ConvertTo-Json -Compress)

$ticketInProgress = Invoke-RestMethod `
  -Method Patch `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/status" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    status = "IN_PROGRESS"
  } | ConvertTo-Json -Compress)

$ticketComment = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/comments" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    content = "Investigating store terminal logs."
  } | ConvertTo-Json -Compress)

$ticketClosed = Invoke-RestMethod `
  -Method Patch `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/status" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    status = "CLOSED"
  } | ConvertTo-Json -Compress)

$ticketReopened = Invoke-RestMethod `
  -Method Patch `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/status" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    status = "OPEN"
  } | ConvertTo-Json -Compress)

$ticketDetailAfter = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/tickets/$ticketId" `
  -Headers $adminHeaders

$ticketAudit = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/audit-events?entityType=ticket&entityId=$ticketId" `
  -Headers $adminHeaders
```

Expected results:

- ticket create returns an `OPEN` ticket in the current tenant
- assignee update succeeds only with a current-tenant assignee resolved from the earlier user lookup
- status changes follow the current lifecycle and reopen works with `CLOSED -> OPEN`
- ticket detail shows comments and workflow logs
- audit query accepts lower-case `entityType=ticket` and returns current-tenant workflow audit rows

## 9. Import-Job Loop

### 9.1 Create A Current `USER_CSV` File

```powershell
@"
username,displayName,email,password,roleCodes
$smokePrefix-import,$smokePrefix Import,$smokePrefix-import@demo-shop.local,abc123,READ_ONLY
"@ | Set-Content -Path $importCsvPath
```

### 9.2 Create, Read, Re-check, And Page Errors

```powershell
$importCreateRaw = & curl.exe -sS -X POST "$baseUrl/api/v1/import-jobs" `
  -H "Authorization: Bearer $token" `
  -F "request={\"importType\":\"USER_CSV\"};type=application/json" `
  -F "file=@$importCsvPath;type=text/csv"

$importCreate = $importCreateRaw | ConvertFrom-Json
$importJobId = $importCreate.data.id

$importList = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs?page=0&size=10" `
  -Headers $adminHeaders

$importListByType = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs?page=0&size=10&importType=USER_CSV" `
  -Headers $adminHeaders

$importListByRequester = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs?page=0&size=10&requestedBy=$($me.data.userId)" `
  -Headers $adminHeaders

$importDetailBefore = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId" `
  -Headers $adminHeaders

Start-Sleep -Seconds 3

$importDetailAfter = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId" `
  -Headers $adminHeaders

$importErrors = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId/errors?page=0&size=10" `
  -Headers $adminHeaders

$importAudit = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/audit-events?entityType=IMPORT_JOB&entityId=$importJobId" `
  -Headers $adminHeaders
```

Expected results:

- `POST /api/v1/import-jobs` returns a tenant-scoped `QUEUED` job
- list and detail are tenant-scoped and include the created job
- `importType=USER_CSV` and `requestedBy=<current admin id>` filters both keep the created job visible
- `/api/v1/import-jobs/{id}/errors` returns a page object for the same tenant-scoped job; a clean success keeps `items=[]`
- if local RabbitMQ and worker processing are healthy, the job later moves to `SUCCEEDED` or `FAILED`
- import audit includes `IMPORT_JOB_CREATED`, `IMPORT_JOB_PROCESSING_STARTED`, and a terminal import action

If the job stays in `QUEUED` or `PROCESSING`, treat that as a live infra or worker-follow-up issue rather than silently marking the import smoke step complete.

This smoke path intentionally stops at the clean-success import baseline. Failed-row replay through `POST /api/v1/import-jobs/{id}/replay-failures` and exact-error-code selective replay through `POST /api/v1/import-jobs/{id}/replay-failures/selective` are part of the broader import regression surface and should be checked through [regression-checklist.md](regression-checklist.md) when those paths change.

## 10. Optional RBAC Demo Checks

Run these only if you want a quick sanity check on the seeded demo endpoints:

```powershell
curl.exe -i -H "Authorization: Bearer $token" "$baseUrl/api/v1/rbac/users"
curl.exe -i -H "Authorization: Bearer $token" "$baseUrl/api/v1/rbac/users/manage"
curl.exe -i -H "Authorization: Bearer $token" "$baseUrl/api/v1/rbac/feature-flags"
```

Use `ops` or `viewer` to confirm permission-denied behavior if you need extra demo-RBAC validation.

## 11. Cleanup

Delete the temporary CSV file:

```powershell
Remove-Item -Path $importCsvPath -ErrorAction SilentlyContinue
```

If you want to clean smoke data from the persistent local database, remove dependent rows first:

```sql
DELETE ae
FROM audit_event ae
JOIN approval_request ar ON ar.id = ae.entity_id AND ae.entity_type = 'APPROVAL_REQUEST' AND ar.tenant_id = ae.tenant_id
JOIN users u ON u.id = ar.entity_id
WHERE ar.entity_type = 'USER'
  AND u.username LIKE 'smoke-%';

DELETE ae
FROM audit_event ae
JOIN import_job ij ON ij.id = ae.entity_id AND ae.entity_type = 'IMPORT_JOB' AND ij.tenant_id = ae.tenant_id
WHERE ij.source_filename LIKE 'smoke-%-users.csv';

DELETE ae
FROM audit_event ae
JOIN ticket t ON t.id = ae.entity_id AND ae.entity_type = 'TICKET' AND t.tenant_id = ae.tenant_id
WHERE t.title LIKE 'smoke-% POS register frozen';

DELETE ae
FROM audit_event ae
JOIN users u ON u.id = ae.entity_id AND ae.entity_type = 'USER' AND u.tenant_id = ae.tenant_id
WHERE u.username LIKE 'smoke-%';

DELETE ae
FROM audit_event ae
JOIN users u ON u.id = ae.operator_id AND u.tenant_id = ae.tenant_id
WHERE u.username LIKE 'smoke-%';

DELETE ar
FROM approval_request ar
JOIN users u ON u.id = ar.entity_id
WHERE ar.entity_type = 'USER'
  AND u.username LIKE 'smoke-%';

DELETE ije
FROM import_job_item_error ije
JOIN import_job ij ON ij.id = ije.import_job_id AND ij.tenant_id = ije.tenant_id
WHERE ij.source_filename LIKE 'smoke-%-users.csv';

DELETE FROM import_job
WHERE source_filename LIKE 'smoke-%-users.csv';

DELETE tol
FROM ticket_operation_log tol
JOIN ticket t ON t.id = tol.ticket_id
WHERE t.title LIKE 'smoke-% POS register frozen';

DELETE tc
FROM ticket_comment tc
JOIN ticket t ON t.id = tc.ticket_id
WHERE t.title LIKE 'smoke-% POS register frozen';

DELETE FROM ticket
WHERE title LIKE 'smoke-% POS register frozen';

DELETE ur
FROM user_role ur
JOIN users u ON u.id = ur.user_id
WHERE u.username LIKE 'smoke-%';

DELETE FROM users
WHERE username LIKE 'smoke-%';
```

## Notes

- Use a fresh generated smoke prefix on every run so the flow remains repeatable against a persistent local database.
- Keep this page focused on the main happy path plus the two high-value authz transitions already included here: stale role token rejection and disabled-user rejection.
- For broader negative-path checks such as bad role codes, illegal ticket transitions, viewer write denial, or password-whitespace validation, use [regression-checklist.md](regression-checklist.md).
- If Swagger UI looks stale during a smoke run, confirm you are connected to the newly started process and inspect `/v3/api-docs` directly before assuming the contract did not refresh.
- The cleanup SQL intentionally removes smoke-linked `audit_event` rows by entity as well as operator so admin-driven ticket/import/user writes from the smoke flow do not accumulate as orphaned history.
- You can run the same business requests from [../../api-demo.http](../../api-demo.http) in an IDE that supports `.http` files.
