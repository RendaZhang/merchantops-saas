# ADR-0001: Use Multi-Module Maven Structure

- Status: Accepted
- Date: 2026-03-08

## Context

The project needs to demonstrate a realistic backend structure that can grow across multiple phases without collapsing API, persistence, shared models, and business rules into one module too early.

## Decision

Use a multi-module Maven structure with separate modules for API, domain, and infrastructure concerns.

## Consequences

- API bootstrap, controllers, HTTP DTOs, and response-envelope concerns stay in `merchantops-api`
- persistence and repository concerns stay in `merchantops-infra`
- shared business errors, use cases, and ports stay in `merchantops-domain`
- business growth lands behind domain use-case and port seams instead of adding more direct `api -> infra` coupling
- build and dependency management is slightly more complex than a single-module setup
