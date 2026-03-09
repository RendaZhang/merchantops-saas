# ADR-0003: Put Tenant Identity In JWT And Request Context

- Status: Accepted
- Date: 2026-03-08

## Context

This project is multi-tenant. Protected requests need consistent tenant and user identity after login, and business code should not depend on callers repeatedly passing tenant identifiers through controller parameters.

## Decision

Store tenant identity in JWT claims and restore it into request-scoped context holders during authentication.

## Consequences

- authenticated requests can access tenant and user identity consistently
- business code can use context helpers instead of trusting caller-supplied tenant identifiers
- tenant isolation is easier to enforce across service and repository code
- JWT claim design becomes security-sensitive and must stay consistent with authorization logic
