# Local Smoke Test

## 0. Run Automated Tests First

Preferred command:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

Run the manual smoke flow below after the automated checks pass. See [automated-tests.md](automated-tests.md) for current unit-test coverage and command selection notes.

## 0.5 Start The App

If controller, repository, entity, or other sibling-module signatures changed, prepare the local Maven cache first:

```powershell
.\mvnw.cmd -pl merchantops-api -am install -DskipTests
```

Then start the API from the module directory:

```powershell
Set-Location .\merchantops-api
..\mvnw.cmd spring-boot:run
```

Do not default to `java -jar .\merchantops-api\target\merchantops-api-0.0.1-SNAPSHOT.jar` for local smoke tests. The current build is not packaged as a runnable fat jar.

## 1. Verify Health

```bash
curl -s http://localhost:8080/health
curl -s http://localhost:8080/actuator/health
```

## 2. Log In

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantCode":"demo-shop","username":"admin","password":"123456"}'
```

PowerShell:

```powershell
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"admin\",\"password\":\"123456\"}"
```

Copy the returned `accessToken`.

## 3. Check Authenticated Endpoints

```bash
TOKEN=<paste-accessToken-from-login-response>
SMOKE_USERNAME="cashier-$(date +%s)"
SMOKE_EMAIL="${SMOKE_USERNAME}@demo-shop.local"
ASSIGNEE_ID=2
# ASSIGNEE_ID=2 assumes a fresh local database with the seeded `ops` user.
# If your local data has drifted, paste the current tenant `ops` id from GET /api/v1/users instead.
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/user/me
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/context
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/roles
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/users?page=0&size=10"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/tickets?page=0&size=10"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/users?page=0&size=10&username=ad&status=ACTIVE&roleCode=TENANT_ADMIN"
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"username\":\"$SMOKE_USERNAME\",\"displayName\":\"Cashier User\",\"email\":\"$SMOKE_EMAIL\",\"password\":\"123456\",\"roleCodes\":[\"READ_ONLY\"]}" \
  http://localhost:8080/api/v1/users
NEW_USER_ID=<paste-id-from-create-response>
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$SMOKE_USERNAME\",\"password\":\"123456\"}"
SMOKE_TOKEN=<paste-accessToken-from-smoke-user-login-response>
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/$NEW_USER_ID
curl -i -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"displayName\":\"Cashier User Updated\",\"email\":\"$SMOKE_USERNAME.updated@demo-shop.local\"}" \
  http://localhost:8080/api/v1/users/$NEW_USER_ID
curl -i -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"roleCodes\":[\"TENANT_ADMIN\"]}" \
  http://localhost:8080/api/v1/users/$NEW_USER_ID/roles
curl -i -H "Authorization: Bearer $SMOKE_TOKEN" http://localhost:8080/api/v1/context
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$SMOKE_USERNAME\",\"password\":\"123456\"}"
REFRESHED_TOKEN=<paste-accessToken-from-role-refresh-login-response>
curl -i -H "Authorization: Bearer $REFRESHED_TOKEN" http://localhost:8080/api/v1/rbac/users/manage
curl -i -X POST -H "Authorization: Bearer $REFRESHED_TOKEN" \
  http://localhost:8080/api/v1/users/$NEW_USER_ID/disable-requests
APPROVAL_REQUEST_ID=<paste-id-from-disable-request-response>
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/approval-requests?page=0&size=10"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/approval-requests?page=0&size=10&status=PENDING&actionType=USER_STATUS_DISABLE&requestedBy=$NEW_USER_ID"
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/approval-requests/$APPROVAL_REQUEST_ID
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/audit-events?entityType=APPROVAL_REQUEST&entityId=$APPROVAL_REQUEST_ID"
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"title\":\"$SMOKE_USERNAME POS register frozen\",\"description\":\"Register screen froze during checkout.\"}" \
  http://localhost:8080/api/v1/tickets
TICKET_ID=<paste-id-from-ticket-create-response>
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tickets/$TICKET_ID
curl -i -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"assigneeId\":$ASSIGNEE_ID}" \
  http://localhost:8080/api/v1/tickets/$TICKET_ID/assignee
curl -i -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}' \
  http://localhost:8080/api/v1/tickets/$TICKET_ID/status
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"Investigating store terminal logs."}' \
  http://localhost:8080/api/v1/tickets/$TICKET_ID/comments
curl -i -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"CLOSED"}' \
  http://localhost:8080/api/v1/tickets/$TICKET_ID/status
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tickets/$TICKET_ID
curl -i -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"OPEN"}' \
  http://localhost:8080/api/v1/tickets/$TICKET_ID/status
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tickets/$TICKET_ID
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/audit-events?entityType=ticket&entityId=$TICKET_ID"
curl -i -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/approval-requests/$APPROVAL_REQUEST_ID/approve
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$SMOKE_USERNAME\",\"password\":\"123456\"}"
curl -i -H "Authorization: Bearer $REFRESHED_TOKEN" http://localhost:8080/api/v1/context
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/audit-events?entityType=USER&entityId=$NEW_USER_ID"
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"username\":\"$SMOKE_USERNAME-bad-role\",\"displayName\":\"Cashier User\",\"email\":\"$SMOKE_USERNAME-bad-role@demo-shop.local\",\"password\":\"123456\",\"roleCodes\":[\"OTHER_ONLY\"]}" \
  http://localhost:8080/api/v1/users
IMPORT_CSV=$(mktemp)
printf 'username,email\n%s-import,%s-import@demo-shop.local\n' "$SMOKE_USERNAME" "$SMOKE_USERNAME" > "$IMPORT_CSV"
curl -i -X POST -H "Authorization: Bearer $TOKEN" \
  -F 'request={"importType":"USER_CSV"};type=application/json' \
  -F "file=@$IMPORT_CSV;type=text/csv" \
  http://localhost:8080/api/v1/import-jobs
IMPORT_JOB_ID=<paste-id-from-import-create-response>
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/import-jobs?page=0&size=10"
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/import-jobs/$IMPORT_JOB_ID
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/audit-events?entityType=IMPORT_JOB&entityId=$IMPORT_JOB_ID"
rm -f "$IMPORT_CSV"
```

PowerShell:

```powershell
$token = "<paste-accessToken-from-login-response>"
$smokeUsername = "cashier-{0}" -f [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$smokeEmail = "$smokeUsername@demo-shop.local"
$assigneeId = 2
# $assigneeId = 2 assumes a fresh local database with the seeded `ops` user.
# If your local data has drifted, paste the current tenant `ops` id from GET /api/v1/users instead.
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/user/me
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/context
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/roles
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/users?page=0&size=10"
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/tickets?page=0&size=10"
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/users?page=0&size=10&username=ad&status=ACTIVE&roleCode=TENANT_ADMIN"
$createBody = @{ username = $smokeUsername; displayName = "Cashier User"; email = $smokeEmail; password = "123456"; roleCodes = @("READ_ONLY") } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $createBody http://localhost:8080/api/v1/users
$newUserId = "<paste-id-from-create-response>"
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
$smokeToken = "<paste-accessToken-from-smoke-user-login-response>"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/users/$newUserId
$updateBody = @{ displayName = "Cashier User Updated"; email = "$smokeUsername.updated@demo-shop.local" } | ConvertTo-Json -Compress
curl.exe -i -X PUT -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $updateBody http://localhost:8080/api/v1/users/$newUserId
$assignRolesBody = @{ roleCodes = @("TENANT_ADMIN") } | ConvertTo-Json -Compress
curl.exe -i -X PUT -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $assignRolesBody http://localhost:8080/api/v1/users/$newUserId/roles
curl.exe -i -H "Authorization: Bearer $smokeToken" http://localhost:8080/api/v1/context
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
$refreshedToken = "<paste-accessToken-from-role-refresh-login-response>"
curl.exe -i -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/rbac/users/manage
curl.exe -i -X POST -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/users/$newUserId/disable-requests
$approvalRequestId = "<paste-id-from-disable-request-response>"
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/approval-requests?page=0&size=10"
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/approval-requests?page=0&size=10&status=PENDING&actionType=USER_STATUS_DISABLE&requestedBy=$newUserId"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/approval-requests/$approvalRequestId
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/audit-events?entityType=APPROVAL_REQUEST&entityId=$approvalRequestId"
$ticketBody = @{ title = "$smokeUsername POS register frozen"; description = "Register screen froze during checkout." } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $ticketBody http://localhost:8080/api/v1/tickets
$ticketId = "<paste-id-from-ticket-create-response>"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/tickets/$ticketId
$ticketAssignBody = @{ assigneeId = $assigneeId } | ConvertTo-Json -Compress
curl.exe -i -X PATCH -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $ticketAssignBody http://localhost:8080/api/v1/tickets/$ticketId/assignee
$ticketStatusBody = @{ status = "IN_PROGRESS" } | ConvertTo-Json -Compress
curl.exe -i -X PATCH -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $ticketStatusBody http://localhost:8080/api/v1/tickets/$ticketId/status
$ticketCommentBody = @{ content = "Investigating store terminal logs." } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $ticketCommentBody http://localhost:8080/api/v1/tickets/$ticketId/comments
$ticketCloseBody = @{ status = "CLOSED" } | ConvertTo-Json -Compress
curl.exe -i -X PATCH -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $ticketCloseBody http://localhost:8080/api/v1/tickets/$ticketId/status
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/tickets/$ticketId
$ticketReopenBody = @{ status = "OPEN" } | ConvertTo-Json -Compress
curl.exe -i -X PATCH -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $ticketReopenBody http://localhost:8080/api/v1/tickets/$ticketId/status
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/tickets/$ticketId
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/audit-events?entityType=ticket&entityId=$ticketId"
curl.exe -i -X POST -H "Authorization: Bearer $token" http://localhost:8080/api/v1/approval-requests/$approvalRequestId/approve
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
curl.exe -i -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/context
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/audit-events?entityType=USER&entityId=$newUserId"
$badRoleBody = @{ username = "$smokeUsername-bad-role"; displayName = "Cashier User"; email = "$smokeUsername-bad-role@demo-shop.local"; password = "123456"; roleCodes = @("OTHER_ONLY") } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $badRoleBody http://localhost:8080/api/v1/users
$importCsvPath = Join-Path $env:TEMP "$smokeUsername-import.csv"
Set-Content -Path $importCsvPath -Value "username,email`n$smokeUsername-import,$smokeUsername-import@demo-shop.local"
curl.exe -i -X POST -H "Authorization: Bearer $token" `
  -F "request={\"importType\":\"USER_CSV\"};type=application/json" `
  -F "file=@$importCsvPath;type=text/csv" `
  http://localhost:8080/api/v1/import-jobs
$importJobId = "<paste-id-from-import-create-response>"
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/import-jobs?page=0&size=10"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/import-jobs/$importJobId
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/audit-events?entityType=IMPORT_JOB&entityId=$importJobId"
Remove-Item $importCsvPath
```

Expected results:

- `GET /api/v1/roles` returns only current-tenant role options
- `GET /api/v1/users` returns a page object for the current tenant only
- `GET /api/v1/tickets` returns a page object for the current tenant only
- `GET /api/v1/users/{id}` returns one current-tenant user and includes current `roleCodes`
- `GET /api/v1/tickets/{id}` returns one current-tenant ticket with comments and workflow logs
- `POST /api/v1/users` with the admin token creates an `ACTIVE` user whose password works immediately for login
- `POST /api/v1/tickets` creates an `OPEN` ticket in the current tenant
- `PATCH /api/v1/tickets/{id}/assignee` assigns the ticket to the current-tenant `ops` handler identified by `ASSIGNEE_ID`
- `PATCH /api/v1/tickets/{id}/status` enforces the current lifecycle including reopen (`OPEN -> IN_PROGRESS`, `OPEN -> CLOSED`, `IN_PROGRESS -> CLOSED`, and `CLOSED -> OPEN`)
- the second ticket detail read shows the reopened `OPEN` state after the `CLOSED -> OPEN` request
- `POST /api/v1/tickets/{id}/comments` appends a comment and the detail view shows it
- `GET /api/v1/audit-events?entityType=ticket&entityId=<ticketId>` returns current-tenant audit rows for the ticket workflow writes and accepts lower-case `entityType`
- `PUT /api/v1/users/{id}` updates only `displayName` and `email`
- `PATCH /api/v1/users/{id}/status` accepts only `ACTIVE` or `DISABLED`
- `PUT /api/v1/users/{id}/roles` clears old roles and writes the new role set
- `POST /api/v1/users/{id}/disable-requests` creates a `PENDING` request and leaves the target user `ACTIVE` before review
- `GET /api/v1/approval-requests?page=0&size=10` returns a tenant-scoped page object for approval requests
- `GET /api/v1/approval-requests?...&requestedBy=<newUserId>` returns the newly created disable request for the smoke user
- `GET /api/v1/approval-requests/{id}` returns the tenant-scoped approval request detail
- `GET /api/v1/audit-events?entityType=APPROVAL_REQUEST&entityId=<approvalRequestId>` returns approval-request audit rows such as `APPROVAL_REQUEST_CREATED`
- `POST /api/v1/approval-requests/{id}/approve` transitions the request to `APPROVED` and then disables the target user
- logging in with the new smoke user succeeds before the disable call
- if you captured a token for that user before changing roles, reusing that old token on a protected endpoint should now return `403` with `token claims are stale, please login again`
- after role reassignment, logging in again should return a new token whose RBAC access matches the new roles
- logging in after approval returns `403` because the user is no longer `ACTIVE`
- if you captured a token for that user before approval, reusing that old token on a protected endpoint should now return `403` with `user is not active`
- `GET /api/v1/audit-events?entityType=USER&entityId=<newUserId>` returns current-tenant audit rows for create, profile update, role reassignment, and disable
- `POST /api/v1/users` with a role code outside the current tenant returns `400`
- `POST /api/v1/import-jobs` returns a tenant-scoped `QUEUED` job and `GET /api/v1/import-jobs?page=0&size=10` includes it
- `GET /api/v1/import-jobs/{id}` returns current parse-level counters and any current item errors for the created job
- after the worker runs, the import job transitions to `SUCCEEDED` or `FAILED` and `GET /api/v1/audit-events?entityType=IMPORT_JOB&entityId=<importJobId>` should show `IMPORT_JOB_CREATED`, `IMPORT_JOB_PROCESSING_STARTED`, and a terminal import action

Use a fresh generated username on each run so the smoke flow stays repeatable against a persistent local database.

Ticket negative-path note:

- This smoke flow keeps ticket verification to the happy path.
- Cross-tenant assignee rejection, illegal or no-op status-transition rejection, and viewer write denial are covered in automated tests and should be checked manually through [regression-checklist.md](regression-checklist.md) when needed.

Password regression note:

- The current rule rejects passwords that start or end with whitespace. If password handling changed, verify that `POST /api/v1/auth/login` and `POST /api/v1/users` both return `400 VALIDATION_ERROR` for a password like `" 123456 "`.
- After you create a smoke user, reuse the returned `id` for the role and status calls, capture one token before role reassignment, and capture a refreshed token before disabling the user.

## 4. Check RBAC Demo Endpoints

```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/rbac/users
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/rbac/users/manage
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/rbac/feature-flags
```

PowerShell:

```powershell
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/rbac/users
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/rbac/users/manage
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/rbac/feature-flags
```

Use `ops` or `viewer` to confirm permission-denied behavior on endpoints they should not access.

## 5. Clean Up Smoke Data

If you created local smoke users against the persistent development database, remove them after the check. Delete `user_role` rows first, then `users` rows.

```sql
DELETE tol
FROM ticket_operation_log tol
JOIN ticket t ON t.id = tol.ticket_id
WHERE t.title LIKE 'cashier-% POS register frozen';

DELETE tc
FROM ticket_comment tc
JOIN ticket t ON t.id = tc.ticket_id
WHERE t.title LIKE 'cashier-% POS register frozen';

DELETE FROM ticket
WHERE title LIKE 'cashier-% POS register frozen';

DELETE ur
FROM user_role ur
JOIN users u ON u.id = ur.user_id
WHERE u.username LIKE 'smoke%'
   OR u.username LIKE 'cashier-%';

DELETE FROM users
WHERE username LIKE 'smoke%'
   OR username LIKE 'cashier-%';
```

If you use a different prefix for generated test users, update the cleanup filter accordingly.

You can also run the same requests from [../../api-demo.http](../../api-demo.http) in an IDE that supports `.http` request files.
