# ADR-0014: Defer Per-Session Session Management Until Metadata Boundary Is Defined

- Status: Accepted
- Date: 2026-05-24

## Context

The Productization Baseline now has a narrow current-user session-management surface:

- `POST /api/v1/auth/login` creates a server-side `auth_session` row
- JWT access tokens carry a required `sid` claim
- protected requests validate the matching active server-side session before tenant, user, role, and permission revalidation
- `POST /api/v1/auth/logout` revokes only the current session
- `POST /api/v1/auth/logout-all` revokes all active sessions for the current tenant and user
- `GET /api/v1/auth/sessions` returns a read-only current-user inventory with current-session marking and computed status
- `POST /api/v1/auth/logout-others` revokes other active sessions for the current tenant and user while preserving the current session
- the admin `/sessions` route exposes the inventory plus the bulk sign-out-other-sessions action without raw `sid`, row id, tenant/user ids, stable public session handles, device metadata, or per-session controls

That G-C2 boundary covers the main security use cases currently needed by the admin console: end the current session, end all current-user sessions, or preserve the current session while revoking the rest.

Per-session revocation would require a stable public identifier for individual sessions. Exposing the raw JWT `sid` or database row id would leak internal revocation keys. Adding only an opaque handle would let the UI revoke a row, but it would not help an operator choose the right row because the current inventory has no device, IP, user-agent, location, or last-seen context.

Adding device metadata, IP, user-agent, and last-seen fields could make per-session revoke useful, but it also creates a larger privacy, retention, trust, and display boundary. The project has not yet defined how to normalize user agents, trust proxy-provided client IPs, refresh last-seen timestamps, redact or display location-like data, retain metadata after revocation or cleanup, or explain uncertainty in the admin UI.

## Decision

MerchantOps SaaS will keep the G-C2 session-management boundary for now. The current public surface remains:

- `GET /api/v1/auth/sessions`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all`
- `POST /api/v1/auth/logout-others`

The project will not expose stable per-session revoke handles, raw `sid`, auth-session row ids, device metadata, IP address, user-agent, location, or last-seen fields in this slice.

Per-session revoke is deferred until a later dedicated slice first defines:

- an opaque public session handle that is not the raw JWT `sid` or database row id
- whether the handle is stable across reads, whether it rotates, and how it behaves after revocation or cleanup
- device, IP, user-agent, and last-seen collection rules
- proxy trust assumptions and environment configuration for client IP handling
- privacy, redaction, display, and retention rules for metadata
- audit, authorization, and failure semantics for revoking one non-current session
- backend, frontend, API reference, runbook, and smoke-test coverage for the richer boundary

This decision does not change token transport, JWT claims, bearer-token storage, refresh-token scope, cookie/session rotation, CSRF scope, `auth_session` persistence, `POST /api/v1/auth/logout-others`, or the response shape of `GET /api/v1/auth/sessions`.

## Options Considered

### Option 1: Keep G-C2 As-Is

Keep the read-only current-user session inventory plus current-session, all-session, and other-session revocation actions. Continue to defer per-session handles and metadata.

This is the chosen option. It keeps the public contract narrow, preserves the existing security value of logout-other-sessions, and avoids adding a weak per-row action that operators cannot confidently target.

### Option 2: Add Only An Opaque Session Handle

Expose a stable opaque handle on each session row and add a per-session revoke endpoint.

This is rejected for now. It avoids raw `sid` exposure, but without device or last-seen context it gives the UI a precise action over indistinguishable rows. It also commits the API to handle stability, revocation semantics, and retention behavior before the product value is clear.

### Option 3: Add Device Metadata Plus Per-Session Revoke

Store and expose device-oriented session metadata, then add per-session revoke controls over an opaque handle.

This is deferred. It is the only option that could make per-session revoke useful, but it needs a separate privacy and metadata decision before implementation. It should not be bundled into the current auth lifecycle or cookie/refresh-token transport decisions.

## Consequences

- the admin `/sessions` screen remains limited to current-user inventory plus `Sign out other sessions`
- operators can still revoke the current session, all current-user sessions, or every other current-user session
- raw `sid`, database row ids, stable public session handles, device metadata, IP, user-agent, location, and last-seen fields remain outside the public contract
- `POST /api/v1/auth/logout-others` remains the current selective revocation depth
- cookie/session rotation and refresh-token work remain separate future decisions
- future per-session revoke work must start with metadata and privacy boundaries, not just a new endpoint
