# Regression Checklist

Last updated: 2026-03-10

Use this checklist after foundation-level changes, security changes, environment changes, or current user-management API changes.

## Automated

- [ ] `.\mvnw.cmd -pl merchantops-api -am test` passes
- [ ] `AuthSecurityIntegrationTest`, `UserQueryServiceTest`, `UserCommandServiceTest`, and `UserManagementControllerTest` cover the code path you changed
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

## User Management

- [ ] Swagger `User Management` tag shows `GET /api/v1/users`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles`
- [ ] Swagger `Role Management` tag shows `GET /api/v1/roles`
- [ ] `GET /api/v1/users` returns a page object rather than a bare array
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
- [ ] a `DISABLED` user is rejected by `POST /api/v1/auth/login`
- [ ] a token issued before a user was disabled is rejected on protected endpoints with `403` / `user is not active`
- [ ] `PUT /api/v1/users/{id}/roles` replaces the old role set rather than appending to it
- [ ] `PUT /api/v1/users/{id}/roles` rejects role codes outside the current tenant
- [ ] a token issued before role or permission changes is rejected on protected endpoints with `403` / `token claims are stale, please login again`
- [ ] after role reassignment, the affected user can log in again and the new token reflects the new RBAC access
- [ ] password edge cases, especially leading and trailing whitespace, behave consistently between create and login
- [ ] manual smoke users were cleaned from the local database after verification

## Tools

- Use [automated-tests.md](automated-tests.md) for the fastest regression command and coverage scope
- Use [../../api-demo.http](../../api-demo.http) for the main request flow
- Compare the current user-list behavior against [../reference/user-management.md](../reference/user-management.md)
- Use [local-smoke-test.md](local-smoke-test.md) when you want a shorter step-by-step validation path
