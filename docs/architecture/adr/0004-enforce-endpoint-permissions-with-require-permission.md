# ADR-0004: Enforce Endpoint Permissions With RequirePermission

- Status: Accepted
- Date: 2026-03-09

## Context

The project needs explicit authorization rules at endpoint level for demo and future business APIs. Scattering ad hoc permission checks inside controller bodies would make behavior harder to audit and document.

## Decision

Use a custom `@RequirePermission` annotation plus interceptor-based enforcement for endpoint permission checks.

## Consequences

- required permissions stay visible near controller methods
- permission failures are handled in a consistent path
- the same approach can be reused when real business APIs are added
- authorization behavior depends on correct interceptor registration and authenticated authority mapping
