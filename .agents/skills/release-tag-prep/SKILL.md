---
name: release-tag-prep
description: Prepare or finalize release-tag documentation for this repository, including open-source release-cut commits. Use when a week or milestone is being declared complete, a tag is being planned or just created, a release-cut commit will be tagged immediately, changelog notes must move between Unreleased and a versioned section, or README, project-status, roadmap, planning entry, product-strategy, release-versioning, and open-source entry files must reflect the current tagged baseline.
---

# Release Tag Prep

## Overview

Use this skill to keep tag-related docs consistent without mixing release history, current status, near-term roadmap, and long-range planning. Prefer explicit dates and real tag evidence over assumptions.

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [../../../CHANGELOG.md](../../../CHANGELOG.md), [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md), [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), [docs/project-plan.md](../../../docs/project-plan.md), [docs/product-strategy.md](../../../docs/product-strategy.md), and [../../../README.md](../../../README.md).
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
   - [docs/roadmap.md](../../../docs/roadmap.md): active release-line milestone, active slice, candidate next slices, and stop condition
   - [docs/project-plan.md](../../../docs/project-plan.md): planning entry point only
   - [docs/product-strategy.md](../../../docs/product-strategy.md): long-range product and engineering strategy
   - [../../../README.md](../../../README.md): high-level current baseline only
   - `LICENSE`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, and `.github` templates: open-source entry and community expectations for a public release cut
4. Prepare docs before a tag:
   - for `DOC pre-tag`, treat the work as the release-cut commit that will be tagged immediately unless the requester explicitly asked only for an earlier planning or tag-check pass
   - keep `Unreleased` limited to release-level changes supported by the current diff until the release-cut commit is ready
   - align phase completion, release-line handoff, and next planning boundary in status and roadmap
   - when the release-cut commit is being prepared, update the minimum doc set to the intended new tagged state: `CHANGELOG.md`, `docs/contributing/release-versioning.md`, `docs/project-status.md`, and `docs/roadmap.md`
   - also update `README.md` when the current tagged milestone summary changed, including any top-of-file version banner, release line, or other high-visibility baseline wording
   - update `docs/product-strategy.md` when milestone framing, release cadence, or long-term direction changed
   - update `docs/project-plan.md` only when planning navigation changed
   - stage the release-doc changes together and generate commit guidance for that release-cut commit
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
- [docs/product-strategy.md](../../../docs/product-strategy.md)
- [../../../README.md](../../../README.md)

## Tag-Ready Minimum Doc Set

Treat these checks as required, not optional, whenever a tag is being prepared or confirmed:

- [../../../CHANGELOG.md](../../../CHANGELOG.md): the release-worthy notes must be in the correct version section for the tag state you are using
- [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md): current tag, previous baseline, and progression wording must match the intended cut
- [docs/project-status.md](../../../docs/project-status.md): current tagged baseline and active phase must match repository reality
- [docs/roadmap.md](../../../docs/roadmap.md): active release-line milestone, active slice, and candidate next slices must match the post-milestone handoff

Review and update when the change supports it:

- [../../../README.md](../../../README.md): current tagged milestone summary, top-of-file version banner, and high-level capability baseline
- [docs/product-strategy.md](../../../docs/product-strategy.md): release cadence and milestone framing
- [docs/project-plan.md](../../../docs/project-plan.md): planning navigation

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
