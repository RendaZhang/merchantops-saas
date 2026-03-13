# Prompt Versioning

Last updated: 2026-03-10

## Goal

Prompt changes should be reviewable and traceable in the same way code changes are reviewable and traceable.

This document defines a lightweight prompt-versioning approach for future AI workflow features in this project.

## Current Boundary

As of today:

- no public AI endpoints exist yet
- no prompt registry is implemented in code yet
- this document defines the baseline to use before Week 6+ AI features become public API

## What Counts as a Versioned AI Artifact

At minimum, version the following:

- system prompt text
- developer or workflow instruction text
- output format contract
- tool-availability assumptions
- safety or refusal instructions
- major few-shot examples if they materially change behavior

If a change can alter output quality, output shape, approval behavior, or tool usage, treat it as versioned.

## Recommended Version Shape

Use a simple, explicit identifier rather than implicit timestamps.

Suggested format:

- `ticket-summary.v1`
- `ticket-summary.v2`
- `ticket-triage.v1`
- `import-error-summary.v1`

Keep version ids stable and human-readable. Avoid “latest” as a stored production reference.

## Recommended Metadata

For each prompt or workflow version, keep:

- version id
- feature name
- intended business workflow
- model target or tested model family
- owner
- created date
- change summary
- linked eval set or regression sample reference
- known limitations

## Change Types

### Patch-Level Intent

Small edits that should preserve workflow intent:

- wording cleanup
- formatting clarification
- minor refusal improvement

Even these should still trigger a lightweight regression pass.

### Minor Behavior Change

Changes that shift output tendencies but not the entire task:

- stronger prioritization guidance
- different draft tone
- updated output schema wording
- new examples

These should get a new version id.

### Major Workflow Change

Changes that alter task boundaries or tool behavior:

- moving from summary-only to tool-using draft generation
- changing approval expectations
- adding new business context fields

These should get a new version id and an updated design note if the workflow contract changes.

## Storage Guidance

Before code implementation exists, versioning can live in docs and PR descriptions.

Once AI code exists, prefer a structure that is easy to diff, for example:

```text
merchantops-api/
  src/main/resources/ai/
    ticket-summary/
      v1.md
      v2.md
    import-error-summary/
      v1.md
```

Pair each versioned prompt with a small metadata file or code-side descriptor if needed.

## Logging Requirements

When AI execution is implemented, logs should record:

- prompt version id
- model id
- tenantId
- userId
- requestId
- workflow name
- approval outcome if applicable

Without this, prompt behavior cannot be tied back to production outcomes.

## Review Expectations

Every prompt change should answer:

- what behavior is intended to improve
- what risk could regress
- which eval samples were checked
- whether output shape changed
- whether downstream approval or tool logic changed

## Minimal Release Rule

Do not promote a new prompt version for public workflow use unless:

- the version id is explicit
- sample regressions were reviewed
- the output contract still matches the API expectation
- AI audit logging can capture the version id

## Example Change Record

```text
Feature: ticket-summary
Version: v2
Reason: reduce overlong summaries and make action items clearer
Checked against: 5 golden samples, 3 failure samples
Output contract changed: no
Approval flow changed: no
```

## Related Documents

- [eval-dataset-guidelines.md](eval-dataset-guidelines.md): how to build supporting eval sets
- [../reference/ai-integration.md](../reference/ai-integration.md): AI workflow guardrails
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): regression expectations after prompt changes
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): AI audit and eval baseline decision
