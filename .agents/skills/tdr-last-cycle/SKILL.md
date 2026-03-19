---
name: tdr-last-cycle
description: Run the repository's three-pass last-commit review cycle. Use when a task maps to `TDR last` and the most recent commit must be checked from testing, documentation, and review-release perspectives, with fixable issues resolved and the cycle repeated until clean or blocked.
---

# TDR Last Cycle

## Overview

Use this skill when the most recent commit needs a disciplined cleanup pass before handoff, merge, or tag work. It keeps the `TT last -> DOC last -> RR last` order stable and makes the restart rule explicit when one pass finds a fixable issue.

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [docs/contributing/testing-agent-guidance.md](../../../docs/contributing/testing-agent-guidance.md), [docs/contributing/documentation-maintenance.md](../../../docs/contributing/documentation-maintenance.md), and [docs/contributing/review-release-agent-guidance.md](../../../docs/contributing/review-release-agent-guidance.md) before acting.
2. Set the review scope to `HEAD^..HEAD`. Do not silently widen to the whole worktree unless the user asked for that.
3. Run the three passes in order:
   - `TT last`: test-focused review of the most recent commit, including the smallest sufficient verification choice and prioritized findings
   - `DOC last`: documentation routing and alignment check for the most recent commit
   - `RR last`: review-release check of the most recent commit, using current diff reality rather than stale earlier findings
4. If one pass finds a fixable issue:
   - fix it
   - update affected docs or runbooks in the same change when required
   - restart from `TT last` instead of continuing from the later pass
5. Stop the loop only when:
   - all three passes report no remaining findings, or
   - a real blocker requires user input or a product decision
6. Keep the output scoped and disciplined:
   - findings first, ordered by urgency
   - then any fixes applied
   - then remaining verification gaps or blockers
7. Do not claim the cycle is clean if one pass was skipped or if a known finding was left unresolved without calling it out.

## Repo Anchors

- [docs/contributing/testing-agent-guidance.md](../../../docs/contributing/testing-agent-guidance.md)
- [docs/contributing/documentation-maintenance.md](../../../docs/contributing/documentation-maintenance.md)
- [docs/contributing/review-release-agent-guidance.md](../../../docs/contributing/review-release-agent-guidance.md)
- [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md)
- [../../../CHANGELOG.md](../../../CHANGELOG.md)

## Output Shape

- State that the scope is the most recent commit first.
- Report pass status in `TT -> DOC -> RR` order.
- If a fix was applied, say that the cycle restarted.
- End with either `cycle clean` or the real remaining blocker.
