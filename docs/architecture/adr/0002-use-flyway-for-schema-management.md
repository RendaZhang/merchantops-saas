# ADR-0002: Use Flyway For Schema Management

- Status: Accepted
- Date: 2026-03-08

## Context

The project already needs repeatable schema creation, demo seed data, and a visible change history for tenant, user, role, and permission data. Manual schema changes would make local setup and review harder.

## Decision

Use Flyway migrations for schema creation and seed-data evolution instead of manual database changes.

## Consequences

- local startup can bootstrap schema state automatically
- schema history is versioned and reviewable in Git
- follow-up changes must be expressed as new migrations instead of editing applied ones
- migration discipline becomes part of the development workflow
