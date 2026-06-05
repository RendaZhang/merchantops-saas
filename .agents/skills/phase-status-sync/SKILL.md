---
name: phase-status-sync
description: Align phase-oriented repository docs without mixing their roles. Use when current phase, completed scope, active slice, tag baseline, next-step sequencing, or long-term product strategy changed and `docs/project-status.md`, `docs/roadmap.md`, `docs/project-plan.md`, and `docs/product-strategy.md` must be checked together for drift.
---

# Phase Status Sync

## Overview

Use this skill when the same implementation change affects status, roadmap, planning navigation, or long-term strategy docs at once. Keep it focused on role separation: current repository reality belongs in `project-status`, active release-line milestone and slice sequencing belong in `roadmap`, planning navigation belongs in `project-plan`, and long-term product strategy belongs in `product-strategy`.

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), [docs/project-plan.md](../../../docs/project-plan.md), [docs/product-strategy.md](../../../docs/product-strategy.md), and [docs/contributing/documentation-maintenance.md](../../../docs/contributing/documentation-maintenance.md) before editing.
2. Ground the update in repository reality before trusting milestone text:
   - staged changes
   - current code and tests
   - Swagger-visible public surface
   - existing release baseline text when tags are involved
3. Re-assign facts to the right file:
   - [docs/project-status.md](../../../docs/project-status.md): current phase summary, current public baseline, current limitations, current verification reality
   - [docs/roadmap.md](../../../docs/roadmap.md): active release-line milestone, active slice, candidate next slices, and stop condition
   - [docs/project-plan.md](../../../docs/project-plan.md): planning entry point only; update when navigation or planning-source links changed
   - [docs/product-strategy.md](../../../docs/product-strategy.md): long-range product and engineering strategy, strategic tracks, timeline shifts, open-source cadence, and major sequencing changes
4. Remove duplication while updating:
   - do not keep detailed current implementation inventory in `project-plan`
   - do not keep retired baseline sections or old completion checklists in `project-status`
   - do not turn `roadmap` into release history or a full endpoint catalog
5. When a milestone or tag boundary changed:
   - keep the current tag baseline aligned with [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md) and [../../../CHANGELOG.md](../../../CHANGELOG.md)
   - make phase completion, release-line handoff, and the next planning boundary read consistently across status and roadmap
6. Finish with a drift pass:
   - current phase or planning boundary matches across files
   - current tag baseline matches across files
   - `project-plan` remains a short planning entry point and does not duplicate `project-status`, `roadmap`, or `product-strategy`
   - no section in `roadmap` duplicates release history or detailed public contract text

## Slice Advancement Rule

When the previous roadmap next-step has already been consumed by code, tests, and reference docs, use the phase sync to advance the active slice explicitly rather than leaving the roadmap one step behind.

Apply this rule when:

- the active phase is unchanged
- the last near-term `roadmap` next-step is now part of the current baseline
- the remaining drift is mostly in `project-status` and `roadmap` wording rather than long-term strategy

In that case, sync the files this way:

- [docs/project-status.md](../../../docs/project-status.md): absorb the newly-stable hardening or support work into the current baseline
- [docs/roadmap.md](../../../docs/roadmap.md): promote the real next slice, next letter, or next narrow workflow step
- [docs/product-strategy.md](../../../docs/product-strategy.md): leave unchanged unless the long-term strategy itself changed
- [docs/project-plan.md](../../../docs/project-plan.md): leave unchanged unless planning navigation changed

Do not leave an already-completed hardening step described as the current next slice just because the active phase has not changed.

## Release-Line Planning Model

After the foundation baseline, do not assume future planning must follow fixed `Week N -> Slice A/B/C` calendar buckets. Use this model instead:

- `product-strategy`: owns long-term strategic tracks and why they matter
- `roadmap`: owns the active release-line milestone, active slice, candidate next slices, and stop condition
- `project-status`: owns current implementation reality, current tag baseline, verification evidence, and known gaps
- `project-plan`: stays a short planning entry page

When a future planning task asks what to do next:

- pick the narrowest evidence-backed slice from the strategic tracks
- state which release-line milestone or planning boundary it belongs to
- define the slice stop condition before widening scope
- avoid creating another long week-by-week plan unless the user explicitly asks for one

## Repo Anchors

- [docs/project-status.md](../../../docs/project-status.md)
- [docs/roadmap.md](../../../docs/roadmap.md)
- [docs/project-plan.md](../../../docs/project-plan.md)
- [docs/product-strategy.md](../../../docs/product-strategy.md)
- [docs/contributing/documentation-maintenance.md](../../../docs/contributing/documentation-maintenance.md)
- [docs/contributing/release-versioning.md](../../../docs/contributing/release-versioning.md)
- [../../../CHANGELOG.md](../../../CHANGELOG.md)

## Output Shape

- State the current phase or drift type first.
- List which of `project-status`, `roadmap`, `project-plan`, and `product-strategy` changed and why.
- Call out any remaining mismatch, or explicitly say the planning docs are aligned.
