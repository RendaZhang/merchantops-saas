# Roadmap

Last updated: 2026-03-10

## Current Phase

- Week 1 Platform Foundation is complete
- Week 2 First Business Loop - Tenant User Management is in progress
- Public user-management HTTP coverage is still limited to `GET /api/v1/users`

## Week 2 Goal

Complete the first business loop by turning the current user-management groundwork into Swagger-visible business APIs on top of the Week 1 foundation.

## Phase Sequence

- Week 2: user management (active)
- Week 3: ticket workflow and audit trail
- Week 4: async CSV import and background processing
- Week 5: feature flag, metrics, deployment hardening, and documentation polish
- Stretch after Week 5: usage / ledger / invoice minimal loop

## Recommended Next Step

Continue with real user management APIs:

- expose paged user listing in controller and Swagger
- add tenant-scoped user detail lookup
- implement create, update, and password-change flows end to end
- add operator audit fields and verification coverage

## Planned Work By Phase

Week 2 completion:

- expose `GET /api/v1/users` query parameters for pagination and status filtering
- add `GET /api/v1/users/{userId}` detail endpoint
- add create, update, status-toggle, and role-assignment endpoints
- add audit logging fields and operator tracking for user-management writes
- keep Swagger, `api-demo.http`, and reference docs aligned

Week 3 target:

- build the ticket workflow and audit trail module

Week 4 target:

- build async import and background processing

Week 5 target:

- add feature flag support, metrics, deployment hardening, and stronger runbook/docs coverage
- add integration tests where the current surface is stable

Stretch target after Week 5:

- add usage / ledger / invoice minimal loop

## Near-Term Priorities

- move from RBAC demo endpoints to clearer business-oriented endpoints
- expand user management beyond read-only listing and complete the first business loop
- keep Week 2 documentation aligned with the public API as endpoints are exposed
- sequence Week 3 ticket work only after Week 2 user management is usable end to end
- improve test coverage for authentication, permission checks, and tenant isolation
- continue turning README-linked docs into a more complete developer handbook

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Implemented features and known current limitations are recorded in [project-status.md](project-status.md).
