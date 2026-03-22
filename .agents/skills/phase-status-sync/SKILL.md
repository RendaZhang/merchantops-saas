---
name: phase-status-sync
description: Align phase-oriented repository docs without mixing their roles. Use when current phase, completed scope, active slice, tag baseline, or next-step sequencing changed and `docs/project-status.md`, `docs/roadmap.md`, and `docs/project-plan.md` must be updated together or checked for drift.
---

# Phase Status Sync

## Overview

Use this skill when the same implementation change affects status, roadmap, and planning docs at once. Keep it focused on role separation: current repository reality belongs in `project-status`, active next steps belong in `roadmap`, and long-range sequencing belongs in `project-plan`.

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), [docs/project-plan.md](../../../docs/project-plan.md), and [docs/contributing/documentation-maintenance.md](../../../docs/contributing/documentation-maintenance.md) before editing.
2. Ground the update in repository reality before trusting milestone text:
   - staged changes
   - current code and tests
   - Swagger-visible public surface
   - existing release baseline text when tags are involved
3. Re-assign facts to the right file:
   - [docs/project-status.md](../../../docs/project-status.md): current phase summary, current public baseline, current limitations, current verification reality
   - [docs/roadmap.md](../../../docs/roadmap.md): active phase, next recommended steps, near-term sequence
   - [docs/project-plan.md](../../../docs/project-plan.md): long-range milestone intent, timeline shifts, open-source cadence, major sequencing changes
4. Remove duplication while updating:
   - do not keep detailed current implementation inventory in `project-plan`
   - do not keep retired baseline sections or old completion checklists in `project-status`
   - do not turn `roadmap` into release history or a full endpoint catalog
5. When a milestone or tag boundary changed:
   - keep the current tag baseline aligned with [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md) and [../../../CHANGELOG.md](../../../CHANGELOG.md)
   - make phase completion and the next active phase read the same across status and roadmap
6. Finish with a drift pass:
   - current phase name matches across files
   - current tag baseline matches across files
   - no section in `project-plan` duplicates `project-status`
   - no section in `roadmap` duplicates release history or detailed public contract text

## Slice Advancement Rule

When the previous roadmap next-step has already been consumed by code, tests, and reference docs, use the phase sync to advance the active slice explicitly rather than leaving the roadmap one step behind.

Apply this rule when:

- the active phase is unchanged
- the last near-term `roadmap` next-step is now part of the current baseline
- the remaining drift is mostly in `project-status` and `roadmap` wording rather than long-range milestone intent

In that case, sync the files this way:

- [docs/project-status.md](../../../docs/project-status.md): absorb the newly-stable hardening or support work into the current baseline
- [docs/roadmap.md](../../../docs/roadmap.md): promote the real next slice, next letter, or next narrow workflow step
- [docs/project-plan.md](../../../docs/project-plan.md): leave unchanged unless the long-range milestone shape itself changed

Do not leave an already-completed hardening step described as the current next slice just because the active phase has not changed.

## Repo Anchors

- [docs/project-status.md](../../../docs/project-status.md)
- [docs/roadmap.md](../../../docs/roadmap.md)
- [docs/project-plan.md](../../../docs/project-plan.md)
- [docs/contributing/documentation-maintenance.md](../../../docs/contributing/documentation-maintenance.md)
- [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md)
- [../../../CHANGELOG.md](../../../CHANGELOG.md)

## Output Shape

- State the current phase or drift type first.
- List which of `project-status`, `roadmap`, and `project-plan` changed and why.
- Call out any remaining mismatch, or explicitly say the three files are aligned.
