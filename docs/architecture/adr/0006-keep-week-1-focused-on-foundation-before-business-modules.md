# ADR-0006: Keep Week 1 Focused On Foundation Before Business Modules

- Status: Accepted
- Date: 2026-03-09

## Context

The project goal is to evolve into a richer merchant operations backend, but the initial phase still needed a stable base for authentication, authorization, tenant isolation, migrations, local setup, and API conventions. Adding business modules too early would have diluted that effort.

## Decision

Keep Week 1 focused on backend foundation work before implementing larger business modules such as tickets, imports, billing, or richer user management.

## Consequences

- Week 1 scope stays small enough to complete with coherent infrastructure and security foundations
- Week 2 can build on a more stable base instead of retrofitting auth and tenant concerns later
- the project temporarily looks more like a platform skeleton than a full business product
- roadmap documents become important so readers understand what is intentionally deferred
