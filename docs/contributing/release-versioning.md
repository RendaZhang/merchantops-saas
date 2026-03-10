# Release Versioning

Last updated: 2026-03-10

## Purpose

This page defines how MerchantOps SaaS should record versions, tags, and release milestones as it moves from portfolio build to open-source release.

## Current Tagged Baseline

- current tag: `v0.1.0`
- tag date: 2026-03-09
- tag message: `Week 1 complete: foundation phase`

This tag represents the completion of Week 1 Platform Foundation. It is the first explicit repository milestone and should be treated as the baseline for all later version planning.

## Source Of Truth

Use the following ownership model:

- Git tag: the authoritative release or milestone identifier
- [../../CHANGELOG.md](../../CHANGELOG.md): the authoritative human-readable release history
- [../project-status.md](../project-status.md): the authoritative description of current implementation status
- [../roadmap.md](../roadmap.md): the authoritative description of intended next-phase work

Do not let version numbers live only inside planning text.

## Changelog Shape

The changelog should stay version-oriented:

- keep an `Unreleased` section for staged or upcoming release-level notes
- keep one section per tag or published release
- summarize only notable external changes, not every implementation step

Detailed build activity should live in commit history, PR descriptions, or separate engineering notes instead of release changelog entries.

## Recommended Scheme

Use a SemVer-shaped scheme with optional pre-release labels:

- `v0.x.y` while the project is still evolving quickly
- `-alpha` for early previews that are intentionally incomplete
- `-beta` for broader public releases that are usable but still expected to change
- move to `v1.0.0` only after the public workflow and AI surface is stable enough to support outside users without constant contract churn

## Recommended Near-Term Progression

The exact tag names can change, but the next milestones should not reuse `v0.1.0`.

Recommended direction:

- `v0.1.0`: Week 1 Platform Foundation complete
- `v0.2.0-alpha`: first open-source preview after Week 5 workflow and async-operation backbone
- `v0.3.0-beta`: first public AI-enhanced release after Week 6 or Week 7
- later `v0.x` releases: governance, usage tracking, hardening, and packaging milestones

## Practical Rules

- create a tag only when the repository state is intentionally presentable
- every new tag should have a matching changelog note
- if the milestone changes the public API or delivery story meaningfully, bump the minor version
- if the change is a small correction inside the same release line, bump the patch version
- avoid creating too many tags for half-finished internal checkpoints

## Documentation Rule

When a new release tag is created:

1. update [../../CHANGELOG.md](../../CHANGELOG.md)
2. update any docs that mention the current release baseline or next planned release
3. keep planning docs consistent with already-used version numbers

## Related Documents

- [../project-plan.md](../project-plan.md): long-range plan and milestone intent
- [../project-status.md](../project-status.md): current implementation state
- [../roadmap.md](../roadmap.md): next-phase work
- [../../CHANGELOG.md](../../CHANGELOG.md): detailed release history
