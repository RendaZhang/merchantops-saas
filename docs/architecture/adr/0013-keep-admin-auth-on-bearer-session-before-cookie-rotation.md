# ADR-0013: Keep Admin Auth On Bearer Sessions Before Cookie Rotation

- Status: Accepted
- Date: 2026-05-18

## Context

The Productization Baseline already has a server-side `auth_session` foundation:

- `POST /api/v1/auth/login` creates an `ACTIVE` auth-session row
- JWT access tokens carry a required `sid` claim
- protected requests validate the matching server-side session before tenant, user, role, and permission revalidation
- `POST /api/v1/auth/logout` revokes only the current session
- `POST /api/v1/auth/logout-all` revokes all active sessions for the current tenant and user
- a cleanup scheduler prunes retention-aged expired or revoked rows
- the admin console stores the bearer access token in `localStorage` and clears it on local expiry, `401`, or auth-ending `403` responses

The same-origin admin runtime now makes cookie-based auth technically possible, but moving directly to refresh tokens, HttpOnly cookies, token rotation, CSRF handling, and selective session management would combine several separate security and UX decisions into one large slice.

That would make the next step too broad for the current Productization Baseline. It would also blur the current public contract, which intentionally evolves in narrow Swagger-visible auth slices instead of combining token transport and session-management work.

## Decision

For the current Productization Baseline, MerchantOps SaaS will keep the admin auth contract on bearer access tokens plus server-side `auth_session` validation.

At this decision point, refresh tokens, HttpOnly cookie auth, access-token rotation, CSRF protection, device metadata, per-session device logout, and sign-out-other-sessions were deferred until selected as dedicated slices.

The first follow-up auth-lifecycle implementation is a current-user session inventory before any token transport or rotation change. That inventory makes the existing server-side session model visible enough for operators to reason about active sessions, current-session identity, and revocation behavior without changing the login response shape or browser storage model.

Update on 2026-05-23: Slice G-C2 selected the sign-out-other-sessions part of that deferred set as a narrow logout-other-sessions contract. `POST /api/v1/auth/logout-others` now revokes other active sessions for the same current tenant/user while preserving the caller's current session. This does not change the bearer-token decision and still leaves refresh tokens, HttpOnly cookies, access-token rotation, CSRF protection, device metadata, and per-session device logout deferred.

Update on 2026-05-24: [ADR-0014](0014-per-session-session-management-boundary.md) keeps per-session revocation deferred. It requires any future per-session revoke slice to define an opaque public handle plus device metadata, privacy, display, retention, and failure semantics before changing the current `/sessions` contract.

Any later move to cookies or refresh tokens must be handled as a separate architecture decision and implementation slice. That later decision must explicitly cover:

- SameSite policy
- CSRF protection
- CORS impact
- logout and logout-all semantics
- token lifetime and rotation rules
- admin-console fallback wording when backend revocation cannot be confirmed
- compatibility with Swagger, `api-demo.http`, and local smoke flows

## Consequences

- the current public auth surface stays narrow: `POST /api/v1/auth/login`, `GET /api/v1/auth/sessions`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/logout-all`, and `POST /api/v1/auth/logout-others`
- the admin console continues to use relative `/api/...` calls and bearer authorization headers
- the server-side `auth_session` table remains the source of revocation truth for active, revoked, expired, and cleanup-deleted sessions
- session visibility and logout-other-sessions are implemented without taking on refresh-token or cookie-auth risk
- cookie/session rotation remains available as a future hardening step, but it should not be bundled with per-session device logout, per-session revoke handles, or unrelated admin-console workflow depth
- docs and runbooks must continue to state that no refresh-token flow, cookie/session rotation, device metadata, stable public session handle, or per-session device logout flow is currently implemented
