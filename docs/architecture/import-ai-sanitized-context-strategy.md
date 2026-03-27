# Import AI Sanitized Context Strategy

Last updated: 2026-03-27

## Purpose

This note records the context-sanitization boundary for the current Week 7 import AI path now that the first import AI error-summary slice is being introduced.

## Current Decision

For the current Week 7 import AI slices:

- import AI should build prompts from sanitized import-job context, not from raw failed-row payloads
- `import_job_item_error.raw_payload` should never be forwarded to an AI provider as-is
- the first import AI slice should use structural-only failed-row summaries plus import-job detail and `errorCodeCounts`
- later import AI slices such as mapping suggestion or fix recommendation should keep the same sanitized-context rule unless an explicit narrower exception is designed

## Why

- the current `USER_CSV` schema includes sensitive fields such as `password`
- failed-row raw payloads may also contain direct identifiers or business data that do not need to leave the service boundary for the current Week 7 guidance use cases
- the current Week 7 value comes from error pattern explanation and operator guidance, not from replaying or echoing full source rows back through the model
- a sanitized-context rule keeps the import AI surface aligned with the existing workflow-first and governance-first AI direction

## Implementation Boundary

- import AI prompt builders should start from current-tenant import-job detail, `errorCodeCounts`, and a bounded window of failed rows from the existing error-page ordering
- each failed row should contribute structural metadata only, such as `rowNumber`, `errorCode`, `errorMessage`, `columnCount`, presence flags, and bounded count-style summaries
- raw username, display name, email, password, and role-code text should stay out of the provider prompt in the current Week 7 path
- if local CSV parsing of a failed row is not possible, the fallback should be even narrower structural metadata rather than forwarding raw row text
- import AI output, AI interaction records, and related audit should keep runtime metadata and narrow summaries only; they should not persist sanitized-context expansions as a backdoor raw-data log
- when new import types are added later, prompt-time field exposure should default to deny and be opened by an explicit per-import-type allowlist rather than by generic pass-through

## Deferred On Purpose

The current Week 7 import AI path should not require:

- generic raw-row redaction pipelines that still preserve most original values
- provider prompts that include full failed-row payload text
- AI suggestions that echo sensitive replacement values back into logs or history records
- tenant-configurable masking policies before the first import AI workflow is proven

Those can be designed later if import AI becomes a broader governed product surface.

## Future Direction

If import AI expands beyond the first error-summary slice:

- mapping-suggestion and fix-recommendation flows should continue using the same sanitized-context baseline
- repeated prompt-sanitization logic should move into a reusable import AI context builder instead of being reimplemented per endpoint
- if multiple import types need materially different exposure rules, promote the allowlist policy into a more explicit architecture rule or ADR
