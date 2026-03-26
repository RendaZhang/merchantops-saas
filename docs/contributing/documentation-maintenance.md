# Documentation Maintenance

Last updated: 2026-03-26

## Purpose

This page defines how documentation should be updated as the project moves through implementation, release, and open-source milestones.

Use it as the routing table for answering two questions:

- what kind of change happened
- which documents must be updated because of that change

## Core Rules

- `README.md` stays high-level. Do not move detailed implementation notes into it.
- `docs/README.md` is the documentation entry page and should link to important new pages.
- `docs/README.md` should prefer section-level entry points plus a short list of high-value jump targets; do not duplicate every child page that already has its own section index.
- repeated multi-step contributor or agent workflows should live in `.agents/skills/`; keep `AGENTS.md` and `docs/contributing/README.md` as entry points and do not duplicate full skill step-by-step logic there.
- `docs/project-plan.md` should stay milestone-level; do not mirror slice-level current implementation notes there when `docs/project-status.md` already records repository reality.
- `docs/project-plan.md` may keep a short planning anchor for the current week, but detailed current status, near-term slice tracking, and detailed release history belong in `docs/project-status.md`, `docs/roadmap.md`, `CHANGELOG.md`, and `docs/contributing/release-versioning.md`.
- `docs/roadmap.md` should stay focused on the active phase, next steps, and near-term sequencing; exact current endpoint inventories belong in `docs/project-status.md` and `docs/reference/`.
- `docs/project-status.md` should stay phase-oriented and current-baseline-oriented; do not keep historical per-week completion checklists or long retrospective inventories there once they stop helping current iteration work.
- Public API documentation must match what is actually exposed in controllers and visible in Swagger.
- Internal groundwork that is not public yet must not be documented as a callable API.
- Real paths, ports, demo accounts, and seeded data must match code and database state.
- Runbooks and reference examples must only claim checks that the documented commands actually execute.
- If a guide relies on seeded IDs or other environment-sensitive sample data, state whether it assumes a fresh local database or expects the reader to copy a runtime value from a previous response.
- If authentication, role assignment, or workflow-state docs describe an access change, state whether existing JWTs still work or whether stale claims are rejected until the user logs in again.
- `CHANGELOG.md` is release-oriented, not a full development diary.

## Maintaining This Document

- This page is itself a living routing guide and may be updated when its rules no longer match the repository's actual documentation structure or workflow.
- If an agent or contributor finds that this page is outdated, inaccurate, missing a recurring rule, or no longer aligned with current docs, it should correct the page instead of following a known-bad routing rule.
- If the change affects repository-level default guidance, update [../../AGENTS.md](../../AGENTS.md) in the same change.

## Document Roles

- `README.md`: project overview, major capabilities, and top-level navigation
- `docs/README.md`: documentation navigation and reading order
- `docs/project-status.md`: current implemented reality, current baseline, and limitations
- `docs/roadmap.md`: next-phase work and intended near-term sequence
- `docs/project-plan.md`: longer-range milestone plan
- `docs/reference/`: stable technical reference pages for the system itself
- `docs/contributing/`: contributor and agent workflow guidance
- `docs/contributing/development-agent-guidance.md`: tenant-scoped implementation and contributor guidance for coding work
- `docs/contributing/java-code-style.md`: Java package organization, shared-support extraction, comment strategy, and style gate baseline
- `docs/architecture/java-architecture-map.md`: current Java module ownership, capability package map, and type-placement guide
- `docs/contributing/testing-agent-guidance.md`: verification and regression rules for testing-focused work
- `docs/contributing/review-release-agent-guidance.md`: staged review, commit, and release rules
- `docs/contributing/execution-planning-agent-guidance.md`: current-phase assessment and next-step planning rules
- `docs/runbooks/`: validation, regression, and operational checklists
- `docs/architecture/adr/`: formal architecture decisions
- `docs/contributing/release-versioning.md`: version rules and tag progression
- `CHANGELOG.md`: release-level notable changes
- `api-demo.http`: IDE-friendly request examples for public APIs

## Public, Planned, And Internal Language

Use these labels consistently:

- `public`: implemented in controller and visible in Swagger
- `planned`: intended future capability that is not public yet
- `internal`: groundwork in service, DTO, repository, migration, or design only

Rules:

- only `public` endpoints belong in API reference as current contract
- `planned` endpoints must be clearly marked as examples or future direction
- `internal` groundwork belongs in status, roadmap, or architecture notes, not public API reference

## Update Matrix By Change Type

### 1. Public API Or Swagger Contract Changes

Always update:

- relevant page under `docs/reference/`
- `api-demo.http`
- related runbook under `docs/runbooks/`
- `docs/project-status.md`

Update when needed:

- `README.md` if the capability overview changed materially
- `docs/README.md` if a new reference or runbook page was added
- `CHANGELOG.md` if the change is part of a release-worthy milestone

### 2. Internal Groundwork Only

Examples:

- service-layer scaffold
- repository method
- DTO or command object
- migration for future features

Update:

- `docs/project-status.md`
- `docs/roadmap.md` if it changes the next step or phase sequence

Update when the internal rule or boundary changed:

- `docs/contributing/development-agent-guidance.md`
- `docs/contributing/java-code-style.md` when package organization, shared-support extraction, or style gate expectations changed
- `docs/architecture/java-architecture-map.md` when Java module ownership, capability package placement, or type-placement guidance changed
- `docs/contributing/testing-agent-guidance.md`
- `docs/contributing/review-release-agent-guidance.md`
- `docs/contributing/execution-planning-agent-guidance.md`
- `AGENTS.md` if the rule should guide future agents by default

Also update these when Java architecture rules changed materially:

- `docs/README.md`
- the relevant index page under `docs/architecture/` or `docs/contributing/`

Examples:

- capability-first package reshaping
- moving a type between `api`, `domain`, and `infra`
- changing where shared support should live
- changing comment or PR review expectations for Java structure work

Do not update as current public API:

- Swagger-facing reference docs
- `api-demo.http`
- capability statements in `README.md`

### 3. Configuration, Ports, Paths, Demo Accounts, Or Seed Data Changes

Update:

- `docs/reference/configuration.md`
- `docs/reference/database-migrations.md` if seed or migration behavior changed
- `docs/getting-started/` pages if startup behavior changed
- any reference page that mentions the affected path, port, or demo account

Update when needed:

- `README.md` if the quick-start or high-level setup summary changed
- `api-demo.http` if login or path examples changed

### 4. Authentication, RBAC, Or Tenant-Isolation Behavior Changes

Update:

- `docs/reference/authentication-and-rbac.md`
- relevant business reference pages
- related runbooks
- `docs/project-status.md`

Update when needed:

- architecture ADR if the change is a new decision, not just implementation detail
- document whether the change takes effect on the next protected request, only after re-login, or both

### 5. Architecture Decision Changes

Update:

- add or update an ADR under `docs/architecture/adr/`
- `docs/architecture/README.md`

Update when needed:

- `docs/project-plan.md` if the decision changes milestone shape
- `docs/roadmap.md` if the decision changes near-term sequencing
- relevant reference page if the decision affects public guidance

### 6. AI Planning, AI Governance, Or Future AI API Shape Changes

Update:

- `docs/reference/ai-integration.md`
- related pages under `docs/ai/`
- `docs/runbooks/ai-regression-checklist.md`

Update when needed:

- `docs/reference/ai-provider-configuration.md`
- ADRs for AI workflow, audit, eval, or provider ownership
- `docs/project-plan.md`, `docs/project-status.md`, and `docs/roadmap.md` if AI milestones changed

### 7. Version, Tag, Or Release Milestone Changes

Update:

- `CHANGELOG.md`
- `docs/contributing/release-versioning.md`

Update when needed:

- `docs/project-status.md` if the current tagged baseline changed
- `docs/roadmap.md` if the planned release sequence changed
- `README.md` if the current tagged milestone summary changed

For `DOC pre-tag` or another release-cut commit that will be tagged immediately:

- align `docs/project-status.md`, `docs/roadmap.md`, and `docs/project-plan.md` with the current phase framing
- update `CHANGELOG.md`, `README.md`, and `docs/contributing/release-versioning.md` to the intended new tagged state on that same commit
- create the Git tag immediately after that commit so docs and Git do not drift

For earlier planning or tag-readiness passes before the release-cut commit exists:

- keep the current tagged milestone unchanged
- describe the intended new version as a prepared next tag instead of current Git reality
- keep `CHANGELOG.md` notes in `Unreleased` until the release-cut commit is ready

Treat the following as the minimum tag-ready doc checklist:

- `CHANGELOG.md`
- `docs/contributing/release-versioning.md`
- `docs/project-status.md`
- `docs/roadmap.md`

Also update when the tagged baseline summary or milestone framing changed:

- `README.md`, including any top-of-file version banner or release-line wording
- `docs/project-plan.md`

## Update Matrix By Phase Or Node

### Week Or Phase Still In Progress

Keep current:

- `docs/project-status.md`
- `docs/roadmap.md`

Update only if public behavior already changed:

- reference docs
- runbooks
- `api-demo.http`

### Week Or Phase Completed

Always review:

- `docs/project-status.md`
- `docs/roadmap.md`
- `docs/project-plan.md` if milestone framing changed
- `CHANGELOG.md` if the completion is release-worthy
- `docs/contributing/release-versioning.md` if a new tag is created

### New Public Module Added

Examples:

- ticket workflow
- import jobs
- AI public endpoints

Usually add or update:

- one or more `docs/reference/` pages
- one or more `docs/runbooks/` pages
- `api-demo.http` or a dedicated demo HTTP file
- `README.md` capability summary
- `docs/README.md` navigation
- `docs/project-status.md`

## Recommended Update Workflow

1. classify the change as `public`, `planned`, or `internal`
2. identify the change type from the matrix above
3. if the request is about the most recent commit instead of the staged diff, inspect `HEAD^..HEAD` first and route the same required doc updates from the actual committed scope
4. update the required documents first
5. verify that Swagger, examples, and docs agree
6. update release docs only if the change affects a milestone or tag

## Pre-Stage Documentation Check

Before staging doc changes, confirm:

- documented endpoints are really visible in Swagger if described as public
- paths use real current routes
- demo accounts, ports, and credentials still match code or seed data
- smoke steps and expected-results text describe the same checks; move extra negative paths into automated-test notes or a broader checklist when the smoke flow does not execute them
- any hard-coded ID in examples is either truly stable for the intended environment or clearly labeled as a fresh-local-db assumption
- access-change examples state whether a pre-change token remains valid or whether the reader must re-login because stale claims are rejected
- `README.md` did not absorb low-level detail that belongs in `docs/`
- newly added docs are linked from the right navigation page
- release/version references do not conflict with existing tags

## Related Documents

- [release-versioning.md](release-versioning.md): version and tag rules
- [../reference/ai-integration.md](../reference/ai-integration.md): AI workflow and public-vs-planned boundaries
- [../project-status.md](../project-status.md): current implementation reality
- [../roadmap.md](../roadmap.md): intended next-phase work
- [../../CHANGELOG.md](../../CHANGELOG.md): release-level change history
