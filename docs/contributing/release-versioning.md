# Release Versioning

Last updated: 2026-03-12

## Purpose

This page defines how MerchantOps SaaS should record versions, tags, and release milestones as it moves from portfolio build to open-source release.

## Current Tagged Milestone

- current tag: `v0.1.3`
- tag date: 2026-03-12
- tag message: `Week 4 complete: audit and approval baseline`

This tag records the first reusable governance baseline while keeping the project inside the early `v0.1.x` line before the broader Week 5 preview story.

## Previous Tagged Milestone

- previous tag: `v0.1.2`
- tag date: 2026-03-11
- tag message: `Week 3 complete: ticket workflow baseline`

This tag records the first completed workflow module before Week 4 broadened the project into reusable governance patterns.

## Earlier Tagged Milestone

- earlier tag: `v0.1.1`
- tag date: 2026-03-11
- tag message: `Week 2 complete: tenant user management loop`

This tag records the first tenant-scoped business loop before Week 3 broadened the project story.

## Initial Tagged Baseline

- initial tag: `v0.1.0`
- tag date: 2026-03-09
- tag message: `Week 1 complete: foundation phase`

This tag remains the explicit Week 1 baseline for later version planning.

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
- `v0.1.1`: Week 2 tenant user management loop complete milestone
- `v0.1.2`: Week 3 ticket workflow baseline complete milestone
- `v0.1.3`: Week 4 audit and approval baseline complete milestone
- `v0.2.0-alpha`: first open-source preview after Week 5 workflow and async-operation backbone
- `v0.3.0-beta`: first public AI-enhanced release after Week 6 or Week 7
- later `v0.x` releases: governance, usage tracking, hardening, and packaging milestones

## Practical Rules

- create a tag only when the repository state is intentionally presentable
- every new tag should have a matching changelog note
- while the project is still stabilizing inside the early `v0.1.x` line, a patch bump may still represent a real milestone if the scope is explicitly documented
- use the next minor version when the project opens a broader release line, such as the first open-source preview or another clearly wider public story
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
