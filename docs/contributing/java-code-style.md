# Java Code Style

Last updated: 2026-03-26

## Purpose

This page defines the repository's baseline Java package-organization, commenting, and review conventions.

Use it when placing new Java types, restructuring packages, extracting shared support, or reviewing whether cleanup is improving readability or just moving code around.

## Architecture Baseline

- Treat the codebase as a three-module Java structure:
  - `merchantops-api -> merchantops-domain`
  - `merchantops-infra -> merchantops-domain`
- `merchantops-api` must not depend directly on `merchantops-infra` entities or repositories.
- `merchantops-domain` owns business-facing use cases, ports, records, and shared business errors; keep it free of Spring Web, Spring Data JPA, and JPA annotations.
- `ApiResponse` stays in `merchantops-api`; `BizException` and `ErrorCode` stay in `merchantops-domain`.

## Package Organization

- Prefer capability-first packages over large horizontal buckets.
- In `merchantops-api`, put business-facing orchestration under capability packages such as `api.user`, `api.ticket`, `api.importjob`, `api.approval`, `api.audit`, and `api.auth`.
- Keep public HTTP DTOs under `api.dto/<capability>/...`.
- Keep shared AI vendor/runtime code under `api.ai/...`; keep ticket-facing AI endpoint orchestration under `api.ticket.ai/...`.
- Reserve `api.platform` for cross-cutting support such as response envelopes, config, security, request context, filters, documentation wiring, and tooling helpers.
- Treat root `api.controller`, `api.contract`, `api.service`, and `api.messaging` as legacy or platform-only transition space. Do not place new capability code there.
- Keep tests close to the production package shape they validate. Shared test doubles or fixture servers should live in a dedicated test-support package instead of a flat catch-all package.

## Boundary Rules

- DTOs describe HTTP contract data only; do not hide service-only helpers or provider adapters there.
- Controllers, Swagger contracts, and `ApiResponse` wrappers stay in `merchantops-api`.
- Domain use cases, ports, records, and shared business errors stay in `merchantops-domain`.
- JPA adapters, entities, and Spring Data repositories stay in `merchantops-infra`.
- Provider adapters should validate provider payload shape and translate provider-specific wire concerns, but should not take over service orchestration or business-side recording.

## Shared Support Extraction

- Extract shared support only when it centralizes repeated mechanics such as request normalization, failure mapping, audit-record assembly, or response mapping.
- Keep business-specific validation, state transitions, replay rules, approval rules, and provider output-policy checks in the capability service that owns them.
- Prefer a capability-scoped helper over a cross-capability abstraction unless the same mechanics are reused across boundaries.
- Do not extract a shared abstraction if it makes the owning workflow less explicit than the duplicated code it replaces.

## Comment Strategy

- Add comments for non-obvious constraints, state-machine rules, replay boundaries, provider differences, truncation rules, or safety guards.
- Prefer explaining why a rule exists over narrating obvious line-by-line behavior.
- Do not add blanket JavaDoc or filler comments just to increase comment count.
- Class-level JavaDoc is optional; add it only when a boundary, ownership rule, or non-obvious lifecycle needs a short explanation.
- When a rule is important across files, document it here or in the relevant contributing guide instead of repeating the same long comment everywhere.

## Local Checks

Run the normal regression baseline first:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

Run the formatting/style gate before merge when Java structure or shared conventions changed:

```powershell
.\mvnw.cmd verify
```

`verify` should keep Checkstyle and ArchUnit green after package moves or boundary changes.

## PR Self-Check

- New or moved business types sit under the correct capability package, not under root horizontal packages.
- `merchantops-api` does not add new direct dependencies on `merchantops-infra` entities or repositories.
- `api.platform` still contains only cross-cutting support, not capability business logic.
- Package names and type names still match the responsibility split after the change.
- No wildcard imports or stale imports remain.
- Shared support only captures repeated mechanics; business-specific validation still lives in the service or adapter that owns it.
- Comments explain constraints or tradeoffs, not obvious assignments.
- Tests still mirror the production package surface they validate.
- If package boundaries or ownership rules changed, update `docs/contributing/java-code-style.md` and `docs/architecture/java-architecture-map.md` in the same change.
