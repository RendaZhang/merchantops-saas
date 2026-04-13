# Documentation

This directory contains the detailed project documentation that sits behind the repository home page.

## Start Here

- [Project Status](project-status.md): implemented scope, current phase, completion status, and known gaps
- [Roadmap](roadmap.md): active release-line milestone, active slice, candidate next slices, and stop condition
- [Product Strategy](product-strategy.md): long-term direction from the `v0.7.0-beta` foundation baseline
- [Project Plan](project-plan.md): planning entry point for current status, roadmap, strategy, and archived foundation-plan links
- [Getting Started](getting-started/README.md): local environment setup and the shortest path to a running API
- [Project Showcase](getting-started/project-showcase.md): 5-10 minute portfolio and open-source handoff path
- [Contributing](contributing/README.md): contributor and agent workflow guidance, release rules, and documentation routing
- [Reference](reference/README.md): configuration, migrations, API conventions, auth, observability, and API documentation
- [Runbooks](runbooks/README.md): smoke tests, regression sign-off, and verification guidance
- [AI Docs](ai/README.md): prompt versioning, eval preparation, and future AI workflow rollout guidance
- [Architecture](architecture/README.md): design notes and known structural gaps
- [Archive](archive/README.md): completed planning records that no longer own current roadmap decisions
- [Diagrams](diagrams/README.md): architecture, request-flow, and sequence diagrams

## Common Jump Targets

- [Release Versioning](contributing/release-versioning.md): current tag baseline and release sources of truth
- [Product Strategy](product-strategy.md): long-term product, delivery, access-control, AI governance, and commercial-discovery tracks
- [Access Control Evolution Plan](architecture/access-control-evolution-plan.md): long-term RBAC, tenant-integrity, and authorization-governance plan
- [Completed 10-Week Foundation Plan](archive/completed-10-week-foundation-plan.md): archived Week 1-10 plan completed at the `v0.7.0-beta` baseline
- [New Coding Agent Start Here](contributing/new-coding-agent-start-here.md): shortest onboarding checklist before deeper development, style, and architecture guidance
- [Documentation Maintenance](contributing/documentation-maintenance.md): which docs must change when API, planning, release, or architecture changes happen
- [Project Showcase](getting-started/project-showcase.md): practical demo flow, architecture talking points, and handoff checklist
- [Java Code Style](contributing/java-code-style.md): package boundaries, comment strategy, and Java review baseline
- [Java Architecture Map](architecture/java-architecture-map.md): module ownership, capability package map, and Java type placement guide
- [Authentication and RBAC](reference/authentication-and-rbac.md): login flow, JWT usage, context propagation, and RBAC expectations
- [Feature Flags Reference](reference/feature-flags.md): current tenant-scoped rollout-control API and the fixed eight-flag inventory
- [User Management Reference](reference/user-management.md): current `/api/v1/users` contract and validation path
- [Ticket Workflow Reference](reference/ticket-workflow.md): current `/api/v1/tickets` contract, state rules, and workflow-log behavior
- [Import Jobs Reference](reference/import-jobs.md): current `/api/v1/import-jobs` contract, `USER_CSV` row execution, and status/error model
- [Import Replay Strategy](architecture/import-replay-derived-job-strategy.md): keep failed-row replay as a new derived import job instead of resetting an existing one
- [Import AI Sanitized Context](architecture/import-ai-sanitized-context-strategy.md): keep import AI prompts on structural, sanitized context instead of forwarding raw failed-row payloads
- [Automated Test Runbook](runbooks/automated-tests.md): preferred Maven test commands and current automated coverage
- [Regression Checklist](runbooks/regression-checklist.md): broader sign-off checklist before merge, release, or phase close
- [Non-Blocking Backlog](architecture/non-blocking-backlog.md): tracked follow-up items that should stay visible without blocking the active phase
- [Target Architecture Diagram](diagrams/target-architecture.md): visual runtime shape for the modular-monolith-first architecture and later selective extraction

## Recommended Reading Order

1. [Local Environment](getting-started/local-environment.md)
2. [Quick Start](getting-started/quick-start.md)
3. [Project Showcase](getting-started/project-showcase.md)
4. [Project Status](project-status.md)
5. [Roadmap](roadmap.md)
6. [Product Strategy](product-strategy.md)
7. [Project Plan](project-plan.md)
8. [Reference](reference/README.md)
9. [Runbooks](runbooks/README.md)
10. [Contributing](contributing/README.md)
11. [Architecture](architecture/README.md)
12. [AI Docs](ai/README.md)
