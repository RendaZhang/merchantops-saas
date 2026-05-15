# Admin Console Architecture

The admin console is a standalone frontend module at `merchantops-admin-web/`. It is not served from Spring Boot static resources. The current Productization Baseline surface uses the existing login/context APIs, current-session logout, current-user logout-all, the existing ticket queue/detail/comment APIs, the existing read-only import-job list/detail/error APIs, the existing approval-request list/detail/review APIs, the existing feature-flag list/update API, and the existing AI interaction usage-summary API.

Productization Baseline Slice C defines the production-like runtime boundary: the built admin app is served by an Nginx container, and that container proxies same-origin `/api/...` requests to the API container.

## Module Boundary

- `merchantops-admin-web/` owns the Vite React app, npm dependencies, frontend build, and frontend documentation.
- `merchantops-api/` remains the backend API and Swagger source of truth.
- Local development uses the Vite dev proxy to forward `/api/...` requests to `http://localhost:8080`.
- Production-like local runtime uses `merchantops-admin-web/Dockerfile` plus Nginx on `http://localhost:8081`.
- Runtime compose keeps API and admin containers on the same `merchantops-infra` network.

## Frontend Runtime Shape

- React Router owns the login route plus the protected Dashboard, Tickets, Ticket Detail, Feature Flags, Imports, Import Detail, Approvals, Approval Detail, and AI Interactions routes.
- The shared authenticated layout owns the app shell, current context query, sign-out mutations, and auth-expired redirect behavior for protected child routes.
- TanStack Query owns the authenticated `/api/v1/context`, `/api/v1/tickets`, `/api/v1/tickets/{id}`, `/api/v1/tickets/{id}/comments`, `/api/v1/import-jobs`, `/api/v1/import-jobs/{id}`, `/api/v1/import-jobs/{id}/errors`, `/api/v1/approval-requests`, `/api/v1/approval-requests/{id}`, `/api/v1/approval-requests/{id}/approve`, `/api/v1/approval-requests/{id}/reject`, `/api/v1/feature-flags`, and `/api/v1/ai-interactions/usage-summary` fetch, mutation, and refresh behavior.
- `src/lib/api-client.ts` is the only fetch boundary for backend calls.
- `src/lib/auth-token.ts` is the only local token persistence boundary.
- Zod validates login, context, ticket page/list/detail/comment/comment-create/operation-log, import-job page, import-job list item, import-job detail, import-job error page, approval-request page, approval-request list item, approval-request detail/review, feature-flag list/update, AI interaction usage-summary, and JWT display-claim shapes before the UI consumes them.

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
- `GET /api/v1/tickets/{id}`
- `POST /api/v1/tickets/{id}/comments`
- `GET /api/v1/import-jobs?page=0&size=10`
- `GET /api/v1/import-jobs/{id}`
- `GET /api/v1/import-jobs/{id}/errors?page=0&size=10`
- `GET /api/v1/approval-requests?page=0&size=10`
- `GET /api/v1/approval-requests/{id}`
- `POST /api/v1/approval-requests/{id}/approve`
- `POST /api/v1/approval-requests/{id}/reject`
- `GET /api/v1/feature-flags`
- `PUT /api/v1/feature-flags/{key}`
- `GET /api/v1/ai-interactions/usage-summary`

`/api/v1/context` is authoritative for the current tenant and operator identity displayed on the dashboard.

The dashboard also decodes role and permission claims from the JWT for display only. Client-decoded claims are not an authorization source. Backend authorization remains enforced by Spring Security, request-time JWT revalidation, and endpoint permissions.

The `/tickets` route renders the first page of the current tenant ticket queue as read-only data and links each ticket title/id to `/tickets/:id`.

The `/tickets/:id` route renders ticket title, description, status, assignee, creator, timestamps, comments, and workflow operation logs through the existing ticket detail API, and includes a plain internal comment composer through the existing ticket comment API. It validates empty and overlong comment input before network calls, clears the input after successful submit, invalidates ticket detail plus ticket list caches, shows generic `TICKET_WRITE` permission failures inline, and keeps auth-ending responses on the shared session-ended path. It does not add assignment, status changes, ticket AI actions, AI interaction-history drilldown, filters, pagination controls, approval actions, or new backend APIs.

The `/imports` route renders the first page of the current tenant import-job queue as read-only data and links each source filename to `/imports/:id`.

The `/imports/:id` route renders read-only import-job overview, counts, timing, error-code diagnostics, and the first failed-row page through the existing import detail and `/errors` APIs. It does not add upload, replay, selective replay, edited replay, import AI actions, filters, pagination controls, approval workflow UI, or new backend APIs.

The `/approvals` route renders the first page of the current tenant approval-request queue as read-only data and links each request id to `/approvals/:id`.

The `/approvals/:id` route renders approval request detail, read-only formatted `payloadJson`, and safe inline confirmation controls for pending approve/reject actions through the existing approval detail and review APIs. `Approve` synchronously executes the underlying action, while `Reject` resolves the request without execution. It does not add bulk review, filters, pagination controls, payload editing, rejection reasons, proposal creation, or new backend APIs.

The `/feature-flags` route renders the current tenant's fixed feature-flag inventory and lets authorized users toggle one flag at a time through the existing update endpoint. It does not add cross-tenant administration, percentage rollout, environment policy, batch editing, audit detail, AI provider configuration, or new backend APIs. Generic permission `403` responses render an in-page `权限不足` state instead of clearing the local session.

The `/ai-interactions` route renders current-tenant aggregate cards for `totalInteractions`, `succeededCount`, `failedCount`, `totalTokens`, and raw `totalCostMicros`, plus read-only `byInteractionType`, `byStatus`, and `byPromptVersion` breakdowns through the existing usage-summary endpoint. It does not add filters, per-request detail, ticket or import entity history drilldown, raw prompt or provider payloads, billing or ledger semantics, write actions, or new backend APIs.

`GET /api/v1/user/me` already exists and returns roles and permissions, but it is intentionally not used yet; the frontend keeps operator context on `/api/v1/context` and relies on backend authorization for ticket, import, and feature-flag access.

## Session Boundary

The frontend stores the access token in `localStorage` under `merchantops.admin.auth.v1`, together with a client-side expiry timestamp derived from the login response `expiresIn`.

Refresh restores the token and refetches `/api/v1/context`. Protected route data requests and mutations such as `/api/v1/context`, `/api/v1/tickets`, `/api/v1/tickets/{id}`, `/api/v1/tickets/{id}/comments`, `/api/v1/import-jobs`, `/api/v1/import-jobs/{id}`, `/api/v1/import-jobs/{id}/errors`, `/api/v1/approval-requests`, `/api/v1/approval-requests/{id}`, `/api/v1/approval-requests/{id}/approve`, `/api/v1/approval-requests/{id}/reject`, `/api/v1/feature-flags`, and `/api/v1/ai-interactions/usage-summary` clear the local token and send the user back to login on expired local sessions, invalid stored sessions, `401`, or the current auth-ending `403` responses `tenant is not active`, `user is not active`, and `token claims are stale, please login again`. A generic permission `403` is not treated as session expiry.

Login creates a backend `auth_session` row. The JWT carries a required `sid` claim, and protected backend requests validate that the session exists, belongs to the same tenant/user, is `ACTIVE`, is not revoked, and has not expired before current tenant/user/role revalidation runs. After a successful login, the frontend stores the new token and clears the context, ticket list/detail, import-jobs, import-job detail, import-job errors, approval request list/detail, feature-flags, and AI interaction usage-summary query caches so stale tenant data from a previous session cannot survive a user or tenant switch.

A background server-side cleanup scheduler now prunes only retention-aged expired `ACTIVE` sessions and retention-aged `REVOKED` sessions. This does not change the frontend contract: restore still depends on the stored access token plus a successful `/api/v1/context` refetch, and old tokens whose session rows were later deleted still fail through the same controlled `401` path.

`Sign out` calls `POST /api/v1/auth/logout`, then clears the local token plus context, ticket list/detail, import-jobs, import-job detail, import-job errors, approval request list/detail, feature-flags, and AI interaction usage-summary caches regardless of logout success, network failure, or `401` / `403`. Logout revokes only the current session; separate logins remain active.

`Sign out all sessions` calls `POST /api/v1/auth/logout-all`. On success, the backend revokes every active session for the same current tenant/user, including the caller's current session, while preserving other users and other tenants; the frontend then clears the same local token plus context, ticket list/detail, import-jobs, import-job detail, import-job errors, approval request list/detail, feature-flags, and AI interaction usage-summary query caches. If the request fails, the frontend still clears the local token and returns to login with a warning that other sessions may still be active.

Backend refresh tokens, cookies, token rotation, session lists, device metadata, selective device logout, and cross-origin CORS policy remain deferred to later productization slices.

## Deferred Screens

The current shell no longer includes disabled navigation placeholders. Ticket creation, assignment, status transitions, ticket filters, pagination controls, approval filters/pagination/bulk review/payload editing/rejection reasons, import upload/replay/AI actions, ticket/import AI interaction filters or per-request detail, and deeper feature-flag platform scope remain later slices.
