# Eval Dataset Guidelines

Last updated: 2026-03-21

## Goal

AI workflow quality should not depend on intuition alone. This project should maintain small, practical evaluation datasets that make prompt or model changes easier to review.

This document defines a lightweight eval dataset approach appropriate for the current ticket-summary and ticket-triage slices plus future Week 6+ AI workflow features.

## Current Boundary

As of today:

- two public AI endpoints exist: `POST /api/v1/tickets/{id}/ai-summary` and `POST /api/v1/tickets/{id}/ai-triage`
- the repository now includes golden-sample fixtures at `merchantops-api/src/test/resources/ai/ticket-summary/golden-samples.json` and `merchantops-api/src/test/resources/ai/ticket-triage/golden-samples.json`
- the current public eval baseline is still small and intentionally reviewable by humans

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
- provider responses that previously produced unstable output

Purpose:

- verify that prompt changes do not reintroduce known bad behavior

### Policy And Safety Set

Use for boundary conditions.

Examples:

- requests that imply cross-tenant mixing
- requests that invite unsupported automation
- malformed or incomplete business context

Purpose:

- ensure the workflow still respects refusal, limitation, and human-approval boundaries

## Recommended Sample Shape

Each sample should stay simple and reviewable.

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

## Current Ticket Summary Expectations

For a ticket summary sample:

- must mention the core issue
- must describe the current ticket state
- must include the latest meaningful operator signal when present
- must include the next reasonable human follow-up or blocker
- must not invent cross-tenant or unsupported context
- must stay concise enough for operator review

## Dataset Size Guidance

Start small.

Suggested initial size per workflow:

- 5 to 10 golden samples
- 3 to 5 failure samples
- 2 to 3 policy or safety samples

This is enough to support meaningful review without creating heavy maintenance burden.

## Storage Guidance

Current live storage:

- ticket summary golden samples are stored under `merchantops-api/src/test/resources/ai/ticket-summary/`
- ticket triage golden samples are stored under `merchantops-api/src/test/resources/ai/ticket-triage/`

If broader AI automation appears later, these can expand into a structure like:

```text
docs/ai/evals/
  ticket-summary/
    golden.md
    failures.md
  import-error-summary/
    golden.md
    failures.md
```

## Review Process

When prompt or model changes are proposed:

1. run the workflow against the relevant small eval set
2. compare output against expected characteristics
3. note improvements, regressions, and unresolved tradeoffs
4. record whether the version should advance or stay unchanged

## Minimal Acceptance Rule

Before promoting a public AI workflow change:

- no obvious regressions should appear in the golden set
- known failure samples should not get worse without explicit acceptance
- safety-boundary samples should still respect tenant, permission, and approval constraints

## Related Documents

- [prompt-versioning.md](prompt-versioning.md): versioning rules for prompts and workflow variants
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): operational checklist for AI changes
- [../reference/ai-integration.md](../reference/ai-integration.md): current workflow guardrails and public boundary
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): audit and eval baseline decision
