# Roadmap

Last updated: 2026-03-09

## Current Phase

- Week 1 foundation is complete
- Week 2 is the next active planning stage

## Week 2 Goal

Build real business APIs on top of the Week 1 foundation.

## Candidate Modules

- user management
- feature flag and whitelist
- ticket workflow
- async CSV import
- billing and usage ledger

## Recommended Next Step

Start with real user management APIs:

- create user
- update user
- disable user
- pagination and filters
- operator audit fields

## Planned Next-Phase Work

- create real user management APIs
- add create and update user endpoints
- introduce pagination and query filters
- add audit logging fields and operator tracking
- build ticket, import, and billing demo modules
- add integration tests
- improve deployment and observability docs

## Near-Term Priorities

- move from RBAC demo endpoints to clearer business-oriented endpoints
- expand user management beyond read-only listing
- improve test coverage for authentication, permission checks, and tenant isolation
- continue turning README-linked docs into a more complete developer handbook

## Notes

- This document tracks intended next-phase work, not committed delivery dates.
- Implemented features and known current limitations are recorded in [project-status.md](project-status.md).
