---
name: release-tag-prep
description: Prepare or finalize release-tag documentation for this repository, including open-source release-cut commits. Use when a week or milestone is being declared complete, a tag is being planned or just created, a release-cut commit will be tagged immediately, changelog notes must move between Unreleased and a versioned section, or README, project-status, roadmap, project-plan, release-versioning, and open-source entry files must reflect the current tagged baseline.
---

# Release Tag Prep

## Overview

Use this skill to keep tag-related docs consistent without mixing release history, current status, near-term roadmap, and long-range planning. Prefer explicit dates and real tag evidence over assumptions.

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [../../../CHANGELOG.md](../../../CHANGELOG.md), [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md), [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), [docs/project-plan.md](../../../docs/project-plan.md), and [../../../README.md](../../../README.md).
2. Determine the release context before editing:
   - pre-tag preparation
   - post-tag cleanup
   - open-source release-cut commit
   - release-notes drafting
   - baseline consistency check
3. Respect document ownership:
   - [../../../CHANGELOG.md](../../../CHANGELOG.md): release-level history only
   - [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md): versioning rules and near-term tag line
   - [docs/project-status.md](../../../docs/project-status.md): current repository reality
   - [docs/roadmap.md](../../../docs/roadmap.md): active phase and next steps
   - [docs/project-plan.md](../../../docs/project-plan.md): long-range milestones only
   - [../../../README.md](../../../README.md): high-level current baseline only
   - `LICENSE`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, and `.github` templates: open-source entry and community expectations for a public release cut
4. Prepare docs before a tag:
   - keep `Unreleased` limited to release-level changes supported by the current diff
   - align phase completion and next active phase in status and roadmap
   - update high-level baseline text without claiming the tag already exists unless the user explicitly confirmed it or git proves it
   - treat the tag-ready minimum doc set as mandatory: `CHANGELOG.md`, `docs/contributing/release-versioning.md`, `docs/project-status.md`, and `docs/roadmap.md`
   - also update `README.md` when the current tagged milestone summary changed
   - also update `docs/project-plan.md` when milestone framing, release cadence, or the current planning anchor changed
5. Handle open-source release-cut work when the same commit will be tagged immediately:
   - allow tagged-state wording only when the user explicitly confirmed the release-cut flow
   - ensure changelog, release-versioning, README, status, roadmap, and plan all reflect the same current tag and prior baseline
   - also confirm the public-release entry set exists and matches the cut: `LICENSE`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, and `.github` issue or PR templates
   - call out any missing public-release entry file instead of treating the release cut as complete
6. Finalize docs after a tag is confirmed:
   - move release-worthy notes from `Unreleased` into a dated version section
   - update current tagged milestone references across README, status, roadmap, and release-versioning
   - demote the prior baseline to previous or earlier milestone wording
7. Keep versioning realistic:
   - do not reuse an existing tag
   - do not write speculative future tags as if they already happened
   - if docs are written in tagged-state wording during a release-cut commit, the tag must be created on that same commit immediately after it lands
   - keep absolute dates explicit

## Repo Anchors

- [../../../CHANGELOG.md](../../../CHANGELOG.md)
- [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md)
- [docs/project-status.md](../../../docs/project-status.md)
- [docs/roadmap.md](../../../docs/roadmap.md)
- [docs/project-plan.md](../../../docs/project-plan.md)
- [../../../README.md](../../../README.md)

## Tag-Ready Minimum Doc Set

Treat these checks as required, not optional, whenever a tag is being prepared or confirmed:

- [../../../CHANGELOG.md](../../../CHANGELOG.md): the release-worthy notes must be in the correct version section for the tag state you are using
- [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md): current tag, previous baseline, and progression wording must match the intended cut
- [docs/project-status.md](../../../docs/project-status.md): current tagged baseline and active phase must match repository reality
- [docs/roadmap.md](../../../docs/roadmap.md): next active phase and near-term sequencing must match the post-milestone handoff

Review and update when the change supports it:

- [../../../README.md](../../../README.md): current tagged milestone summary and high-level capability baseline
- [docs/project-plan.md](../../../docs/project-plan.md): planning anchor, release cadence, and milestone framing

For open-source release cuts, also confirm:

- `LICENSE`
- `CONTRIBUTING.md`
- `CODE_OF_CONDUCT.md`
- `SECURITY.md`
- `.github` issue and PR templates

## Output Shape

- State whether the work is pre-tag, post-tag, open-source release-cut, or a consistency pass.
- State the current tagged baseline and the target or confirmed next tag when relevant.
- List the files updated.
- State whether the tag-ready minimum doc set is complete, and name any missing file explicitly.
- Call out whether any open-source entry file is still missing when the task is a release cut.
- Call out any remaining pre-tag or post-tag gaps.
