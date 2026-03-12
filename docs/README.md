# Documentation

This directory contains the detailed project documentation that sits behind the repository home page.

## Start Here

- [Project Plan](project-plan.md): the 10-week plan for a workflow-first, AI-enhanced vertical SaaS with a portfolio-to-open-source-to-commercial progression path
- [Project Status](project-status.md): implemented scope, current phase, completion status, and known gaps
- [Roadmap](roadmap.md): next-phase work aligned to the 10-week project plan
- [Getting Started](getting-started/README.md): local environment setup and the shortest path to a running API
- [Contributing](contributing/README.md): contributor and agent workflow guidance, release rules, and documentation routing
- [Reference](reference/README.md): configuration, migrations, API conventions, auth, observability, and API documentation
- [Release Versioning](contributing/release-versioning.md): current tag baseline, release sources of truth, and recommended version progression
- [Documentation Maintenance](contributing/documentation-maintenance.md): which docs must change when API, planning, release, or architecture changes happen
- [Development Agent Guidance](contributing/development-agent-guidance.md): tenant-scoped implementation and contributor guidance for coding work
- [Testing Agent Guidance](contributing/testing-agent-guidance.md): verification, regression, and coverage guidance for testing-focused work
- [Review And Release Agent Guidance](contributing/review-release-agent-guidance.md): staged review, commit, and release guidance for review-focused work
- [Execution Planning Agent Guidance](contributing/execution-planning-agent-guidance.md): current-phase assessment, next-step planning, and plan-adjustment guidance
- [User Management Reference](reference/user-management.md): the current `/api/v1/users` contract, validation path, and current Week 2 delivery boundary
- [Ticket Workflow Reference](reference/ticket-workflow.md): the current `/api/v1/tickets` contract, status rules, and workflow-log behavior
- [Import Jobs Reference](reference/import-jobs.md): the current `/api/v1/import-jobs` async submission and status-query contract
- [AI Integration Reference](reference/ai-integration.md): workflow-first AI design, guardrails, and planned endpoint direction
- [AI Provider Configuration](reference/ai-provider-configuration.md): who should own model-provider keys, how cost should be understood, and why the project starts with instance-level configuration
- [AI Docs](ai/README.md): prompt versioning and eval dataset guidance for future AI workflow rollout
- [AI Workflow Candidates](ai/workflow-candidates.md): prioritized candidate AI workflows for future ticket and import rollout
- [Runbooks](runbooks/README.md): smoke tests and troubleshooting steps
- [Automated Test Runbook](runbooks/automated-tests.md): preferred Maven test commands, current automated coverage, and when to switch to manual verification
- [AI Regression Checklist](runbooks/ai-regression-checklist.md): rollout checklist for future AI endpoints, audit logs, and eval changes
- [Architecture](architecture/README.md): design notes and known structural gaps
- [Import File Storage Strategy](architecture/import-file-storage-strategy.md): why Week 5 import files should start behind a replaceable local-storage abstraction before later object-storage rollout
- [Non-Blocking Backlog](architecture/non-blocking-backlog.md): recorded follow-up items that stay visible without blocking the current phase
- [Target Architecture Diagram](diagrams/target-architecture.md): visual runtime shape for the modular-monolith-first architecture and later selective extraction
- [Architecture Decision: Workflow Logs vs Generic Audit Events](architecture/adr/0011-keep-workflow-logs-separate-from-generic-audit-events.md): why ticket workflow history stays separate from the Week 4 reusable audit layer
- [Architecture Decision: Modular Monolith Before Microservices](architecture/adr/0010-prefer-modular-monolith-and-workload-based-extraction-before-microservices.md): why the project stays workflow-first and defers service extraction until workload boundaries are proven
- [Diagrams](diagrams/README.md): architecture, request-flow, and sequence diagrams
- [Incidents](incidents/README.md): placeholder for incident records and postmortems
- [Performance](performance/README.md): placeholder for benchmarks and tuning notes

## Recommended Reading Order

1. [Local Environment](getting-started/local-environment.md)
2. [Quick Start](getting-started/quick-start.md)
3. [Project Plan](project-plan.md)
4. [Project Status](project-status.md)
5. [Roadmap](roadmap.md)
6. [Contributing](contributing/README.md)
7. [Release Versioning](contributing/release-versioning.md)
8. [Documentation Maintenance](contributing/documentation-maintenance.md)
9. [Authentication and RBAC](reference/authentication-and-rbac.md)
10. [Development Agent Guidance](contributing/development-agent-guidance.md)
11. [Testing Agent Guidance](contributing/testing-agent-guidance.md)
12. [Review And Release Agent Guidance](contributing/review-release-agent-guidance.md)
13. [Execution Planning Agent Guidance](contributing/execution-planning-agent-guidance.md)
14. [User Management Reference](reference/user-management.md)
15. [Ticket Workflow Reference](reference/ticket-workflow.md)
16. [Import Jobs Reference](reference/import-jobs.md)
17. [AI Integration Reference](reference/ai-integration.md)
18. [AI Provider Configuration](reference/ai-provider-configuration.md)
19. [AI Docs](ai/README.md)
20. [AI Workflow Candidates](ai/workflow-candidates.md)
21. [Automated Test Runbook](runbooks/automated-tests.md)
22. [Regression Checklist](runbooks/regression-checklist.md)
23. [AI Regression Checklist](runbooks/ai-regression-checklist.md)
24. [Architecture Decision: Modular Monolith Before Microservices](architecture/adr/0010-prefer-modular-monolith-and-workload-based-extraction-before-microservices.md)
25. [Architecture Decision: Workflow Logs vs Generic Audit Events](architecture/adr/0011-keep-workflow-logs-separate-from-generic-audit-events.md)
26. [Target Architecture Diagram](diagrams/target-architecture.md)
27. [Non-Blocking Backlog](architecture/non-blocking-backlog.md)
