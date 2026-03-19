---
name: release-tag-prep
description: Prepare or finalize release-tag documentation for this repository. Use when a week or milestone is being declared complete, a tag is being planned or just created, changelog notes must move between Unreleased and a versioned section, or README, project-status, roadmap, project-plan, and release-versioning need to reflect the current tagged baseline.
---

# Release Tag Prep

## Overview

Use this skill to keep tag-related docs consistent without mixing release history, current status, near-term roadmap, and long-range planning. Prefer explicit dates and real tag evidence over assumptions.

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [../../../CHANGELOG.md](../../../CHANGELOG.md), [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md), [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), [docs/project-plan.md](../../../docs/project-plan.md), and [../../../README.md](../../../README.md).
2. Determine the release context before editing:
   - pre-tag preparation
   - post-tag cleanup
   - release-notes drafting
   - baseline consistency check
3. Respect document ownership:
   - [../../../CHANGELOG.md](../../../CHANGELOG.md): release-level history only
   - [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md): versioning rules and near-term tag line
   - [docs/project-status.md](../../../docs/project-status.md): current repository reality
   - [docs/roadmap.md](../../../docs/roadmap.md): active phase and next steps
   - [docs/project-plan.md](../../../docs/project-plan.md): long-range milestones only
   - [../../../README.md](../../../README.md): high-level current baseline only
4. Prepare docs before a tag:
   - keep `Unreleased` limited to release-level changes supported by the current diff
   - align phase completion and next active phase in status and roadmap
   - update high-level baseline text without claiming the tag already exists unless the user explicitly confirmed it or git proves it
5. Finalize docs after a tag is confirmed:
   - move release-worthy notes from `Unreleased` into a dated version section
   - update current tagged milestone references across README, status, roadmap, and release-versioning
   - demote the prior baseline to previous or earlier milestone wording
6. Keep versioning realistic:
   - do not reuse an existing tag
   - do not write speculative future tags as if they already happened
   - keep absolute dates explicit

## Repo Anchors

- [../../../CHANGELOG.md](../../../CHANGELOG.md)
- [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md)
- [docs/project-status.md](../../../docs/project-status.md)
- [docs/roadmap.md](../../../docs/roadmap.md)
- [docs/project-plan.md](../../../docs/project-plan.md)
- [../../../README.md](../../../README.md)

## Output Shape

- State whether the work is pre-tag, post-tag, or a consistency pass.
- State the current tagged baseline and the prepared or confirmed next tag when relevant.
- List the files updated.
- Call out any remaining pre-tag or post-tag gaps.
