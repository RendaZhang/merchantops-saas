---
name: doc-staged-sync
description: Inspect staged changes or the most recent commit in this repository, classify the change type, and route it into the right documentation updates. Use when handling DOC staged or DOC last work, Swagger-visible API changes, documentation navigation changes, current-phase status updates, or any task that asks which docs must change after code changes.
---

# Doc Staged Sync

## Overview

Use this skill to keep repository docs aligned with implementation without spreading the same routing logic across every task. Prefer existing repo docs as the source of detail and keep this skill focused on the decision path.

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md) plus [docs/contributing/documentation-maintenance.md](../../../docs/contributing/documentation-maintenance.md) before editing docs.
2. Inspect the requested scope:
   - use the staged diff for `DOC staged`
   - use `HEAD^..HEAD` for `DOC last`
   - use named files directly if the user pointed to a specific area
3. Classify the change before editing:
   - public API or Swagger contract change
   - internal groundwork only
   - configuration, demo-account, seed-data, or environment change
   - phase, milestone, or release-baseline change
   - navigation-only doc move or rename
4. Route updates by change type:
   - public API: update the relevant page under `docs/reference/`, `api-demo.http`, matching runbooks, and [docs/project-status.md](../../../docs/project-status.md)
   - internal groundwork: update [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), and architecture or contributing docs when boundaries changed; do not present it as public API
   - config or environment: update [docs/reference/configuration.md](../../../docs/reference/configuration.md) and any affected getting-started or runbook pages
   - phase or milestone: update [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), and only touch [docs/project-plan.md](../../../docs/project-plan.md) if the long-range plan itself changed
   - release/tag context: update [../../../CHANGELOG.md](../../../CHANGELOG.md), [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md), and any file that states the current baseline
5. Keep wording boundaries explicit:
   - `public` only when the endpoint is controller-backed and visible in Swagger
   - `planned` for future intent
   - `internal` for groundwork, migrations, services, repositories, and DTOs that are not public yet
6. Finish with a short audit pass:
   - verify real paths, ports, demo accounts, permissions, and seeded IDs
   - verify stale-token versus refreshed-login wording where auth or roles changed
   - verify examples and runbooks only claim checks the documented steps actually perform

## Repo Anchors

- [docs/contributing/documentation-maintenance.md](../../../docs/contributing/documentation-maintenance.md)
- [docs/contributing/development-agent-guidance.md](../../../docs/contributing/development-agent-guidance.md)
- [docs/project-status.md](../../../docs/project-status.md)
- [docs/roadmap.md](../../../docs/roadmap.md)
- [docs/reference/README.md](../../../docs/reference/README.md)
- [docs/runbooks/README.md](../../../docs/runbooks/README.md)

## Output Shape

- State the staged or recent change type first.
- List the docs updated or confirmed already aligned.
- Call out remaining gaps, or explicitly say none remain.
