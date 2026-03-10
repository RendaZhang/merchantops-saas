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
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/user/me
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/context
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/roles
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/users?page=0&size=10"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/users?page=0&size=10&username=ad&status=ACTIVE&roleCode=TENANT_ADMIN"
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"username\":\"$SMOKE_USERNAME\",\"displayName\":\"Cashier User\",\"email\":\"$SMOKE_EMAIL\",\"password\":\"123456\",\"roleCodes\":[\"READ_ONLY\"]}" \
  http://localhost:8080/api/v1/users
NEW_USER_ID=<paste-id-from-create-response>
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$SMOKE_USERNAME\",\"password\":\"123456\"}"
SMOKE_TOKEN=<paste-accessToken-from-smoke-user-login-response>
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
curl -i -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"status\":\"DISABLED\"}" \
  http://localhost:8080/api/v1/users/$NEW_USER_ID/status
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$SMOKE_USERNAME\",\"password\":\"123456\"}"
curl -i -H "Authorization: Bearer $REFRESHED_TOKEN" http://localhost:8080/api/v1/context
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"username\":\"$SMOKE_USERNAME-bad-role\",\"displayName\":\"Cashier User\",\"email\":\"$SMOKE_USERNAME-bad-role@demo-shop.local\",\"password\":\"123456\",\"roleCodes\":[\"OTHER_ONLY\"]}" \
  http://localhost:8080/api/v1/users
```

PowerShell:

```powershell
$token = "<paste-accessToken-from-login-response>"
$smokeUsername = "cashier-{0}" -f [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$smokeEmail = "$smokeUsername@demo-shop.local"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/user/me
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/context
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/roles
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/users?page=0&size=10"
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/users?page=0&size=10&username=ad&status=ACTIVE&roleCode=TENANT_ADMIN"
$createBody = @{ username = $smokeUsername; displayName = "Cashier User"; email = $smokeEmail; password = "123456"; roleCodes = @("READ_ONLY") } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $createBody http://localhost:8080/api/v1/users
$newUserId = "<paste-id-from-create-response>"
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
$smokeToken = "<paste-accessToken-from-smoke-user-login-response>"
$updateBody = @{ displayName = "Cashier User Updated"; email = "$smokeUsername.updated@demo-shop.local" } | ConvertTo-Json -Compress
curl.exe -i -X PUT -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $updateBody http://localhost:8080/api/v1/users/$newUserId
$assignRolesBody = @{ roleCodes = @("TENANT_ADMIN") } | ConvertTo-Json -Compress
curl.exe -i -X PUT -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $assignRolesBody http://localhost:8080/api/v1/users/$newUserId/roles
curl.exe -i -H "Authorization: Bearer $smokeToken" http://localhost:8080/api/v1/context
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
$refreshedToken = "<paste-accessToken-from-role-refresh-login-response>"
curl.exe -i -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/rbac/users/manage
$statusBody = @{ status = "DISABLED" } | ConvertTo-Json -Compress
curl.exe -i -X PATCH -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $statusBody http://localhost:8080/api/v1/users/$newUserId/status
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
curl.exe -i -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/context
$badRoleBody = @{ username = "$smokeUsername-bad-role"; displayName = "Cashier User"; email = "$smokeUsername-bad-role@demo-shop.local"; password = "123456"; roleCodes = @("OTHER_ONLY") } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $badRoleBody http://localhost:8080/api/v1/users
```

Expected results:

- `GET /api/v1/roles` returns only current-tenant role options
- `GET /api/v1/users` returns a page object for the current tenant only
- `POST /api/v1/users` with the admin token creates an `ACTIVE` user whose password works immediately for login
- `PUT /api/v1/users/{id}` updates only `displayName` and `email`
- `PATCH /api/v1/users/{id}/status` accepts only `ACTIVE` or `DISABLED`
- `PUT /api/v1/users/{id}/roles` clears old roles and writes the new role set
- logging in with the new smoke user succeeds before the disable call
- if you captured a token for that user before changing roles, reusing that old token on a protected endpoint should now return `403` with `token claims are stale, please login again`
- after role reassignment, logging in again should return a new token whose RBAC access matches the new roles
- logging in after the disable call returns `403` because the user is no longer `ACTIVE`
- if you captured a token for that user before disabling it, reusing that old token on a protected endpoint should now return `403` with `user is not active`
- `POST /api/v1/users` with a role code outside the current tenant returns `400`

Use a fresh generated username on each run so the smoke flow stays repeatable against a persistent local database.

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
