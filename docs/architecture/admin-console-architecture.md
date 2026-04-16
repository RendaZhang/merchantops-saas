# Admin Console Architecture

The admin console is a standalone frontend module at `merchantops-admin-web/`. It is not served from Spring Boot static resources. The current Productization Baseline surface uses the existing login/context APIs plus the Slice B logout API.

Productization Baseline Slice C defines the production-like runtime boundary: the built admin app is served by an Nginx container, and that container proxies same-origin `/api/...` requests to the API container.

## Module Boundary

- `merchantops-admin-web/` owns the Vite React app, npm dependencies, frontend build, and frontend documentation.
- `merchantops-api/` remains the backend API and Swagger source of truth.
- Local development uses the Vite dev proxy to forward `/api/...` requests to `http://localhost:8080`.
- Production-like local runtime uses `merchantops-admin-web/Dockerfile` plus Nginx on `http://localhost:8081`.
- Runtime compose keeps API and admin containers on the same `merchantops-infra` network.

## Frontend Runtime Shape

- React Router owns the login route and the protected dashboard route.
- TanStack Query owns the authenticated `/api/v1/context` fetch and refresh behavior.
- `src/lib/api-client.ts` is the only fetch boundary for backend calls.
- `src/lib/auth-token.ts` is the only local token persistence boundary.
- Zod validates login, context, and JWT display-claim shapes before the UI consumes them.

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

`/api/v1/context` is authoritative for the current tenant and operator identity displayed on the dashboard.

The dashboard also decodes role and permission claims from the JWT for display only. Client-decoded claims are not an authorization source. Backend authorization remains enforced by Spring Security, request-time JWT revalidation, and endpoint permissions.

`GET /api/v1/user/me` already exists and returns roles and permissions, but it is intentionally not used yet to keep the first frontend call boundary to login, context, and logout.

## Session Boundary

The frontend stores the access token in `localStorage` under `merchantops.admin.auth.v1`, together with a client-side expiry timestamp derived from the login response `expiresIn`.

Refresh restores the token and refetches `/api/v1/context`. Expired local sessions, invalid stored sessions, `401`, and `403` responses clear the local token and send the user back to login.

Login creates a backend `auth_session` row. The JWT carries a required `sid` claim, and protected backend requests validate that the session exists, belongs to the same tenant/user, is `ACTIVE`, is not revoked, and has not expired before current tenant/user/role revalidation runs.

`Sign out` calls `POST /api/v1/auth/logout`, then clears the local token and context cache regardless of logout success, network failure, or `401` / `403`. Logout revokes only the current session; separate logins remain active.

Backend refresh tokens, cookies, token rotation, logout-all-devices, cross-origin CORS policy, and session cleanup scheduling remain deferred to later productization slices.

## Deferred Screens

The shell includes placeholders for:

- Tickets
- Approvals
- Imports
- AI Interactions
- Feature Flags

Those pages should be added as narrow workflow slices that call the existing public APIs and keep AI write execution behind the current proposal and approval flows.
