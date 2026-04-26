# Admin Console Architecture

The admin console is a standalone frontend module at `merchantops-admin-web/`. It is not served from Spring Boot static resources. The current Productization Baseline surface uses the existing login/context APIs, current-session logout, current-user logout-all, the existing read-only ticket queue API, and the existing feature-flag list/update API.

Productization Baseline Slice C defines the production-like runtime boundary: the built admin app is served by an Nginx container, and that container proxies same-origin `/api/...` requests to the API container.

## Module Boundary

- `merchantops-admin-web/` owns the Vite React app, npm dependencies, frontend build, and frontend documentation.
- `merchantops-api/` remains the backend API and Swagger source of truth.
- Local development uses the Vite dev proxy to forward `/api/...` requests to `http://localhost:8080`.
- Production-like local runtime uses `merchantops-admin-web/Dockerfile` plus Nginx on `http://localhost:8081`.
- Runtime compose keeps API and admin containers on the same `merchantops-infra` network.

## Frontend Runtime Shape

- React Router owns the login route plus the protected Dashboard, Tickets, and Feature Flags routes.
- The shared authenticated layout owns the app shell, current context query, sign-out mutations, and auth-expired redirect behavior for protected child routes.
- TanStack Query owns the authenticated `/api/v1/context`, `/api/v1/tickets`, and `/api/v1/feature-flags` fetch and refresh behavior.
- `src/lib/api-client.ts` is the only fetch boundary for backend calls.
- `src/lib/auth-token.ts` is the only local token persistence boundary.
- Zod validates login, context, ticket page, ticket list item, feature-flag list/update, and JWT display-claim shapes before the UI consumes them.

## Runtime Host Model

- Development origin: `http://localhost:5173`
- Production-like origin: `http://localhost:8081`
- API debug/health origin: `http://localhost:8080`
- Browser-facing API base: relative `/api/...`

In production-like runtime, Nginx serves the static `dist/` files and proxies `/api/...` to `http://merchantops-api:8080` without path rewriting. This keeps the browser on one origin and avoids introducing CORS in this slice.

The API runtime profile is `runtime`, not `dev`. Runtime secrets and credentials are injected through compose or the deployment environment; they are not baked into either image.

## Current Backend Contract Use

The current frontend calls only:

- `POST /api/v1/auth/login`
- `GET /api/v1/context`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all`
- `GET /api/v1/tickets?page=0&size=10`
- `GET /api/v1/feature-flags`
- `PUT /api/v1/feature-flags/{key}`

`/api/v1/context` is authoritative for the current tenant and operator identity displayed on the dashboard.

The dashboard also decodes role and permission claims from the JWT for display only. Client-decoded claims are not an authorization source. Backend authorization remains enforced by Spring Security, request-time JWT revalidation, and endpoint permissions.

The `/tickets` route renders the first page of the current tenant ticket queue as read-only data. It does not add ticket detail, filters, pagination controls, assignment, status changes, comments, AI actions, approval actions, or new backend APIs.

The `/feature-flags` route renders the current tenant's fixed feature-flag inventory and lets authorized users toggle one flag at a time through the existing update endpoint. It does not add cross-tenant administration, percentage rollout, environment policy, batch editing, audit detail, AI provider configuration, or new backend APIs. Generic permission `403` responses render an in-page `权限不足` state instead of clearing the local session.

`GET /api/v1/user/me` already exists and returns roles and permissions, but it is intentionally not used yet; the frontend keeps operator context on `/api/v1/context` and relies on backend authorization for ticket and feature-flag access.

## Session Boundary

The frontend stores the access token in `localStorage` under `merchantops.admin.auth.v1`, together with a client-side expiry timestamp derived from the login response `expiresIn`.

Refresh restores the token and refetches `/api/v1/context`. Protected route data requests such as `/api/v1/context`, `/api/v1/tickets`, and `/api/v1/feature-flags` clear the local token and send the user back to login on expired local sessions, invalid stored sessions, `401`, or the current auth-ending `403` responses `tenant is not active`, `user is not active`, and `token claims are stale, please login again`. A generic permission `403` is not treated as session expiry.

Login creates a backend `auth_session` row. The JWT carries a required `sid` claim, and protected backend requests validate that the session exists, belongs to the same tenant/user, is `ACTIVE`, is not revoked, and has not expired before current tenant/user/role revalidation runs.

A background server-side cleanup scheduler now prunes only retention-aged expired `ACTIVE` sessions and retention-aged `REVOKED` sessions. This does not change the frontend contract: restore still depends on the stored access token plus a successful `/api/v1/context` refetch, and old tokens whose session rows were later deleted still fail through the same controlled `401` path.

`Sign out` calls `POST /api/v1/auth/logout`, then clears the local token plus context, tickets, and feature-flags caches regardless of logout success, network failure, or `401` / `403`. Logout revokes only the current session; separate logins remain active.

`Sign out all sessions` calls `POST /api/v1/auth/logout-all`. On success, the backend revokes every active session for the same current tenant/user, including the caller's current session, while preserving other users and other tenants; the frontend then clears the same local token plus context, tickets, and feature-flags query caches. If the request fails, the frontend still clears the local token and returns to login with a warning that other sessions may still be active.

Backend refresh tokens, cookies, token rotation, session lists, device metadata, selective device logout, and cross-origin CORS policy remain deferred to later productization slices.

## Deferred Screens

The shell includes placeholders for:

- Approvals
- Imports
- AI Interactions

Those pages should be added as narrow workflow slices that call the existing public APIs and keep AI write execution behind the current proposal and approval flows. Ticket detail, ticket mutations, ticket filters, pagination controls, and deeper feature-flag platform scope also remain later slices.
