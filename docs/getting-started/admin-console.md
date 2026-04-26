# Admin Console

The admin console is the first product-facing frontend entry for the Productization Baseline. It lives in `merchantops-admin-web/` as a standalone Vite + React app.

There are two supported local paths:

- local development: Vite serves the app at `http://localhost:5173` and proxies `/api/...` to `http://localhost:8080`
- production-like runtime: Nginx serves the built app at `http://localhost:8081` and proxies same-origin `/api/...` to the API container

The current Productization Baseline frontend is intentionally narrow:

- login with the seeded tenant admin account
- restore the local token after refresh
- fetch current tenant and operator context
- show JWT role and permission claims for display
- sign out through the backend auth-session revocation endpoint
- sign out all current-user sessions through the backend bulk revocation endpoint
- render the first read-only workflow screen at `/tickets` using the current tenant ticket queue
- render the Feature Flags control screen at `/feature-flags` using the current tenant feature-flag API
- expose navigation placeholders for Approvals, Imports, and AI Interactions

It does not add ticket detail, mutations, filters, pagination controls, AI actions, approval actions, cross-tenant feature-flag administration, percentage rollout, batch flag editing, audit detail, AI provider configuration, or backend API changes.

## Prerequisites

- JDK 21
- Docker Desktop or another Docker Engine runtime with `docker compose`
- Node.js and npm
- Access to Maven Central and npm registry

## Start The Backend

From the repository root:

```powershell
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd -pl merchantops-api -am -DskipTests install
.\mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

The API should be available at `http://localhost:8080`.

## Start The Admin Console For Local Development

In a second terminal:

```powershell
cd merchantops-admin-web
npm install
npm run dev
```

Open `http://localhost:5173`.

The frontend calls `/api/v1/auth/login`, `/api/v1/context`, `/api/v1/auth/logout`, `/api/v1/auth/logout-all`, `/api/v1/tickets?page=0&size=10`, `/api/v1/feature-flags`, and `/api/v1/feature-flags/{key}` with relative `/api/...` paths. During local development, Vite proxies those calls to `http://localhost:8080`.

## Start The Production-Like Admin Runtime

From the repository root:

```powershell
Copy-Item .env.example .env -ErrorAction SilentlyContinue
docker compose -f docker-compose.yml -f docker-compose.runtime.yml up -d --build
```

Open `http://localhost:8081`.

This path does not use the Vite dev server. The admin image serves `dist/` through Nginx, and Nginx proxies same-origin `/api/...` requests to `merchantops-api:8080` inside the compose network.

The API container uses `SPRING_PROFILES_ACTIVE=runtime`. Required secrets and credentials, including `JWT_SECRET`, must be injected through `.env` or the runtime environment; they are not baked into the image.

## Local Development Smoke Test

1. Open `http://localhost:5173`.
2. Log in with tenant `demo-shop`, username `admin`, and password `123456`.
3. Confirm the dashboard shows tenant code, tenant ID, operator, operator ID, token roles, and token permissions.
4. Select `Tickets` and confirm `/tickets` renders the current tenant queue from `/api/v1/tickets?page=0&size=10`.
5. Select `Feature Flags` and confirm `/feature-flags` renders eight current-tenant flags from `/api/v1/feature-flags`.
6. Toggle one flag, confirm the row reflects the persisted state returned by `PUT /api/v1/feature-flags/{key}`, and restore the original value before ending the smoke.
7. Sign out, log in as `ops` or `viewer`, open `/feature-flags`, and confirm the page shows `权限不足` without returning to login.
8. Confirm Approvals, Imports, and AI Interactions remain disabled placeholders.
9. Refresh `/tickets` and `/feature-flags` and confirm context plus route data reload without returning to login while the session is active.
10. Select `Sign out` and confirm the app returns to the login screen.
11. Log in again, select `Sign out all sessions`, and confirm the app returns to the login screen.
12. Reusing a signed-out token against `/api/v1/context` should return `401`.

The seeded `admin`, `ops`, and `viewer` users all have `TICKET_READ` and can load the read-only queue.

## Production-Like Runtime Smoke Test

Use [../runbooks/deployment-runtime-smoke-test.md](../runbooks/deployment-runtime-smoke-test.md) for the same-origin admin + API runtime smoke.

Minimum acceptance:

1. Open `http://localhost:8081`.
2. Log in with tenant `demo-shop`, username `admin`, and password `123456`.
3. Confirm dashboard context is loaded through same-origin `/api/v1/context`.
4. Open `Tickets` and confirm the queue loads through same-origin `/api/v1/tickets?page=0&size=10`.
5. Open `Feature Flags` and confirm the eight-key inventory loads through same-origin `/api/v1/feature-flags`.
6. Toggle one flag through the UI and restore the original value.
7. Refresh `/tickets` and `/feature-flags` and confirm context restores while the server-side session is active.
8. Select `Sign out` and confirm the app returns to login.
9. Log in again, select `Sign out all sessions`, and confirm the app returns to login.
10. Reusing a signed-out token against `http://localhost:8081/api/v1/context` should return `401`.

## Current Session Limits

The frontend stores the JWT access token in `localStorage` for this baseline. It clears that token when it expires locally or when `/api/v1/context`, `/api/v1/tickets`, or `/api/v1/feature-flags` returns `401` or one of the current auth-ending `403` messages: `tenant is not active`, `user is not active`, or `token claims are stale, please login again`. A generic permission `403` does not clear the local session; `/feature-flags` shows an in-page `权限不足` state for ordinary permission denial.

Login creates a server-side auth session and the JWT carries a required `sid` claim. `Sign out` calls `POST /api/v1/auth/logout`, revokes only the current session, clears the local token, clears the context, tickets, and feature-flags query caches, and returns to login even if the logout request fails or the token is already invalid.

`Sign out all sessions` calls `POST /api/v1/auth/logout-all`. On success, the backend revokes every active session for the same current tenant/user, then the frontend clears the same local token plus context, tickets, and feature-flags query caches. Other users and other tenants are unaffected. If the request fails, the frontend still clears the local token and returns to login, but it warns that other sessions may still be active.

A background auth-session cleanup scheduler now prunes retention-aged expired `ACTIVE` sessions and retention-aged `REVOKED` sessions on the server side without changing the frontend contract. There is still no refresh-token flow, cookie/session rotation, session list, device metadata, or selective device logout in this slice. When the access token expires or the server-side session is invalid, the user must log in again.

The production-like runtime keeps the same bearer-token model and uses same-origin reverse proxying instead of CORS.

## Verification Commands

From the repository root:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

From `merchantops-admin-web`:

```powershell
npm run typecheck
npm run lint
npm run build
```
