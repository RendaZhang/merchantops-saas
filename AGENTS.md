# Project Guidance

This repository currently organizes handoff rules around five roles plus a small set of repo-local skills for repeated workflows:

1. Documentation
2. Testing
3. Development
4. Review and Release
5. Execution Planning

## AGENTS Maintenance Policy

- Treat this file as a living repository-level guide, not as a frozen handoff note.
- An agent may update this file when it finds guidance that is outdated, inaccurate, missing, or repeatedly needed across tasks.
- Only promote rules into this file when they are stable, reusable, and likely to matter again for future work.
- Do not add one-off task notes, temporary workarounds, or short-lived phase noise here.
- Keep this file short. If a rule starts turning into implementation detail, runbook nuance, or verification edge-case guidance, move that detail into the appropriate page under `docs/contributing/` and leave only the repository-level summary here.
- If this file or another repository guidance page is clearly outdated, inaccurate, or incomplete, fix the guidance instead of working around it silently.
- A good promotion threshold is:
  - the same confusion or mistake has already happened more than once
  - the rule applies across tasks, not only to a single change
  - future agents are likely to benefit from seeing it before they start work
- If this file is updated, keep linked reference pages and runbooks aligned in the same change when needed.
- Do not use this file as the only place for product, architecture, permission-policy, or release-policy decisions; those still belong in the relevant docs and ADRs.
- If a repeated workflow is too detailed for this file but stable enough to reuse, promote it into `.agents/skills/` and keep only the entry-point summary here.

## Cross-Role Documentation Ownership

- The roles in this file are working perspectives, not ownership walls.
- Any role may update documentation when the task requires it, but should do so from the perspective of that role and still follow [docs/contributing/documentation-maintenance.md](docs/contributing/documentation-maintenance.md).
- Documentation-focused work should keep structure, navigation, and public-vs-internal wording clear.
- Testing-focused work may update runbooks, automated test guidance, regression checklists, and related reference docs when coverage or verification reality changes.
- Development-focused work may update docs when implementation changes, but must not present internal groundwork as public API.
- Review and release work may update changelog, versioning, release notes, and milestone summaries when the staged diff supports it.
- Execution-planning work may update status, roadmap, product strategy, planning entry points, and architecture guidance when it finds phase drift, scope mismatch, or planning assumptions that no longer match repository reality.
- If a role updates a doc outside its usual focus area, it should keep the change scoped to the facts learned through that role's work rather than rewriting unrelated narrative.

## Repo Skills

- Repo-local skills live under [`.agents/skills/README.md`](.agents/skills/README.md).
- Use a repo skill when the task matches a stable, repeated, multi-step workflow that would otherwise add too much detail to this file.
- Read the matching `.agents/skills/<skill-name>/SKILL.md` before acting when the task clearly fits that workflow.
- Skills refine execution. They do not override repository-level rules in this file or the linked docs under `docs/contributing/`.
- Keep repo skills concise and prefer linking repository docs over duplicating long guidance into the skill itself.

## Current Repo Skills

- [`.agents/skills/doc-staged-sync/SKILL.md`](.agents/skills/doc-staged-sync/SKILL.md): staged or recent diff to documentation-routing and documentation-sync workflow, plus repo-wide doc maintenance when no diff exists
- [`.agents/skills/phase-status-sync/SKILL.md`](.agents/skills/phase-status-sync/SKILL.md): align `project-status`, `roadmap`, `project-plan`, and `product-strategy` without mixing current reality, near-term work, planning navigation, and long-range strategy
- [`.agents/skills/release-tag-prep/SKILL.md`](.agents/skills/release-tag-prep/SKILL.md): milestone and release-tag documentation preparation, open-source release-cut work, and post-tag cleanup
- [`.agents/skills/import-surface-sync/SKILL.md`](.agents/skills/import-surface-sync/SKILL.md): align import workflow docs, approval-backed import execution bridge docs, import AI docs, examples, runbooks, and milestone text with the current public import surface
- [`.agents/skills/ai-ticket-surface-sync/SKILL.md`](.agents/skills/ai-ticket-surface-sync/SKILL.md): align public ticket AI docs, provider/runtime wording, runbooks, examples, and milestone text with the current public ticket AI surface
- [`.agents/skills/auth-surface-sync/SKILL.md`](.agents/skills/auth-surface-sync/SKILL.md): align login/logout/logout-all, JWT `sid`, auth-session cleanup, admin auth UX wording, auth examples, runbooks, and phase docs with the current public auth surface
- [`.agents/skills/tdr-last-cycle/SKILL.md`](.agents/skills/tdr-last-cycle/SKILL.md): run the `TT last`, `DOC last`, `RR last` cleanup loop against the most recent commit until no fixable findings remain

## Documentation Role

- Read [docs/contributing/documentation-maintenance.md](docs/contributing/documentation-maintenance.md) before making documentation changes.
- Read [docs/contributing/development-agent-guidance.md](docs/contributing/development-agent-guidance.md) before making development-facing documentation changes.
- When Java package ownership, module boundaries, or shared-support placement rules change, sync [docs/contributing/java-code-style.md](docs/contributing/java-code-style.md), [docs/architecture/java-architecture-map.md](docs/architecture/java-architecture-map.md), and the relevant documentation-routing guidance together.
- When long-term product direction changes from the `v0.7.0-beta` foundation baseline, update [docs/product-strategy.md](docs/product-strategy.md); when access-control strategy changes, update [docs/architecture/access-control-evolution-plan.md](docs/architecture/access-control-evolution-plan.md).
- Keep root `README.md` high-level and move development detail into `docs/`.
- Do not document an endpoint as public unless it is visible in Swagger.
- Use the linked contributing pages for detailed wording rules around verification scope, environment-sensitive sample data, and stale-token versus re-login expectations instead of duplicating those details here.
- Follow the routing rules in the linked maintenance and development guidance pages instead of re-encoding detailed update matrices here.
- For AI docs, keep [docs/reference/ai-integration.md](docs/reference/ai-integration.md) as the canonical current public contract page; use `docs/ai/` for prompt-versioning, eval, workflow-candidate, and high-level current-boundary guidance instead of duplicating full API reference detail there.
- Use [`.agents/skills/doc-staged-sync/SKILL.md`](.agents/skills/doc-staged-sync/SKILL.md) when the task is centered on `DOC staged`, `DOC last`, Swagger-visible doc sync, or documentation routing after implementation changes.
- Use [`.agents/skills/import-surface-sync/SKILL.md`](.agents/skills/import-surface-sync/SKILL.md) when the task is centered on import endpoints, replay modes, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, import AI endpoints, import AI runbooks, or import-specific doc alignment across reference, approval docs, AI docs, examples, and milestone pages.
- Use [`.agents/skills/ai-ticket-surface-sync/SKILL.md`](.agents/skills/ai-ticket-surface-sync/SKILL.md) when the task is centered on `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets/{id}/ai-reply-draft`, or ticket-only AI provider/runtime and runbook alignment across the current public ticket AI surface.
- Use [`.agents/skills/auth-surface-sync/SKILL.md`](.agents/skills/auth-surface-sync/SKILL.md) when the task is centered on `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/logout-all`, JWT `sid`, `auth_session` cleanup behavior, or admin-console sign-out / restore wording that must stay aligned across auth reference docs, admin docs, runbooks, examples, and phase pages.
- When the task is centered on shared AI governance reads or wording that spans both ticket and import AI, prefer [`.agents/skills/doc-staged-sync/SKILL.md`](.agents/skills/doc-staged-sync/SKILL.md) unless the change is clearly ticket-only or import-only.
- Use [`.agents/skills/release-tag-prep/SKILL.md`](.agents/skills/release-tag-prep/SKILL.md) and [`.agents/skills/phase-status-sync/SKILL.md`](.agents/skills/phase-status-sync/SKILL.md) together when the task is centered on the final release-doc sync across status, roadmap, plan, changelog, README, and release-versioning.
- Shortcut prefix: `DOC`
- Supported shortcuts include:
  - `DOC staged`: inspect the staged diff first and identify which docs must change
  - `DOC last`: inspect the most recent commit (`HEAD^..HEAD`) first and identify which docs should have changed or still need follow-up updates
  - `DOC pre-tag`: run the final release-doc sync for the release-cut commit, update docs to the intended new tagged state, stage the doc changes, and create the Git tag immediately after that same commit lands
  - `DOC route`: map the current change to the required doc updates using [docs/contributing/documentation-maintenance.md](docs/contributing/documentation-maintenance.md)
  - `DOC sync`: align affected docs with the current implementation, keeping `public`, `planned`, and `internal` wording correct
  - `DOC nav`: update `README.md`, `docs/README.md`, and the relevant index pages when docs were added, moved, or renamed
  - `DOC swagger`: align Swagger-visible endpoints with `docs/reference/`, `api-demo.http`, and runbooks
  - `DOC phase`: refresh `docs/project-status.md`, `docs/roadmap.md`, and related milestone docs for the current phase
  - `DOC audit`: scan for outdated paths, ports, demo accounts, permissions, or contradictory statements
  - `DOC split`: move low-level detail out of `README.md` and into the appropriate `docs/` page without losing navigation clarity

## Testing Role

- Read [docs/contributing/testing-agent-guidance.md](docs/contributing/testing-agent-guidance.md) before changing tests, test coverage notes, or verification guidance.
- Start from [docs/runbooks/automated-tests.md](docs/runbooks/automated-tests.md) for the current regression command and coverage boundary.
- Keep tests, runbooks, and public API docs aligned when verification reality changes.
- Use the linked testing guidance for detailed rules on smoke-scope wording, Flyway migration sign-off, and stale-token versus refreshed-login verification.
- When AI provider wiring, `.env` loading, provider selection, or live vendor compatibility changes, use [docs/runbooks/ai-live-smoke-test.md](docs/runbooks/ai-live-smoke-test.md) plus [docs/runbooks/ai-regression-checklist.md](docs/runbooks/ai-regression-checklist.md) instead of improvising ad hoc live checks.
- When multiple public AI endpoints share the same slice and governance model, keep adapter failure-set, degraded-mode, no-side-effect assertions, and runbook/checklist coverage symmetric instead of hardening only one endpoint.
- Treat `TT staged` as a testing-focused staged review entry point: inspect the staged diff, run or choose the smallest sufficient verification set, and report findings ordered by urgency (`P1`, `P2`, `P3`) rather than stopping at scope mapping alone.
- Shortcut prefix: `TT`
- Supported shortcuts include:
  - `TT staged`: review the staged diff from a testing perspective, run or select the smallest sufficient verification, and report prioritized findings plus remaining verification gaps
  - `TT last`: review the most recent commit (`HEAD^..HEAD`) from a testing perspective, map it to affected test layers, and report prioritized findings plus remaining verification gaps
  - `TT test`: run or recommend the default automated regression command `.\mvnw.cmd -pl merchantops-api -am test`
  - `TT coverage`: summarize current automated coverage, manual-only gaps, and the right next verification step
  - `TT smoke`: run or guide the local smoke flow from [docs/runbooks/local-smoke-test.md](docs/runbooks/local-smoke-test.md)
  - `TT live`: prepare live smoke prerequisites with `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`, then start the API from `merchantops-api` with `..\mvnw.cmd spring-boot:run`
  - `TT ai-live`: prepare `.env`, refresh local SNAPSHOTs with `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`, start the API from `merchantops-api` with `..\mvnw.cmd spring-boot:run`, then follow [docs/runbooks/ai-live-smoke-test.md](docs/runbooks/ai-live-smoke-test.md) and [docs/runbooks/ai-regression-checklist.md](docs/runbooks/ai-regression-checklist.md)
  - `TT auth`: focus verification on login, JWT, `/api/v1/users` (`GET` and `POST`), and password-rule regressions
  - `TT sql`: focus verification on native SQL and H2 `MODE=MySQL` test realism, including datasource-replacement pitfalls
  - `TT clean`: provide or execute cleanup steps for generated smoke users after manual verification

## Development Role

- If you are a new coding agent in this repository, read [docs/contributing/new-coding-agent-start-here.md](docs/contributing/new-coding-agent-start-here.md) first, then continue into the development, Java style, and architecture guidance pages.
- Read [docs/contributing/development-agent-guidance.md](docs/contributing/development-agent-guidance.md) before changing tenant-scoped repositories, services, DTOs, or user-management internals.
- Read [docs/contributing/java-code-style.md](docs/contributing/java-code-style.md) before large package refactors, shared-support extraction, or Java style cleanup.
- Read [docs/architecture/java-architecture-map.md](docs/architecture/java-architecture-map.md) before moving Java types across modules or capability packages.
- This is a multi-module Maven repository. When `merchantops-api` depends on sibling-module changes, prefer reactor commands with `-am`; before standalone `spring-boot:run` or API-only verification after repository/entity/signature changes, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests` first so local SNAPSHOT dependencies are current.
- Keep security-sensitive auth UI wording aligned with server-confirmed outcomes; use the linked development guidance for revoke-style fallback wording.
- Keep tenant scoping explicit and keep query/write models separated.
- On mixed-action approval surfaces, route queue/detail/approve/reject authorization by approval action capability instead of hard-coding a controller-wide `USER_*` gate.
- Use the linked development guidance for detailed rules on `operatorId` / `requestId` propagation, internal attribution fields, and request-time access revalidation.
- For large repetitive or cross-file edits, prefer scoped Python or Node transforms over a single giant `apply_patch`; keep the detailed execution guidance in the linked development guidance page.
- Do not present internal groundwork as public API.
- For public suggestion-only AI endpoints, if provider data fails local output-policy validation after the provider returns, keep that path mapped as `AiProviderException(INVALID_RESPONSE)` so `ai_interaction_record` preserves `INVALID_RESPONSE` rather than collapsing into a generic business error.
- If implementation changes affect public contract or reusable repo rules, update the linked docs in the same change.

## Review and Release Role

- Read [docs/contributing/review-release-agent-guidance.md](docs/contributing/review-release-agent-guidance.md) before doing staged review, commit-message suggestion, or release/tag work.
- Default review scope is the staged diff only.
- Treat pasted findings from earlier review rounds as hints, not as still-open facts. Re-check whether they still apply to the current staged diff before repeating or carrying them forward.
- Keep changelog, release versioning, and milestone docs aligned with the reviewed change set.
- For tag readiness, treat `CHANGELOG.md`, `docs/contributing/release-versioning.md`, `docs/project-status.md`, and `docs/roadmap.md` as the minimum release-doc set; include `README.md` when the current tagged baseline summary changed, `docs/product-strategy.md` when long-term milestone framing changed, and `docs/project-plan.md` only when planning navigation changed.
- If a staged release-cut commit already moves docs to tagged-state wording, that same commit must be tagged immediately after it lands; do not leave tagged-state docs untagged across later commits.
- Use [`.agents/skills/release-tag-prep/SKILL.md`](.agents/skills/release-tag-prep/SKILL.md) when the task is centered on tag readiness, release-note drafting, or pre-tag / post-tag doc alignment.
- Shortcut prefix: `RR`
- Supported shortcuts include:
  - `RR review`: review the staged diff only
  - `RR last`: review the most recent commit (`HEAD^..HEAD`) instead of the staged diff
  - `RR review all`: review the whole worktree
  - `RR commit`: generate a commit message from the staged diff only
  - `RR review+commit`: review first, then provide a commit message only if no findings remain
  - `RR summary`: summarize what is currently staged
  - `RR tag-check`: check whether the current state is ready for a release tag
  - `RR tag`: suggest a tag name and tag commands
  - `RR release-notes`: draft changelog or release-note text from the staged diff or recent release context
  - `RR branch-check`: check whether the current work looks suitable for `main` or a short-lived branch instead

## Composite Shortcuts

- `TDR last`: run a three-pass role-based check against the most recent commit (`HEAD^..HEAD`) in this order: `TT last`, `DOC last`, `RR last`. If any pass finds a fixable issue, fix it, then restart from `TT last` and repeat until all three passes report no remaining findings or a real blocker requires user input.
- Use [`.agents/skills/tdr-last-cycle/SKILL.md`](.agents/skills/tdr-last-cycle/SKILL.md) when the task is centered on running or repeating this composite loop.

## Execution Planning Role

- Read [docs/contributing/execution-planning-agent-guidance.md](docs/contributing/execution-planning-agent-guidance.md) before assessing current phase, completed scope, remaining work, or next recommended slice.
- Ground execution guidance in code, tests, Swagger, staged changes, and status docs before trusting milestone text alone.
- Output concrete next tasks, validation steps, and doc-update expectations without dropping into code unless the requester asks for implementation.
- If the current plan, architecture, or milestone sequencing no longer matches repository reality, call that out explicitly and point to the docs that should change.
- When code, tests, and the current public contract have already consumed the roadmap's old near-term next step, prefer `EP adjust` or phase-doc sync before issuing a fresh `EP next`.
- Use [`.agents/skills/phase-status-sync/SKILL.md`](.agents/skills/phase-status-sync/SKILL.md) when the task is centered on keeping `docs/project-status.md`, `docs/roadmap.md`, `docs/project-plan.md`, and `docs/product-strategy.md` aligned without repeating current-state text across planning docs.
- Shortcut prefix: `EP`
- Supported shortcuts include:
  - `EP now`: summarize current phase, completed scope, and remaining work
  - `EP next`: recommend the next concrete slice and implementation steps
  - `EP plan-check`: compare status, roadmap, and plan for drift or mismatch
  - `EP adjust`: suggest plan, milestone, or architecture adjustments when current direction no longer fits reality
