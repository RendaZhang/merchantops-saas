# Prompt Versioning

Last updated: 2026-03-21

## Goal

Prompt changes should be reviewable and traceable in the same way code changes are reviewable and traceable.

This document defines the lightweight prompt-versioning approach used by the current ticket-summary, ticket-triage, and ticket-reply-draft AI slices and expected for future AI workflow features.

## Current Boundary

As of today:

- three public AI endpoints exist: `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft`
- the current prompt versions are `ticket-summary-v1`, `ticket-triage-v1`, and `ticket-reply-draft-v1`
- the public response returns `promptVersion`
- `ai_interaction_record` persists the prompt version for each AI invocation
- prompt text still lives in code-side prompt builders rather than a separate prompt registry

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

Keep version ids stable and human-readable. Avoid storing `latest` as a production reference.

## Current Storage Guidance

Today the live prompt version is carried by configuration and code together:

- `merchantops.ai.prompt-version` stores the public and persisted summary version identifier
- `merchantops.ai.triage-prompt-version` stores the public and persisted triage version identifier
- `merchantops.ai.reply-draft-prompt-version` stores the public and persisted reply-draft version identifier
- the current prompt text lives in `TicketSummaryPromptBuilder`, `TicketTriagePromptBuilder`, and `TicketReplyDraftPromptBuilder`

That is acceptable for the first slice because the public contract and stored audit record already expose an explicit version id.

If prompt files move into resources later, prefer a structure that stays easy to diff, for example:

```text
merchantops-api/
  src/main/resources/ai/
    ticket-summary/
      v1.md
      v2.md
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
- which golden or failure samples were checked
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
