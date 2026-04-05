# Prompt Versioning

Last updated: 2026-04-05

## Goal

Prompt changes should be reviewable and traceable in the same way code changes are reviewable and traceable.

This document defines the lightweight prompt-versioning approach used by the current ticket and import AI generation slices and expected for future AI workflow features.

## Current Boundary

As of today:

- eight public AI endpoints exist: two history read endpoints plus six generation endpoints across ticket and import workflows
- the current generation prompt versions are `ticket-summary-v1`, `ticket-triage-v1`, `ticket-reply-draft-v1`, `import-error-summary-v1`, `import-mapping-suggestion-v1`, and `import-fix-recommendation-v1`
- public generation responses return `promptVersion`, and the history endpoints reuse the stored prompt version from previous AI invocations
- `ai_interaction_record` persists the prompt version for each AI invocation
- the active six-workflow prompt inventory is now executable in main code through `merchantops-api/src/main/java/com/renda/merchantops/api/ai/core/AiGenerationWorkflow.java`
- the test-side governance baseline lives alongside that runtime inventory in `merchantops-api/src/test/java/com/renda/merchantops/api/ai/eval/AiWorkflowEvalInventory.java`, but it is maintained independently so prompt-version bumps fail the eval baseline until the governance inventory is intentionally updated
- prompt text still lives in code-side prompt builders rather than a separate prompt registry

## Active Workflow Inventory

The current live prompt inventory is explicit and executable rather than only described in prose:

| Workflow | Interaction Type | Active Prompt Version | Golden Samples | Failure Samples | Policy Samples |
| --- | --- | --- | --- | --- | --- |
| `ticket-summary` | `SUMMARY` | `ticket-summary-v1` | `merchantops-api/src/test/resources/ai/ticket-summary/golden-samples.json` | `merchantops-api/src/test/resources/ai/ticket-summary/failure-samples.json` | `merchantops-api/src/test/resources/ai/ticket-summary/policy-samples.json` |
| `ticket-triage` | `TRIAGE` | `ticket-triage-v1` | `merchantops-api/src/test/resources/ai/ticket-triage/golden-samples.json` | `merchantops-api/src/test/resources/ai/ticket-triage/failure-samples.json` | `merchantops-api/src/test/resources/ai/ticket-triage/policy-samples.json` |
| `ticket-reply-draft` | `REPLY_DRAFT` | `ticket-reply-draft-v1` | `merchantops-api/src/test/resources/ai/ticket-reply-draft/golden-samples.json` | `merchantops-api/src/test/resources/ai/ticket-reply-draft/failure-samples.json` | `merchantops-api/src/test/resources/ai/ticket-reply-draft/policy-samples.json` |
| `import-job-error-summary` | `ERROR_SUMMARY` | `import-error-summary-v1` | `merchantops-api/src/test/resources/ai/import-job-error-summary/golden-samples.json` | `merchantops-api/src/test/resources/ai/import-job-error-summary/failure-samples.json` | `merchantops-api/src/test/resources/ai/import-job-error-summary/policy-samples.json` |
| `import-job-mapping-suggestion` | `MAPPING_SUGGESTION` | `import-mapping-suggestion-v1` | `merchantops-api/src/test/resources/ai/import-job-mapping-suggestion/golden-samples.json` | `merchantops-api/src/test/resources/ai/import-job-mapping-suggestion/failure-samples.json` | `merchantops-api/src/test/resources/ai/import-job-mapping-suggestion/policy-samples.json` |
| `import-job-fix-recommendation` | `FIX_RECOMMENDATION` | `import-fix-recommendation-v1` | `merchantops-api/src/test/resources/ai/import-job-fix-recommendation/golden-samples.json` | `merchantops-api/src/test/resources/ai/import-job-fix-recommendation/failure-samples.json` | `merchantops-api/src/test/resources/ai/import-job-fix-recommendation/policy-samples.json` |

The default regression now checks that inventory through `merchantops-api/src/test/java/com/renda/merchantops/api/ai/eval/AiWorkflowEvalComparatorTest.java`.

When a workflow prompt version advances, update both `AiGenerationWorkflow` and `AiWorkflowEvalInventory` intentionally in the same change. The runtime catalog defines what the application serves; the eval inventory defines the reviewed governance baseline that must still pass.

## What Counts As A Versioned AI Artifact

At minimum, version the following:

- system prompt text
- workflow-specific instruction text
- output format contract
- safety or refusal instructions
- major few-shot examples if they materially change behavior
- context-assembly rules when they materially change the AI-visible input

If a change can alter output quality, output shape, approval behavior, or tool usage, treat it as versioned.

## Recommended Version Shape

Use a simple, explicit identifier rather than implicit timestamps.

Suggested format:

- `ticket-summary-v1`
- `ticket-summary-v2`
- `ticket-triage-v1`
- `ticket-reply-draft-v1`
- `import-error-summary-v1`
- `import-mapping-suggestion-v1`
- `import-fix-recommendation-v1`

Keep version ids stable and human-readable. Avoid storing `latest` as a production reference.

## Current Storage Guidance

Today the live prompt version is carried by configuration and code together:

- `merchantops.ai.prompt-version` stores the public and persisted summary version identifier
- `merchantops.ai.triage-prompt-version` stores the public and persisted triage version identifier
- `merchantops.ai.reply-draft-prompt-version` stores the public and persisted reply-draft version identifier
- `merchantops.ai.import-error-summary-prompt-version` stores the public and persisted import error-summary version identifier
- `merchantops.ai.import-mapping-suggestion-prompt-version` stores the public and persisted import mapping-suggestion version identifier
- `merchantops.ai.import-fix-recommendation-prompt-version` stores the public and persisted import fix-recommendation version identifier
- the current prompt text lives in `TicketSummaryPromptBuilder`, `TicketTriagePromptBuilder`, `TicketReplyDraftPromptBuilder`, `ImportJobErrorSummaryPromptBuilder`, `ImportJobMappingSuggestionPromptBuilder`, and `ImportJobFixRecommendationPromptBuilder`
- the default version fallback, stable workflow key, entity type, and interaction type now live together in `AiGenerationWorkflow`

That is acceptable for the current baseline because the public contract and stored audit record already expose an explicit version id.

If prompt files move into resources later, prefer a structure that stays easy to diff, for example:

```text
merchantops-api/
  src/main/resources/ai/
    ticket-summary/
      v1.md
      v2.md
    import-error-summary/
      v1.md
```

## Logging Requirements

When AI execution runs, logs and persistence should record:

- prompt version id
- model id
- tenantId
- userId
- requestId
- workflow name or interaction type
- approval outcome if a future slice adds write-back behavior

Without this, prompt behavior cannot be tied back to production outcomes.

## Review Expectations

Every prompt change should answer:

- what behavior is intended to improve
- what risk could regress
- which golden, failure, or policy samples were checked
- whether the public output shape changed
- whether downstream approval or tool logic changed

## Minimal Release Rule

Do not promote a new prompt version for public workflow use unless:

- the version id is explicit
- sample regressions were reviewed
- the output contract still matches the API expectation
- AI audit logging still captures the version id

## Example Change Record

```text
Feature: ticket-summary
Version: ticket-summary-v2
Reason: reduce overlong summaries and make next actions clearer
Checked against: 5 golden samples, 3 failure samples
Output contract changed: no
Approval flow changed: no
```

## Related Documents

- [eval-dataset-guidelines.md](eval-dataset-guidelines.md): how to build supporting eval sets
- [../reference/ai-integration.md](../reference/ai-integration.md): current AI workflow guardrails and public contract
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): regression expectations after prompt changes
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): AI audit and eval baseline decision
