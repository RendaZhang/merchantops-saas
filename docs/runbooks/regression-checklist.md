# Regression Checklist

Last updated: 2026-03-10

Use this checklist after foundation-level changes, security changes, environment changes, or current user-management API changes.

## Infra

- [ ] `docker compose up -d` works
- [ ] MySQL is reachable
- [ ] Redis `PING` returns `PONG`
- [ ] RabbitMQ UI is accessible

## Application

- [ ] application starts successfully
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

- [ ] Swagger `User Management` tag shows only `GET /api/v1/users`
- [ ] `GET /api/v1/users` returns a page object rather than a bare array
- [ ] `GET /api/v1/users?page=0&size=10` works
- [ ] `GET /api/v1/users?username=ad` filters by username
- [ ] `GET /api/v1/users?status=ACTIVE` filters by status
- [ ] `GET /api/v1/users?roleCode=TENANT_ADMIN` filters by role code
- [ ] each `/api/v1/users` item includes `id`, `username`, `displayName`, `email`, and `status`
- [ ] `/api/v1/users` response includes `items`, `page`, `size`, `total`, and `totalPages`
- [ ] `GET /api/v1/users` returns the seeded `admin`, `ops`, and `viewer` users for tenant `demo-shop` when filters allow

## Tools

- Use [../../api-demo.http](../../api-demo.http) for the main request flow
- Compare the current user-list behavior against [../reference/user-management.md](../reference/user-management.md)
- Use [local-smoke-test.md](local-smoke-test.md) when you want a shorter step-by-step validation path
