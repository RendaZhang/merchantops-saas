# Project Guidance

This repository currently organizes handoff rules around five roles:

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
- If this file or another repository guidance page is clearly outdated, inaccurate, or incomplete, fix the guidance instead of working around it silently.
- A good promotion threshold is:
  - the same confusion or mistake has already happened more than once
  - the rule applies across tasks, not only to a single change
  - future agents are likely to benefit from seeing it before they start work
- If this file is updated, keep linked reference pages and runbooks aligned in the same change when needed.
- Do not use this file as the only place for product, architecture, permission-policy, or release-policy decisions; those still belong in the relevant docs and ADRs.

## Cross-Role Documentation Ownership

- The roles in this file are working perspectives, not ownership walls.
- Any role may update documentation when the task requires it, but should do so from the perspective of that role and still follow [docs/contributing/documentation-maintenance.md](docs/contributing/documentation-maintenance.md).
- Documentation-focused work should keep structure, navigation, and public-vs-internal wording clear.
- Testing-focused work may update runbooks, automated test guidance, regression checklists, and related reference docs when coverage or verification reality changes.
- Development-focused work may update docs when implementation changes, but must not present internal groundwork as public API.
- Review and release work may update changelog, versioning, release notes, and milestone summaries when the staged diff supports it.
- Execution-planning work may update status, roadmap, plan, and architecture guidance when it finds phase drift, scope mismatch, or planning assumptions that no longer match repository reality.
- If a role updates a doc outside its usual focus area, it should keep the change scoped to the facts learned through that role's work rather than rewriting unrelated narrative.

## Documentation Role

- Read [docs/contributing/documentation-maintenance.md](docs/contributing/documentation-maintenance.md) before making documentation changes.
- Read [docs/contributing/development-agent-guidance.md](docs/contributing/development-agent-guidance.md) before making development-facing documentation changes.
- Keep root `README.md` high-level and move development detail into `docs/`.
- Do not document an endpoint as public unless it is visible in Swagger.
- Keep runbooks and reference examples honest about verification scope: only claim checks that the documented commands actually execute, and label automated-only or manual-only negative paths explicitly.
- When a doc uses seeded IDs, demo accounts, or other environment-sensitive sample data, state whether it assumes a fresh local database or whether the reader should paste an ID from a live response.
- Follow the routing rules in the linked maintenance and development guidance pages instead of re-encoding detailed update matrices here.
- Shortcut prefix: `DOC`
- Supported shortcuts include:
  - `DOC staged`: inspect the staged diff first and identify which docs must change
  - `DOC last`: inspect the most recent commit (`HEAD^..HEAD`) first and identify which docs should have changed or still need follow-up updates
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
- Keep smoke guidance scoped to what it really executes. If a negative path stays covered only by automated tests or a broader regression checklist, say that directly instead of implying the smoke flow already exercised it.
- If a staged change adds or edits a Flyway migration, do not stop at H2 or manually-created test schemas; verify the migration effect against the real local MySQL path as part of testing sign-off.
- Treat `TT staged` as a testing-focused staged review entry point: inspect the staged diff, run or choose the smallest sufficient verification set, and report findings ordered by urgency (`P1`, `P2`, `P3`) rather than stopping at scope mapping alone.
- Shortcut prefix: `TT`
- Supported shortcuts include:
  - `TT staged`: review the staged diff from a testing perspective, run or select the smallest sufficient verification, and report prioritized findings plus remaining verification gaps
  - `TT last`: review the most recent commit (`HEAD^..HEAD`) from a testing perspective, map it to affected test layers, and report prioritized findings plus remaining verification gaps
  - `TT test`: run or recommend the default automated regression command `.\mvnw.cmd -pl merchantops-api -am test`
  - `TT coverage`: summarize current automated coverage, manual-only gaps, and the right next verification step
  - `TT smoke`: run or guide the local smoke flow from [docs/runbooks/local-smoke-test.md](docs/runbooks/local-smoke-test.md)
  - `TT live`: prepare live smoke prerequisites with `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`, then start the API from `merchantops-api` with `..\mvnw.cmd spring-boot:run`
  - `TT auth`: focus verification on login, JWT, `/api/v1/users` (`GET` and `POST`), and password-rule regressions
  - `TT sql`: focus verification on native SQL and H2 `MODE=MySQL` test realism, including datasource-replacement pitfalls
  - `TT clean`: provide or execute cleanup steps for generated smoke users after manual verification

## Development Role

- Read [docs/contributing/development-agent-guidance.md](docs/contributing/development-agent-guidance.md) before changing tenant-scoped repositories, services, DTOs, or user-management internals.
- Keep tenant scoping explicit and keep query/write models separated.
- Treat `operatorId` the same way as `tenantId` for tenant-scoped writes when operator attribution matters: resolve it at the controller edge, pass it explicitly through service methods, and keep attribution fields internal unless Swagger exposes them on purpose.
- Do not present internal groundwork as public API.
- If implementation changes affect public contract or reusable repo rules, update the linked docs in the same change.

## Review and Release Role

- Read [docs/contributing/review-release-agent-guidance.md](docs/contributing/review-release-agent-guidance.md) before doing staged review, commit-message suggestion, or release/tag work.
- Default review scope is the staged diff only.
- Treat pasted findings from earlier review rounds as hints, not as still-open facts. Re-check whether they still apply to the current staged diff before repeating or carrying them forward.
- Keep changelog, release versioning, and milestone docs aligned with the reviewed change set.
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

## Execution Planning Role

- Read [docs/contributing/execution-planning-agent-guidance.md](docs/contributing/execution-planning-agent-guidance.md) before assessing current phase, completed scope, remaining work, or next recommended slice.
- Ground execution guidance in code, tests, Swagger, staged changes, and status docs before trusting milestone text alone.
- Output concrete next tasks, validation steps, and doc-update expectations without dropping into code unless the requester asks for implementation.
- If the current plan, architecture, or milestone sequencing no longer matches repository reality, call that out explicitly and point to the docs that should change.
- Shortcut prefix: `EP`
- Supported shortcuts include:
  - `EP now`: summarize current phase, completed scope, and remaining work
  - `EP next`: recommend the next concrete slice and implementation steps
  - `EP plan-check`: compare status, roadmap, and plan for drift or mismatch
  - `EP adjust`: suggest plan, milestone, or architecture adjustments when current direction no longer fits reality
