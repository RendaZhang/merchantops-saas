# Admin Console

The admin console is the first product-facing frontend entry for the Productization Baseline. It lives in `merchantops-admin-web/` as a standalone Vite + React app and talks to the existing backend through the Vite dev proxy.

The current Productization Baseline frontend is intentionally narrow:

- login with the seeded tenant admin account
- restore the local token after refresh
- fetch current tenant and operator context
- show JWT role and permission claims for display
- sign out through the backend auth-session revocation endpoint
- expose navigation placeholders for Tickets, Approvals, Imports, AI Interactions, and Feature Flags

It does not add business workflow screens.

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

## Start The Admin Console

In a second terminal:

```powershell
cd merchantops-admin-web
npm install
npm run dev
```

Open `http://localhost:5173`.

The frontend calls `/api/v1/auth/login`, `/api/v1/context`, and `/api/v1/auth/logout` with relative `/api/...` paths. During local development, Vite proxies those calls to `http://localhost:8080`.

## Smoke Test

1. Open `http://localhost:5173`.
2. Log in with tenant `demo-shop`, username `admin`, and password `123456`.
3. Confirm the dashboard shows tenant code, tenant ID, operator, operator ID, token roles, and token permissions.
4. Confirm the sidebar lists Tickets, Approvals, Imports, AI Interactions, and Feature Flags as placeholders.
5. Refresh the page and confirm the dashboard reloads context without returning to login.
6. Select `Sign out` and confirm the app returns to the login screen.
7. Reusing the signed-out token against `/api/v1/context` should return `401`.

## Current Session Limits

The frontend stores the JWT access token in `localStorage` for this local baseline. It clears that token when it expires locally or when `/api/v1/context` returns `401` or `403`.

Login creates a server-side auth session and the JWT carries a required `sid` claim. `Sign out` calls `POST /api/v1/auth/logout`, revokes only the current session, clears the local token, clears the context query cache, and returns to login even if the logout request fails or the token is already invalid.

There is still no refresh-token flow, cookie/session rotation, logout-all-devices flow, or session cleanup scheduler in this slice. When the access token expires or the server-side session is invalid, the user must log in again.

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
