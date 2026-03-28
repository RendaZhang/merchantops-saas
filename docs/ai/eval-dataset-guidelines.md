# Eval Dataset Guidelines

Last updated: 2026-03-28

## Goal

AI workflow quality should not depend on intuition alone. This project should maintain small, practical evaluation datasets that make prompt or model changes easier to review.

This document defines a lightweight eval dataset approach appropriate for the current ticket and import AI generation slices plus future workflow candidates.

## Current Boundary

As of today:

- the public AI surface now includes two history read endpoints plus six generation endpoints across ticket and import workflows
- the repository now includes golden-sample fixtures at:
  - `merchantops-api/src/test/resources/ai/ticket-summary/golden-samples.json`
  - `merchantops-api/src/test/resources/ai/ticket-triage/golden-samples.json`
  - `merchantops-api/src/test/resources/ai/ticket-reply-draft/golden-samples.json`
  - `merchantops-api/src/test/resources/ai/import-job-error-summary/golden-samples.json`
  - `merchantops-api/src/test/resources/ai/import-job-mapping-suggestion/golden-samples.json`
  - `merchantops-api/src/test/resources/ai/import-job-fix-recommendation/golden-samples.json`
- the current public eval baseline is still small and intentionally reviewable by humans
- the history endpoints depend on stored interaction records and query behavior; prompt-quality eval sets are primarily needed for the generation paths

## Dataset Principles

- keep datasets small enough to review manually
- prefer realistic workflow samples over synthetic trivia
- include both good-path and failure-path cases
- tie each dataset to a specific workflow, not `AI in general`

## Recommended Dataset Types

### Golden Set

Use for representative, expected good-path behavior.

Examples:

- normal ticket summary cases
- common ticket triage cases
- common internal ticket reply-draft cases
- common import error summary cases
- common import mapping suggestion cases
- common import fix recommendation cases

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

## Current Workflow Expectations Example

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
- ticket reply-draft golden samples are stored under `merchantops-api/src/test/resources/ai/ticket-reply-draft/`
- import error-summary golden samples are stored under `merchantops-api/src/test/resources/ai/import-job-error-summary/`
- import mapping-suggestion golden samples are stored under `merchantops-api/src/test/resources/ai/import-job-mapping-suggestion/`
- import fix-recommendation golden samples are stored under `merchantops-api/src/test/resources/ai/import-job-fix-recommendation/`

If broader AI automation appears later, these can expand into a structure like:

```text
docs/ai/evals/
  ticket-summary/
    golden.md
    failures.md
  import-error-summary/
    golden.md
    failures.md
  import-mapping-suggestion/
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
