# Documentation

This directory contains the detailed project documentation that sits behind the repository home page.

## Start Here

- [Project Status](project-status.md): implemented scope, current phase, completion status, and known gaps
- [Roadmap](roadmap.md): next-phase work aligned to the 10-week project plan
- [Project Plan](project-plan.md): the 10-week plan for a workflow-first, AI-enhanced vertical SaaS with a portfolio-to-open-source-to-commercial progression path
- [Getting Started](getting-started/README.md): local environment setup and the shortest path to a running API
- [Contributing](contributing/README.md): contributor and agent workflow guidance, release rules, and documentation routing
- [Reference](reference/README.md): configuration, migrations, API conventions, auth, observability, and API documentation
- [Runbooks](runbooks/README.md): smoke tests, regression sign-off, and verification guidance
- [AI Docs](ai/README.md): prompt versioning, eval preparation, and future AI workflow rollout guidance
- [Architecture](architecture/README.md): design notes and known structural gaps
- [Diagrams](diagrams/README.md): architecture, request-flow, and sequence diagrams

## Common Jump Targets

- [Release Versioning](contributing/release-versioning.md): current tag baseline and release sources of truth
- [Documentation Maintenance](contributing/documentation-maintenance.md): which docs must change when API, planning, release, or architecture changes happen
- [Java Code Style](contributing/java-code-style.md): package boundaries, comment strategy, and Java review baseline
- [Java Architecture Map](architecture/java-architecture-map.md): module ownership, capability package map, and Java type placement guide
- [Authentication and RBAC](reference/authentication-and-rbac.md): login flow, JWT usage, context propagation, and RBAC expectations
- [User Management Reference](reference/user-management.md): current `/api/v1/users` contract and validation path
- [Ticket Workflow Reference](reference/ticket-workflow.md): current `/api/v1/tickets` contract, state rules, and workflow-log behavior
- [Import Jobs Reference](reference/import-jobs.md): current `/api/v1/import-jobs` contract, `USER_CSV` row execution, and status/error model
- [Import Replay Strategy](architecture/import-replay-derived-job-strategy.md): keep failed-row replay as a new derived import job instead of resetting an existing one
- [Automated Test Runbook](runbooks/automated-tests.md): preferred Maven test commands and current automated coverage
- [Regression Checklist](runbooks/regression-checklist.md): broader sign-off checklist before merge, release, or phase close
- [Non-Blocking Backlog](architecture/non-blocking-backlog.md): tracked follow-up items that should stay visible without blocking the active phase
- [Target Architecture Diagram](diagrams/target-architecture.md): visual runtime shape for the modular-monolith-first architecture and later selective extraction

## Recommended Reading Order

1. [Local Environment](getting-started/local-environment.md)
2. [Quick Start](getting-started/quick-start.md)
3. [Project Status](project-status.md)
4. [Roadmap](roadmap.md)
5. [Project Plan](project-plan.md)
6. [Reference](reference/README.md)
7. [Runbooks](runbooks/README.md)
8. [Contributing](contributing/README.md)
9. [Architecture](architecture/README.md)
10. [AI Docs](ai/README.md)
