# Eval Dataset Guidelines

Last updated: 2026-03-10

## Goal

AI workflow quality should not depend on intuition alone. This project should maintain small, practical evaluation datasets that make prompt or model changes easier to review.

This document defines a lightweight eval dataset approach appropriate for a portfolio project and future Week 6+ AI workflow features.

## Current Boundary

As of today:

- no public AI endpoints exist yet
- no eval dataset files exist in the repository yet
- this document describes how to prepare eval assets before or alongside the first AI workflow rollout

## Dataset Principles

- keep datasets small enough to review manually
- prefer realistic workflow samples over synthetic trivia
- include both good-path and failure-path cases
- tie each dataset to a specific workflow, not “AI in general”

## Recommended Dataset Types

### Golden Set

Use for representative, expected good-path behavior.

Examples:

- normal ticket summary cases
- common ticket triage cases
- common import error summary cases

Purpose:

- detect regressions in normal business output quality

### Failure Set

Use for known hard or risky cases.

Examples:

- ambiguous ticket descriptions
- mixed-language content
- noisy import rows
- edge cases that previously produced unsafe or unhelpful output

Purpose:

- verify that prompt changes do not reintroduce known bad behavior

### Policy and Safety Set

Use for boundary conditions.

Examples:

- requests that imply cross-tenant mixing
- requests that invite unsupported automation
- malformed or incomplete business context

Purpose:

- ensure the workflow still respects refusal, limitation, and human-approval boundaries

## Recommended Sample Shape

Each sample should be simple and reviewable.

Suggested fields:

- sample id
- workflow name
- input context
- expected output characteristics
- unacceptable output characteristics
- reviewer notes

The expected result does not always need to be one exact answer. For workflow AI, it is often better to define:

- what must be present
- what must not happen
- what quality bar is acceptable

## Example Expectations

For a ticket summary sample:

- must mention the core issue
- must include next action or unresolved point
- must not invent tenant data
- must stay within a reasonable length

For an import error summary sample:

- must identify the dominant error pattern
- must not claim certainty where data is ambiguous
- must avoid pretending to repair source data automatically

## Dataset Size Guidance

Start small.

Suggested initial size per workflow:

- 5 to 10 golden samples
- 3 to 5 failure samples
- 2 to 3 policy or safety samples

This is enough to support meaningful review without creating heavy maintenance burden.

## Storage Guidance

Once AI workflow implementation begins, prefer a structure like:

```text
docs/ai/evals/
  ticket-summary/
    golden.md
    failures.md
  import-error-summary/
    golden.md
    failures.md
```

If code-side automation appears later, these can evolve into JSON, YAML, or test fixtures.

## Review Process

When prompt or model changes are proposed:

1. run the workflow against the relevant small eval set
2. compare output against expected characteristics
3. note improvements, regressions, and unresolved tradeoffs
4. record whether the version should advance or stay unchanged

## What to Avoid

- giant benchmark sets that nobody reviews
- fully synthetic samples with no workflow realism
- one-score evaluation without reviewer notes
- datasets that mix unrelated workflows together

## Minimal Acceptance Rule

Before promoting a public AI workflow change:

- no obvious regressions should appear in the golden set
- known failure samples should not get worse without explicit acceptance
- safety-boundary samples should still respect tenant, permission, and approval constraints

## Related Documents

- [prompt-versioning.md](prompt-versioning.md): versioning rules for prompts and workflow variants
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): operational checklist for AI changes
- [../reference/ai-integration.md](../reference/ai-integration.md): workflow guardrails and public boundary
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): audit and eval baseline decision
