# ADR-0005: Require Tenant-Aware Repository Methods

- Status: Accepted
- Date: 2026-03-09

## Context

This project is multi-tenant and already exposes authenticated business reads. If repository methods are written without tenant scoping, it becomes too easy for service code to accidentally read or combine data across tenants.

## Decision

Require repository methods that serve tenant-scoped business data to include tenant-aware query constraints.

## Consequences

- tenant isolation is enforced closer to the data-access layer
- service methods can rely less on ad hoc controller-level filtering
- user, role, and permission queries are less likely to leak cross-tenant data
- repository APIs become more explicit, but also more verbose than unconstrained CRUD-style methods
