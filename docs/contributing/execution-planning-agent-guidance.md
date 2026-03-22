# Execution Planning Agent Guidance

Last updated: 2026-03-22

## Purpose

This page defines how execution-planning work should guide the next implementation steps without jumping straight into code.

Use it when the requester wants help answering questions such as:

- what phase the project is currently in
- what is already done
- what remains unfinished
- what the next concrete slice should be
- whether the plan, milestones, or architecture should be adjusted

## Default Outcome

The default output should be a developer-facing execution brief, not an implementation patch.

That brief should explain:

- the current phase
- the completed scope
- the unfinished scope
- the next recommended slice
- the concrete next steps
- the expected validation and documentation follow-up
- whether plan or architecture changes are recommended

Do not drop into code unless the requester explicitly asks for implementation.

## Source Priority

Use these sources in this order when deciding what is actually true:

1. code, tests, Swagger/OpenAPI, and the current staged diff
2. [../project-status.md](../project-status.md)
3. [../roadmap.md](../roadmap.md)
4. [../project-plan.md](../project-plan.md)
5. [../../README.md](../../README.md)

Rules:

- do not mark work as complete only because it appears in a plan
- if higher-confidence sources conflict with lower-confidence planning text, report the conflict explicitly
- if docs drift from repository reality, recommend the doc updates that should happen next

## Phase Evidence Pass

Before writing an `EP now`, `EP next`, `EP plan-check`, or `EP adjust` answer, build a short evidence pass from repository reality.

At minimum, answer these four questions first:

1. what is the latest non-doc-only code slice at `HEAD` or immediately below it
2. what phase or milestone the current tag baseline actually records
3. whether the most recent default regression pass succeeded, and when that happened
4. whether `project-status`, `roadmap`, or `project-plan` still describe an old near-term next step as if it were unfinished

Preferred evidence sources:

- latest non-doc-only commit from `git log`
- current tag baseline from git plus [../project-status.md](../project-status.md)
- latest default regression result from the current shell session, CI evidence, or a fresh local run when needed
- current public contract from code and Swagger-visible controller interfaces

Use this pass to anchor the phase readout before comparing milestone text.

## Execution Planning Workflow

1. run the Phase Evidence Pass first
2. determine the current phase from [../project-status.md](../project-status.md)
3. compare the current implementation and public API surface against [../roadmap.md](../roadmap.md) and [../project-plan.md](../project-plan.md)
4. separate findings into `completed`, `in progress`, `internal groundwork`, and `not started`
5. identify the smallest sensible next slice that moves the current phase forward
6. describe the next tasks in implementation order
7. list the validation, documentation, or planning follow-up that should happen with that slice
8. decide whether the current plan or architecture should stay as-is or be adjusted

## Output Shape

Use this shape by default:

- `Current Phase`
- `Completed`
- `Still Missing`
- `Next Recommended Slice`
- `Concrete Next Steps`
- `Validation`
- `Docs To Update`
- `Plan Or Architecture Adjustment`

Keep the steps specific enough that a developer can act on them immediately, but stop short of writing code unless asked.

## Scope And Adjustment Rules

Prefer the smallest vertical slice that keeps the project moving toward a real workflow milestone.

Recommend a plan adjustment when:

- the near-term phase scope no longer matches repository reality
- a later milestone depends on prerequisites that are still missing
- repeated work reveals that the current milestone split is awkward or misleading
- a planned AI item no longer fits workflow-first value, tenant isolation, RBAC, audit, or evaluation constraints

### When `EP next` Becomes `EP adjust`

Do not keep reusing old roadmap wording when the code, tests, and public contract have already consumed that near-term slice.

If the current implementation has effectively completed the roadmap's old `next slice`, do this instead:

1. run a small `EP plan-check`
2. confirm whether the drift is mainly in `project-status`, `roadmap`, or related reference wording
3. recommend `EP adjust` or phase-doc sync first
4. only after the near-term framing is corrected, give the new `EP next`

This is especially important when slice lettering or naming has moved forward in code reality but the roadmap still points at the prior slice.

Recommend an architecture adjustment when:

- the next slice cannot be implemented cleanly within current tenant or RBAC boundaries
- repeated implementation patterns imply a missing architecture rule
- a cross-cutting concern should be captured as an ADR instead of repeating ad hoc guidance

Do not silently rewrite the plan. Explain why an adjustment is needed and which document should change.

## Boundaries

- do not replace development work with vague planning language
- do not present internal groundwork as public capability
- do not recommend broad scope expansion when a smaller milestone slice is enough
- do not optimize for AI novelty over workflow value and governance

## Documentation Touchpoints

Execution-planning work commonly requires updates in these places:

- [../project-status.md](../project-status.md): current implementation reality
- [../roadmap.md](../roadmap.md): near-term next steps
- [../project-plan.md](../project-plan.md): milestone shape and longer-range sequencing
- [documentation-maintenance.md](documentation-maintenance.md): if routing rules or doc workflow expectations changed
- [../architecture/README.md](../architecture/README.md) and `docs/architecture/adr/`: if the recommendation implies a new architecture decision

## Related Documents

- [documentation-maintenance.md](documentation-maintenance.md): doc-routing rules
- [development-agent-guidance.md](development-agent-guidance.md): implementation-side constraints
- [testing-agent-guidance.md](testing-agent-guidance.md): verification baseline and regression expectations
- [review-release-agent-guidance.md](review-release-agent-guidance.md): staged review and release rules
- [../project-status.md](../project-status.md): current repository reality
- [../roadmap.md](../roadmap.md): next-phase intent
- [../project-plan.md](../project-plan.md): longer-range milestone plan
