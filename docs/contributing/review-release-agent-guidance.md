# Review And Release Agent Guidance

Last updated: 2026-03-10

## Purpose

This page records the current staged review, commit, and release guidance for review-focused agents and contributors.

Use it when reviewing staged changes, preparing commit messages, or checking whether a release/tag suggestion is appropriate.

## Shortcut Commands

The repository shorthand for this role is `RR`.

Use these commands as the default interpretation:

- `RR review`: review the staged diff only
- `RR last`: review the most recent commit (`HEAD^..HEAD`) instead of the staged diff
- `RR review all`: review the whole worktree instead of only staged changes
- `RR commit`: generate a commit message from the staged diff only
- `RR review+commit`: review first, then provide a commit message only if no findings remain
- `RR summary`: summarize the current staged diff before review or commit work
- `RR tag-check`: check whether the current branch, worktree, and release docs are ready for a tag
- `RR tag`: suggest a tag name and the corresponding tag commands
- `RR release-notes`: draft release-note or changelog text from the current staged diff or release context
- `RR branch-check`: assess whether the current work should go directly to `main` or live on a short-lived branch

Equivalent plain-language requests are also valid, for example "review the staged diff as RR" or "use RR role to check whether we can tag now".

## Review Scope Rules

- Default review scope is the staged diff only: `git diff --cached`
- For `RR last`, default review scope is the most recent commit only: `git show --stat --patch HEAD^..HEAD`
- Do not mix unstaged or untracked files into findings unless the requester explicitly asks for full-worktree review
- If the requester pastes findings from an earlier review round, treat them as context only and re-check whether they still apply to the current staged diff or commit scope before repeating them
- If findings exist, report fixes and risks first
- If no findings exist, say so explicitly and still mention remaining testing or verification gaps

## Commit Guidance

- Generate commit messages from the staged diff only, not from the whole working tree
- Prefer Conventional Commit style such as `feat(...)`, `fix(...)`, `docs(...)`, `test(...)`, `refactor(...)`, or `chore(...)`
- If the staged diff mixes unrelated scopes, call that out before suggesting a single commit message

## Release And Tag Guidance

- Only create or suggest release tags from `main`
- Only create or suggest release tags from a clean working tree
- Prefer annotated tags (`git tag -a`) over lightweight tags
- Do not tag a feature or topic branch unless the requester explicitly asks for it
- Before creating or suggesting a new tag, verify that [../../CHANGELOG.md](../../CHANGELOG.md) and [release-versioning.md](release-versioning.md) are aligned with that milestone

## Branch Guidance

- For non-urgent work that is not intended to land directly on `main`, prefer a short-lived descriptive branch with the required `codex/` prefix, such as `codex/feature/...`, `codex/fix/...`, `codex/docs/...`, or `codex/test/...`

## Documentation Touchpoints

Review and release work commonly requires doc updates in these places:

- [../../CHANGELOG.md](../../CHANGELOG.md): release-level notable changes
- [release-versioning.md](release-versioning.md): current tag baseline and next-version rules
- [../project-status.md](../project-status.md): if the current tagged milestone changed
- [../roadmap.md](../roadmap.md): if the planned release sequence changed

## Related Documents

- [release-versioning.md](release-versioning.md): version and tag rules
- [documentation-maintenance.md](documentation-maintenance.md): which docs must change for release and milestone changes
- [../../CHANGELOG.md](../../CHANGELOG.md): release-level change history
