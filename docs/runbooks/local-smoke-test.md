# Local Smoke Test

## 0. Run Automated Tests First

Preferred command:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

Run the manual smoke flow below after the automated checks pass. See [automated-tests.md](automated-tests.md) for current unit-test coverage and command selection notes.

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
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/user/me
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/context
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/users?page=0&size=10"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/users?page=0&size=10&username=ad&status=ACTIVE&roleCode=TENANT_ADMIN"
```

PowerShell:

```powershell
$token = "<paste-accessToken-from-login-response>"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/user/me
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/context
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/users?page=0&size=10"
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/users?page=0&size=10&username=ad&status=ACTIVE&roleCode=TENANT_ADMIN"
```

`GET /api/v1/users` should return a page object for the current tenant only.

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

You can also run the same requests from [../../api-demo.http](../../api-demo.http) in an IDE that supports `.http` request files.
