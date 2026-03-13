# ADR-0001: Use Multi-Module Maven Structure

- Status: Accepted
- Date: 2026-03-08

## Context

The project needs to demonstrate a realistic backend structure that can grow across multiple phases without collapsing API, persistence, shared models, and business rules into one module too early.

## Decision

Use a multi-module Maven structure with separate modules for API, domain, infrastructure, and common concerns.

## Consequences

- API bootstrap, controllers, and security stay in `merchantops-api`
- persistence and repository concerns stay in `merchantops-infra`
- shared responses, exceptions, and utilities stay in `merchantops-common`
- business growth in later phases has a clearer place to land
- build and dependency management is slightly more complex than a single-module setup
