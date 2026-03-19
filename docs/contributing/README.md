# Contributing

This section holds contributor and agent workflow guidance for documentation, development, testing, review, release, and execution planning work.

Keep [../../AGENTS.md](../../AGENTS.md) as the short repository entry point. Use the pages here for the detailed, reusable rules that sit behind it.

If a rule starts turning into implementation detail, runbook nuance, or verification edge-case guidance, keep that detail here and leave [../../AGENTS.md](../../AGENTS.md) as the shorter repository-level summary.

## Pages

- [documentation-maintenance.md](documentation-maintenance.md): routing rules for which docs must change when implementation, API, release, or architecture changes happen
- [development-agent-guidance.md](development-agent-guidance.md): tenant-scoped implementation and contributor guidance for coding work
- [testing-agent-guidance.md](testing-agent-guidance.md): verification, regression, and coverage guidance for testing-focused work
- [review-release-agent-guidance.md](review-release-agent-guidance.md): staged review, commit, and release guidance for review-focused work
- [execution-planning-agent-guidance.md](execution-planning-agent-guidance.md): current-phase assessment, next-step planning, and plan-adjustment guidance
- [release-versioning.md](release-versioning.md): current tag baseline, changelog ownership, and recommended version progression

## Repo Skills

- [../../.agents/skills/README.md](../../.agents/skills/README.md): repo-local skill index for repeated multi-step workflows that are too detailed to keep in [../../AGENTS.md](../../AGENTS.md)

## Suggested Entry Points

- start with [../../AGENTS.md](../../AGENTS.md) if you are new to the repository
- read [documentation-maintenance.md](documentation-maintenance.md) before changing multiple docs or updating documentation structure
- read [development-agent-guidance.md](development-agent-guidance.md) before changing tenant-scoped code or development-facing docs
- read [testing-agent-guidance.md](testing-agent-guidance.md) before changing tests, automated coverage notes, or regression guidance
- read [review-release-agent-guidance.md](review-release-agent-guidance.md) before staged review, commit-message suggestion, or release/tag work
- read [execution-planning-agent-guidance.md](execution-planning-agent-guidance.md) before deciding what the current phase means, what remains unfinished, or whether the plan should change
- read [release-versioning.md](release-versioning.md) when planning tags, release notes, or milestone numbering

## Role Shorthand

- `DOC`: Documentation role shortcut for doc routing, synchronization, navigation, and consistency checks
- `TT`: Testing role shortcut for staged verification, automated regression, smoke checks, and test-coverage review
- `RR`: Review and Release role shortcut for staged review, commit-message suggestion, branch checks, and tag/release checks
- `EP`: Execution Planning role shortcut for current-phase assessment, next-step planning, and drift checks

When a shortcut keeps triggering the same multi-step workflow, check [../../.agents/skills/README.md](../../.agents/skills/README.md) for a matching repo-local skill before expanding the rule set in [../../AGENTS.md](../../AGENTS.md).

Common variants:

- `DOC pre-tag`: run the final release-doc sync before a tag cut; use prepared-next-tag wording by default, or tagged-state wording only when the same commit will be tagged immediately
- `DOC staged` / `DOC last`: inspect the staged diff or the most recent commit first, then route required doc updates
- `TT staged` / `TT last`: inspect the staged diff or the most recent commit first, then choose the smallest sufficient verification set
- `RR review` / `RR last`: review the staged diff or the most recent commit instead of the full worktree

## Composite Shortcuts

- `TDR last`: composite last-commit check; see [../../.agents/skills/tdr-last-cycle/SKILL.md](../../.agents/skills/tdr-last-cycle/SKILL.md) for the loop and restart details
