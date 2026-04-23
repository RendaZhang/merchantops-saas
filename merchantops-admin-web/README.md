# MerchantOps Admin Web

Vite + React admin console for the Productization Baseline.

This app is intentionally thin. The current Productization Baseline proves the frontend placement, local run path, login flow, current tenant context, token restoration, backend current-session sign-out, all-session sign-out for the current user, and the first read-only workflow screen: the current tenant tickets queue. Approvals, Imports, AI Interactions, and Feature Flags remain navigation placeholders.

## Stack

- Vite
- React
- TypeScript
- React Router
- TanStack Query
- Tailwind CSS
- Zod

## Local Run

Install dependencies:

```powershell
cd merchantops-admin-web
npm install
```

Run the dev server:

```powershell
npm run dev
```

The Vite app runs at `http://localhost:5173` by default. API calls use relative `/api/...` paths and the Vite dev proxy forwards them to `http://localhost:8080`.

## Production-Like Runtime

Build the admin image from the repository root:

```powershell
docker build -t merchantops-admin-web:local .\merchantops-admin-web
```

Run the full same-origin runtime stack:

```powershell
docker compose -f docker-compose.yml -f docker-compose.runtime.yml up -d --build
```

Open `http://localhost:8081`.

The image builds the Vite app with Node, serves `dist/` through Nginx, and proxies `/api/...` to `http://merchantops-api:8080` inside the compose network. The browser uses one origin and does not need CORS for this slice.

## Backend Prerequisite

Start the backend from the repository root before logging in:

```powershell
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd -pl merchantops-api -am -DskipTests install
.\mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

The Dockerized API path documented in `../docs/getting-started/quick-start.md` also works. For the same-origin admin + API runtime path, use `../docs/runbooks/deployment-runtime-smoke-test.md`.

## Demo Login

Use the seeded admin account:

- Tenant: `demo-shop`
- Username: `admin`
- Password: `123456`

The admin console calls:

- `POST /api/v1/auth/login`
- `GET /api/v1/context`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all`
- `GET /api/v1/tickets?page=0&size=10`

Roles and permissions displayed in the dashboard are decoded from JWT claims for operator visibility only. They are not used as an authorization source.

The Tickets route is available at `/tickets`. It renders the first page of the current tenant ticket queue as a read-only table and does not include ticket detail, pagination controls, filters, assignment, status changes, AI actions, or approval actions.

## Session Boundary

The app stores the JWT access token in `localStorage` under `merchantops.admin.auth.v1` with a client-side expiry timestamp derived from `expiresIn`.

On page refresh, the app restores the token, refetches `/api/v1/context`, and clears the session on expired, invalid, `401`, or `403` responses. Ticket queue `401` or `403` responses use the same session-ended path.

Login creates a revocable server-side auth session and the JWT carries a required `sid` claim. `Sign out` calls `POST /api/v1/auth/logout`, revokes only the current session, clears the local token, clears the context and tickets query caches, and returns to login even if the logout request fails.

`Sign out all sessions` calls `POST /api/v1/auth/logout-all`. On success, the backend revokes every active session for the same current tenant/user, and then the frontend clears the same local token and query caches. Other users and other tenants are unaffected. If the request fails, the frontend still clears the local token and returns to login, but it warns that other sessions may still be active.

A background auth-session cleanup scheduler now prunes retention-aged expired `ACTIVE` sessions and retention-aged `REVOKED` sessions on the server side without changing the frontend contract. There is still no refresh-token flow, cookie/session rotation, session list, device metadata, or selective device logout in this slice. When the access token expires or the server-side session is invalid, sign in again.

## Verification

```powershell
npm run typecheck
npm run lint
npm run build
```

Manual smoke:

1. Start the backend.
2. Start this frontend with `npm run dev`.
3. Open `http://localhost:5173`.
4. Log in with `demo-shop` / `admin` / `123456`.
5. Confirm the dashboard shows tenant, operator, token roles, and token permissions.
6. Open `Tickets` from the sidebar and confirm `/tickets` renders the current tenant queue from `/api/v1/tickets?page=0&size=10`.
7. Refresh `/tickets` and confirm context plus tickets reload while the session is active.
8. Use `Sign out` and confirm the app returns to login.
9. Log in again, use `Sign out all sessions`, and confirm the app returns to login.
10. Reusing a signed-out token against `/api/v1/context` should return `401`.

Production-like smoke uses `http://localhost:8081` instead of the Vite dev server and verifies the same `/tickets` route through the Nginx same-origin proxy. It is documented in `../docs/runbooks/deployment-runtime-smoke-test.md`.

## Image Credit

The login-page operations image is loaded from Wikimedia Commons:

- [Warehouse distribution-center-1136510.jpg](https://commons.wikimedia.org/wiki/File:Warehouse_distribution-center-1136510.jpg), by Rsherwin, licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/deed.en)
