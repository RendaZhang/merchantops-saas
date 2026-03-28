# Import Jobs

Last updated: 2026-03-28

## Public API Surface

Week 7 now exposes twelve import endpoints:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/import-jobs` | Bearer JWT | `USER_WRITE` | Accepts multipart request + CSV file, creates a `QUEUED` job, and schedules the MQ publish after transaction commit |
| `GET` | `/api/v1/import-jobs` | Bearer JWT | `USER_READ` | Pages current-tenant import jobs with optional queue filters |
| `GET` | `/api/v1/import-jobs/{id}` | Bearer JWT | `USER_READ` | Returns one current-tenant import job overview with `errorCodeCounts` plus backward-compatible `itemErrors` |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures` | Bearer JWT | `USER_WRITE` | Creates a new derived `QUEUED` job from the source job's replayable failed rows only |
| `POST` | `/api/v1/import-jobs/{id}/replay-file` | Bearer JWT | `USER_WRITE` | Creates a new derived `QUEUED` job by copying the source file for a full-failure `FAILED` source job |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures/selective` | Bearer JWT | `USER_WRITE` | Creates a new derived `QUEUED` job from the source job's replayable failed rows whose `errorCode` exactly matches one of the requested values |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures/edited` | Bearer JWT | `USER_WRITE` | Creates a new derived `QUEUED` job from caller-provided full replacement rows that target replayable failed-row `errorId` values only |
| `GET` | `/api/v1/import-jobs/{id}/errors` | Bearer JWT | `USER_READ` | Pages current-tenant failure items for one job with optional `errorCode` filter |
| `GET` | `/api/v1/import-jobs/{id}/ai-interactions` | Bearer JWT | `USER_READ` | Pages narrowed stored AI interaction history for one current-tenant import job |
| `POST` | `/api/v1/import-jobs/{id}/ai-error-summary` | Bearer JWT | `USER_READ` | Generates a read-only suggestion-only AI summary from current import detail, `errorCodeCounts`, and the first sanitized failed-row window |
| `POST` | `/api/v1/import-jobs/{id}/ai-mapping-suggestion` | Bearer JWT | `USER_READ` | Generates a read-only suggestion-only canonical-field mapping proposal from sanitized header/global signal plus bounded structural failure context |
| `POST` | `/api/v1/import-jobs/{id}/ai-fix-recommendation` | Bearer JWT | `USER_READ` | Generates a read-only suggestion-only fix recommendation from locally grounded row-level `errorCode` groups without returning replacement values |

## Current Week 5 Contract (`USER_CSV` + Reporting + Replay Surface)

- supported `sourceType`: `CSV`
- supported `importType`: `USER_CSV`
- status model: `QUEUED`, `PROCESSING`, `SUCCEEDED`, `FAILED`
- tenant scope always comes from JWT context
- row processing is now business-executing (user creation), not parse-only

`POST /api/v1/import-jobs` accepts multipart form data:

- `request` (JSON): `{ "importType": "USER_CSV" }`
- `file` (CSV): uploaded source file

Required `USER_CSV` header (fixed order):

```csv
username,displayName,email,password,roleCodes
```

`roleCodes` uses `|` as an in-cell delimiter, for example: `READ_ONLY|TENANT_ADMIN`.
CSV parsing follows standard quoted-record behavior: quote any field that contains commas, double quotes, or embedded newlines, and escape inner quotes as `""`.

Worker and runtime behavior (Week 5 runtime hardening):

1. create requests persist a `QUEUED` job and schedule the first MQ publish after transaction commit
2. if after-commit publish fails and the job remains `QUEUED`, the scheduled recovery loop republishes aged queued jobs in bounded batches; this is a best-effort recovery path, not a new public API
3. worker startup claims a fresh `QUEUED` job for processing
4. a fresh duplicate delivery for a still-active `PROCESSING` job is acknowledged and ignored so the in-flight worker keeps ownership; that duplicate message does not emit a duplicate processing-started audit
5. if a `PROCESSING` job stays stuck past the stale threshold, the scheduled recovery loop republishes it for recovery handling
6. a stale `PROCESSING` job with no recorded progress is restarted from `PROCESSING`; the recovery-started audit snapshot adds `recoveredFromStale=true`
7. a stale `PROCESSING` job that already has `totalCount`, `successCount`, or `failureCount` progress is failed in place with job summary `import job processing expired after partial progress` and audit error `PROCESSING_STALE`
8. the worker parses the file through the current CSV parser, strips a UTF-8 BOM on the first header cell when present, and validates fixed header + row shape
9. one worker still owns one job and processes the file in internal sequential chunks; chunking is not a public API concept and does not change the current status model
10. each row executes in its own transaction through the existing user-create service chain, so row-level partial success still holds across chunk boundaries
11. row errors are recorded in `import_job_item_error` for both parse-level and business-level failures
12. `totalCount`, `successCount`, and `failureCount` are flushed back to `import_job` after each chunk so `GET /api/v1/import-jobs/{id}` can show real progress during `PROCESSING`; handled-row progress and saved row errors are still persisted before an unexpected runtime failure writes the terminal job failure
13. files that exceed the configured row guardrail fail with `MAX_ROWS_EXCEEDED`
14. terminal transitions still write job-level audit events such as `IMPORT_JOB_COMPLETED` and `IMPORT_JOB_FAILED`

Current internal processing controls:

- one import job is still consumed by one worker
- default `merchantops.import.processing.chunk-size=100`
- default `merchantops.import.processing.max-rows-per-job=1000`
- default `merchantops.import.processing.stale-processing-threshold-seconds=300`
- default `merchantops.import.processing.enqueue-recovery-batch-size=100`
- default `merchantops.import.processing.enqueue-recovery-delay-ms=300000`
- default `merchantops.import.processing.enqueue-recovery-min-age-seconds=60`
- current implementation is intentionally sequential; parallel chunk workers, sub-jobs, and shard tables are still out of scope

Current list query surface:

- `page` and `size`
- exact `status`
- exact `importType`
- exact `requestedBy`
- `hasFailuresOnly=true` for queue triage across partial-success and failed jobs

Current error-page query surface:

- `page` and `size`
- exact `errorCode`

List filtering remains tenant-scoped and keeps stable ordering `createdAt DESC, id DESC`.
Failure-item ordering is stable for both detail and `/errors`: global/header errors without `rowNumber` first, then `rowNumber ASC, id ASC`.

Detail semantics for reporting:

- `GET /api/v1/import-jobs/{id}` is the overview read surface
- detail now includes `errorCodeCounts` for quick triage
- detail now includes nullable `sourceJobId`; original jobs keep `null`, replay-derived jobs point back to the source job
- detail still returns backward-compatible `itemErrors`
- detail counters now update during `PROCESSING` after each committed chunk
- `GET /api/v1/import-jobs/{id}/errors` is the paged failure-item read surface for larger jobs

## Read-Only Import AI Interaction History (`GET /api/v1/import-jobs/{id}/ai-interactions`)

This history slice stays intentionally narrow:

- request body is empty
- permission is `USER_READ`
- tenant scope and not-found behavior match the existing import read path, so cross-tenant or missing jobs still return `404`
- the endpoint is read-only; it does not trigger generation, replay, approval, or any import-job mutation
- the endpoint reuses the existing `ai_interaction_record` rows already written by import AI generation endpoints

Current query surface is minimal:

- `page`
- `size`
- exact `interactionType`
- exact `status`

Current supported canonical `interactionType` values are:

- `ERROR_SUMMARY`
- `MAPPING_SUGGESTION`
- `FIX_RECOMMENDATION`

Current stored `status` values include:

- `SUCCEEDED`
- `FEATURE_DISABLED`
- `PROVIDER_NOT_CONFIGURED`
- `PROVIDER_TIMEOUT`
- `PROVIDER_UNAVAILABLE`
- `INVALID_RESPONSE`

Current response shape is:

- `items`
- `page`
- `size`
- `total`
- `totalPages`

Each `items[]` record includes:

- `id`
- `interactionType`
- `status`
- `outputSummary`
- `promptVersion`
- `modelId`
- `latencyMs`
- `requestId`
- `usagePromptTokens`
- `usageCompletionTokens`
- `usageTotalTokens`
- `usageCostMicros`
- `createdAt`

Current history behavior is fixed:

- ordering is stable `createdAt DESC, id DESC`
- usage/cost fields are returned as `null` for failed or otherwise unmetered rows
- raw prompt text and raw provider payload are not exposed
- this remains an operator-visible runtime and governance surface, not a billing or ledger surface

## Read-Only Import AI Error Summary (`POST /api/v1/import-jobs/{id}/ai-error-summary`)

This error-summary slice stays intentionally narrow:

- request body is empty
- permission is `USER_READ`
- tenant scope and not-found behavior match the existing import read path
- the endpoint is read-only and suggestion-only; it does not mutate `import_job`, `import_job_item_error`, or replay state

Current prompt context is assembled from:

- current `GET /api/v1/import-jobs/{id}`-equivalent detail fields
- current `errorCodeCounts`
- the first 20 failure rows from the existing error-page query ordering

Current prompt-safety rules are hard-coded:

- raw `itemErrors.rawPayload` is never sent to the provider
- each failed row contributes only `rowNumber`, `errorCode`, `errorMessage`, and a structural-only summary such as `columnCount`, `usernamePresent`, `displayNamePresent`, `emailPresent`, `passwordPresent`, `roleCodesPresent`, and `roleCodeCount`
- raw username, display name, email, password, and role-code text are intentionally excluded
- if local CSV parsing fails, the prompt falls back to structural metadata only rather than forwarding raw row text

See [../architecture/import-ai-sanitized-context-strategy.md](../architecture/import-ai-sanitized-context-strategy.md) for the reusable architecture rule behind this Week 7 prompt boundary.

Current response shape is:

- `importJobId`
- `summary`
- `topErrorPatterns`
- `recommendedNextSteps`
- `promptVersion`
- `modelId`
- `generatedAt`
- `latencyMs`
- `requestId`

Current failure behavior:

- `403` when `USER_READ` is missing
- `404` for cross-tenant or missing import jobs
- controlled `503 SERVICE_UNAVAILABLE` when AI is disabled, not configured, unavailable, times out, or returns an invalid structured payload

## Read-Only Import AI Mapping Suggestion (`POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`)

Current Week 7 Slice B stays intentionally narrow:

- request body is empty
- permission is `USER_READ`
- tenant scope and not-found behavior match the existing import read path
- the endpoint is read-only and suggestion-only; it does not mutate `import_job`, `import_job_item_error`, source files, replay state, approvals, or business audit rows
- the endpoint currently supports `USER_CSV` jobs only

Current eligibility rules are explicit:

- the source job must already have failure signal, such as `failureCount > 0`, `errorCodeCounts`, or existing `itemErrors`
- the source job must expose at least one parseable sanitized header/global signal from existing `rowNumber=null` item errors
- jobs that have only business-row failures without parseable header/global signal return `400`
- clean jobs with no failure signal return `400`

Current prompt context is assembled from:

- current `GET /api/v1/import-jobs/{id}`-equivalent detail fields
- current `errorCodeCounts`
- one sanitized header/global parse signal made of normalized header names, 1-based positions, and header-column count
- current header/global parse-error code plus message
- the first 20 row-level failures from the existing error-page query ordering

Current prompt-safety rules are hard-coded:

- raw `itemErrors.rawPayload` is never sent to the provider
- the model only sees normalized header tokens such as `login` or `display_name`, not raw header lines or raw row values
- each failed row still contributes only `rowNumber`, `errorCode`, `errorMessage`, and the same structural-only summary used by the error-summary slice
- the mapping-suggestion slice does not rescan the source file, infer from hidden row values, generate fix values, or generate replay scripts

Current response shape is:

- `importJobId`
- `summary`
- `suggestedFieldMappings`
- `confidenceNotes`
- `recommendedOperatorChecks`
- `promptVersion`
- `modelId`
- `generatedAt`
- `latencyMs`
- `requestId`

Each `suggestedFieldMappings[]` record includes:

- `canonicalField`
- `observedColumnSignal`
- `reasoning`
- `reviewRequired`

Current failure behavior:

- `400` when the job has no failure signal
- `400` when the job has no sanitized header signal
- `403` when `USER_READ` is missing
- `404` for cross-tenant or missing import jobs
- controlled `503 SERVICE_UNAVAILABLE` when AI is disabled, not configured, unavailable, times out, or returns an invalid structured payload

## Read-Only Import AI Fix Recommendation (`POST /api/v1/import-jobs/{id}/ai-fix-recommendation`)

Current Week 7 Slice C also stays intentionally narrow:

- request body is empty
- permission is `USER_READ`
- tenant scope and not-found behavior match the existing import read path
- the endpoint is read-only and suggestion-only; it does not mutate `import_job`, `import_job_item_error`, source files, replay state, approvals, or business audit rows
- the endpoint currently supports `USER_CSV` jobs only

Current eligibility rules are explicit:

- the source job must already have failure signal, such as `failureCount > 0`, `errorCodeCounts`, or existing `itemErrors`
- the source job must be `USER_CSV`
- the source job must expose at least one row-level sanitized error group from existing `rowNumber != null` item errors
- jobs that only have header/global parse signal with no row-level sanitized group return `400`
- clean jobs with no failure signal return `400`

Current prompt context is assembled from:

- current `GET /api/v1/import-jobs/{id}`-equivalent detail fields
- current `errorCodeCounts`
- the first 20 row-level failures from the existing error-page query ordering
- local grounded row-level failure groups keyed by `errorCode`, each with `affectedRowsEstimate`, a bounded sample-row window, bounded sample error messages, and the same structural-only row summary used by the other import AI slices

Current prompt-safety and output-safety rules are hard-coded:

- raw `itemErrors.rawPayload` is never sent to the provider
- each failed row still contributes only `rowNumber`, `errorCode`, `errorMessage`, and structural-only summary fields such as `columnCount`, `usernamePresent`, `displayNamePresent`, `emailPresent`, `passwordPresent`, `roleCodesPresent`, and `roleCodeCount`
- the model never receives raw username, display name, email, password, or role-code text
- the model must return recommendations keyed by grounded local `errorCode` values only; ungrounded or duplicate codes are rejected as `INVALID_RESPONSE`
- the model does not return direct replacement values; any value edit still belongs to manual replay prep outside the AI response
- provider output is rejected if it echoes raw CSV-like strings or sensitive local row values such as username, display name, email, password, or role-code text

Current response shape is:

- `importJobId`
- `summary`
- `recommendedFixes`
- `confidenceNotes`
- `recommendedOperatorChecks`
- `promptVersion`
- `modelId`
- `generatedAt`
- `latencyMs`
- `requestId`

Each `recommendedFixes[]` record includes:

- `errorCode`
- `recommendedAction`
- `reasoning`
- `reviewRequired`
- `affectedRowsEstimate`

Current failure behavior:

- `400` when the job has no failure signal
- `400` when the job is not `USER_CSV`
- `400` when the job has no sanitized row-level signal
- `403` when `USER_READ` is missing
- `404` for cross-tenant or missing import jobs
- controlled `503 SERVICE_UNAVAILABLE` when AI is disabled, not configured, unavailable, times out, or returns an invalid structured payload

## Failed-Row Replay (`POST /api/v1/import-jobs/{id}/replay-failures`)

Current replay scope is intentionally narrow:

- replay creates a new derived `import_job`; it does not reset or mutate the old job
- replay copies replayable failed rows only from the source job into a new system-generated CSV file
- the replay job keeps `sourceJobId=<source job id>` and starts in `QUEUED`
- replay is currently supported for `USER_CSV` only
- replay jobs reuse the same worker path as any other standard `USER_CSV` import

Current rejection rules:

- source job is not found in the current tenant
- source job is not in a terminal status (`SUCCEEDED` or `FAILED`)
- source job has `failureCount = 0`
- source job is not `USER_CSV`
- source job has no replayable row-level failures, for example only header/global errors

Replay file generation rules:

- source rows come from `import_job_item_error.raw_payload`
- the replay builder reparses each failed row and prints a fresh CSV instead of string-concatenating raw payload
- the replay file always uses the fixed header `username,displayName,email,password,roleCodes`
- system-generated replay filenames currently follow `replay-failures-job-<sourceJobId>.csv`

Still out of scope in this slice:

- whole-file replay for source jobs that already succeeded any rows
- broader import types beyond `USER_CSV`
- automatic dedupe or idempotency ledger behavior beyond current business validation
- parallel chunk execution, sub-job / shard tables, and retry orchestration

## Whole-File Replay (`POST /api/v1/import-jobs/{id}/replay-file`)

Whole-file replay stays intentionally narrow:

- request body is empty
- whole-file replay still creates a new derived `import_job`; it does not reset or mutate the source job
- the source file is copied through the current storage abstraction into a new system-generated file
- the source job must be current-tenant `FAILED`, `USER_CSV`, and have `successCount = 0`
- the replay job keeps `sourceJobId=<source job id>` and starts in `QUEUED`
- the worker still consumes a standard `USER_CSV` file and does not need a whole-file-specific execution branch

Current rejection rules:

- source job is not found in the current tenant
- source job is not `FAILED`
- source job has `failureCount = 0`
- source job has any successful rows, including `FAILED` jobs such as `MAX_ROWS_EXCEEDED` after earlier successes
- source job is not `USER_CSV`

Current file-generation and audit behavior:

- the generated filename uses `replay-file-job-<sourceJobId>.csv`
- the copied replay file preserves the stored source file bytes instead of rebuilding rows from `import_job_item_error.raw_payload`
- source and replay audit snapshots add `replayMode=WHOLE_FILE`
- audit keeps lineage plus replay mode only; it does not persist file contents

## Selective Failed-Row Replay (`POST /api/v1/import-jobs/{id}/replay-failures/selective`)

Selective replay stays additive and intentionally narrow:

- request body shape is `{ "errorCodes": ["UNKNOWN_ROLE", "INVALID_EMAIL"] }`
- matching is by exact `errorCode` value; use `/errors` or detail `errorCodeCounts` to choose the current source-job codes
- selective replay still creates a new derived `import_job`; it does not reset or mutate the old job
- selective replay copies only replayable row-level failures whose `errorCode` matches one of the requested values
- the replay job keeps `sourceJobId=<source job id>` and starts in `QUEUED`
- selective replay is currently supported for terminal `USER_CSV` source jobs only
- the worker still consumes a standard generated `USER_CSV` file and does not need special selective-replay logic

Current rejection rules:

- request body is missing or `errorCodes` is empty
- `errorCodes` contains blank values
- source job is not found in the current tenant
- source job is not in a terminal status (`SUCCEEDED` or `FAILED`)
- source job has `failureCount = 0`
- source job is not `USER_CSV`
- none of the requested `errorCodes` resolve to replayable row-level failures in the source job

Current audit behavior:

- the source job still writes `IMPORT_JOB_REPLAY_REQUESTED`
- the replay job still writes `IMPORT_JOB_CREATED`
- selective replay adds `selectedErrorCodes` to both audit snapshots instead of adding a new import-job column in this slice

## Edited Failed-Row Replay (`POST /api/v1/import-jobs/{id}/replay-failures/edited`)

Edited replay keeps the same derived-job strategy while letting the caller replace the failed-row payload in full:

- request body shape is:
  - `{ "items": [{ "errorId": 701, "username": "...", "displayName": "...", "email": "...", "password": "...", "roleCodes": ["READ_ONLY"] }] }`
- each item is a full replacement row, not a sparse patch, so missing-field semantics stay unambiguous
- matching is by exact `errorId` from detail `itemErrors` or `GET /api/v1/import-jobs/{id}/errors`
- edited replay still creates a new derived `import_job`; it does not reset or mutate the source job
- edited replay keeps `sourceJobId=<source job id>` and starts in `QUEUED`
- edited replay is currently supported for terminal `USER_CSV` source jobs only
- the worker still consumes a standard generated `USER_CSV` file and does not need any edited-replay-specific branch

Current rejection rules:

- request body is missing or `items` is empty
- `items` contains duplicate `errorId` values
- any item is missing required fields or has empty `roleCodes`
- source job is not found in the current tenant
- source job is not in a terminal status (`SUCCEEDED` or `FAILED`)
- source job has `failureCount = 0`
- source job is not `USER_CSV`
- any requested `errorId` does not belong to the source job in the current tenant
- any requested `errorId` resolves to a header/global error instead of a replayable row-level error with raw payload

Current file-generation and audit behavior:

- the generated filename uses `replay-edited-job-<sourceJobId>.csv`
- the generated file still uses the fixed header `username,displayName,email,password,roleCodes`
- `roleCodes` are re-serialized as the standard `|`-delimited cell expected by the worker
- source and replay audit snapshots keep lineage plus edit scope only through `editedErrorIds`, `editedRowCount`, and `editedFields`
- replacement values such as `password`, `email`, or the full replacement row are intentionally not persisted in replay audit snapshots

Current error-code examples in `itemErrors`:

- parse/header/runtime: `EMPTY_FILE`, `INVALID_HEADER`, `INVALID_ROW_SHAPE`, `FILE_READ_ERROR`, `MAX_ROWS_EXCEEDED`, `UNSUPPORTED_IMPORT_TYPE`
- business-row: `DUPLICATE_USERNAME`, `UNKNOWN_ROLE`, `INVALID_EMAIL`, `INVALID_PASSWORD`, `MISSING_ROLE_CODES`

The list above is illustrative rather than exhaustive. A fallback code such as `BUSINESS_VALIDATION_FAILED` can still appear if the user-create path rejects a row for a business rule that has not yet been split into a narrower import-specific code.

## Example Request And Response

Example multipart request:

```http
POST /api/v1/import-jobs
Authorization: Bearer <admin-token>
Content-Type: multipart/form-data

request={"importType":"USER_CSV"}
file=@users.csv
```

Example `users.csv`:

```csv
username,displayName,email,password,roleCodes
alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
quoted,"Escaped ""Quote"", User",quoted@example.com,abc123,READ_ONLY
admin,Duplicate User,dup@example.com,abc123,READ_ONLY
beta,Beta User,beta@example.com,abc123,UNKNOWN_ROLE
gamma,Gamma User,gamma@example.com,abc123,READ_ONLY
```

Example detail response after partial success:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 1201,
    "tenantId": 1,
    "importType": "USER_CSV",
    "sourceType": "CSV",
    "sourceFilename": "users.csv",
    "storageKey": "1/550e8400-e29b-41d4-a716-446655440000-users.csv",
    "sourceJobId": null,
    "status": "SUCCEEDED",
    "requestedBy": 1,
    "requestId": "req-import-create-1201",
    "totalCount": 4,
    "successCount": 2,
    "failureCount": 2,
    "errorSummary": "completed with some row errors",
    "createdAt": "2026-03-12T18:20:00",
    "startedAt": "2026-03-12T18:20:02",
    "finishedAt": "2026-03-12T18:20:05",
    "errorCodeCounts": [
      {
        "errorCode": "DUPLICATE_USERNAME",
        "count": 1
      },
      {
        "errorCode": "UNKNOWN_ROLE",
        "count": 1
      }
    ],
    "itemErrors": [
      {
        "id": 31,
        "rowNumber": 3,
        "errorCode": "DUPLICATE_USERNAME",
        "errorMessage": "username already exists in current tenant",
        "rawPayload": "admin,Duplicate User,dup@example.com,abc123,READ_ONLY",
        "createdAt": "2026-03-12T18:20:04"
      },
      {
        "id": 32,
        "rowNumber": 4,
        "errorCode": "UNKNOWN_ROLE",
        "errorMessage": "roleCodes must exist in current tenant",
        "rawPayload": "beta,Beta User,beta@example.com,abc123,UNKNOWN_ROLE",
        "createdAt": "2026-03-12T18:20:04"
      }
    ]
  }
}
```

Example replay request:

```http
POST /api/v1/import-jobs/1201/replay-failures
Authorization: Bearer <admin-token>
```

Example import AI error-summary request:

```http
POST /api/v1/import-jobs/1201/ai-error-summary
Authorization: Bearer <admin-token>
```

Example import AI error-summary response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "importJobId": 1201,
    "summary": "The job is primarily blocked by tenant role validation failures, with a smaller duplicate-username tail. The sampled failed rows are structurally complete, so role-map cleanup should come before any replay attempt.",
    "topErrorPatterns": [
      "UNKNOWN_ROLE is the dominant error code in both the aggregate counts and the sampled failed rows.",
      "Most sampled failed rows still contain all expected `USER_CSV` columns, so the failures look data-quality related rather than parser-shape related."
    ],
    "recommendedNextSteps": [
      "Confirm the valid tenant role codes that should replace the invalid mappings before replay.",
      "Review duplicate usernames separately because those rows need edits rather than role-map cleanup."
    ],
    "promptVersion": "import-error-summary-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-27T10:25:15",
    "latencyMs": 512,
    "requestId": "import-ai-error-summary-req-1"
  }
}
```

Example import AI mapping-suggestion request:

```http
POST /api/v1/import-jobs/1202/ai-mapping-suggestion
Authorization: Bearer <admin-token>
```

Example import AI mapping-suggestion response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "importJobId": 1202,
    "summary": "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.",
    "suggestedFieldMappings": [
      {
        "canonicalField": "username",
        "observedColumnSignal": {
          "headerName": "login",
          "headerPosition": 1
        },
        "reasoning": "`login` is the closest observed header for the canonical username field.",
        "reviewRequired": false
      },
      {
        "canonicalField": "displayName",
        "observedColumnSignal": {
          "headerName": "display_name",
          "headerPosition": 2
        },
        "reasoning": "`display_name` is the closest semantic match for displayName.",
        "reviewRequired": false
      },
      {
        "canonicalField": "email",
        "observedColumnSignal": {
          "headerName": "email_address",
          "headerPosition": 3
        },
        "reasoning": "`email_address` is the most likely email column.",
        "reviewRequired": false
      },
      {
        "canonicalField": "password",
        "observedColumnSignal": {
          "headerName": "passwd",
          "headerPosition": 4
        },
        "reasoning": "`passwd` should be manually confirmed.",
        "reviewRequired": true
      },
      {
        "canonicalField": "roleCodes",
        "observedColumnSignal": {
          "headerName": "roles",
          "headerPosition": 5
        },
        "reasoning": "`roles` is the closest available signal for roleCodes.",
        "reviewRequired": true
      }
    ],
    "confidenceNotes": [
      "The source file failed header validation, so each suggested mapping should be reviewed before reuse."
    ],
    "recommendedOperatorChecks": [
      "Confirm the source header order before editing any replay input.",
      "Verify that the observed `roles` column really contains tenant role codes in the expected delimiter format."
    ],
    "promptVersion": "import-mapping-suggestion-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-27T10:30:15",
    "latencyMs": 544,
    "requestId": "import-ai-mapping-suggestion-req-1"
  }
}
```

Example import AI fix-recommendation request:

```http
POST /api/v1/import-jobs/1201/ai-fix-recommendation
Authorization: Bearer <admin-token>
```

Example import AI fix-recommendation response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "importJobId": 1201,
    "summary": "The import is mostly blocked by tenant role validation, with a smaller duplicate-username tail that should be handled as a separate cleanup step before replay.",
    "recommendedFixes": [
      {
        "errorCode": "UNKNOWN_ROLE",
        "recommendedAction": "Verify that the referenced role codes exist in the current tenant and normalize the source role-code format before preparing replay input.",
        "reasoning": "The grouped failures point to tenant role validation rather than CSV shape corruption.",
        "reviewRequired": true,
        "affectedRowsEstimate": 7
      },
      {
        "errorCode": "DUPLICATE_USERNAME",
        "recommendedAction": "Review the source usernames against current-tenant users and prepare unique replacements outside the AI response before replay.",
        "reasoning": "The grouped failures indicate a uniqueness conflict that needs an operator-reviewed edit.",
        "reviewRequired": true,
        "affectedRowsEstimate": 2
      }
    ],
    "confidenceNotes": [
      "The recommendations are grounded in row-level error groups, so operators should still confirm tenant-specific business rules before reuse."
    ],
    "recommendedOperatorChecks": [
      "Confirm which error-code group is the highest-volume cleanup target before editing replay input.",
      "Review the affected rows in /errors so value changes can be prepared outside the AI response."
    ],
    "promptVersion": "import-fix-recommendation-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-28T11:20:15",
    "latencyMs": 548,
    "requestId": "import-ai-fix-recommendation-req-1"
  }
}
```

Example selective replay request:

```http
POST /api/v1/import-jobs/1201/replay-failures/selective
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "errorCodes": ["UNKNOWN_ROLE"]
}
```

Example edited replay request:

```http
POST /api/v1/import-jobs/1201/replay-failures/edited
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "items": [
    {
      "errorId": 32,
      "username": "beta",
      "displayName": "Beta User",
      "email": "beta@example.com",
      "password": "abc123",
      "roleCodes": ["READ_ONLY"]
    }
  ]
}
```

Example replay-job response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 1202,
    "tenantId": 1,
    "importType": "USER_CSV",
    "sourceType": "CSV",
    "sourceFilename": "replay-failures-job-1201.csv",
    "storageKey": "1/550e8400-e29b-41d4-a716-446655440111-replay-failures-job-1201.csv",
    "sourceJobId": 1201,
    "status": "QUEUED",
    "requestedBy": 1,
    "requestId": "req-import-replay-1202",
    "totalCount": 0,
    "successCount": 0,
    "failureCount": 0,
    "errorSummary": null,
    "createdAt": "2026-03-12T19:00:00",
    "startedAt": null,
    "finishedAt": null,
    "errorCodeCounts": [],
    "itemErrors": []
  }
}
```

Example paged error response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 31,
        "rowNumber": 3,
        "errorCode": "DUPLICATE_USERNAME",
        "errorMessage": "username already exists in current tenant",
        "rawPayload": "admin,Duplicate User,dup@example.com,abc123,READ_ONLY",
        "createdAt": "2026-03-12T18:20:04"
      }
    ],
    "page": 0,
    "size": 1,
    "total": 2,
    "totalPages": 2
  }
}
```

Example list response item:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 1201,
        "importType": "USER_CSV",
        "sourceType": "CSV",
        "sourceFilename": "users.csv",
        "status": "SUCCEEDED",
        "requestedBy": 1,
        "hasFailures": true,
        "totalCount": 4,
        "successCount": 2,
        "failureCount": 2,
        "errorSummary": "completed with some row errors",
        "createdAt": "2026-03-12T18:20:00",
        "startedAt": "2026-03-12T18:20:02",
        "finishedAt": "2026-03-12T18:20:05"
      }
    ],
    "page": 0,
    "size": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

## Job Counter Semantics

Current semantics:

- `totalCount`: total data rows read, including the over-limit row when `MAX_ROWS_EXCEEDED` terminates the job
- `successCount`: rows that really created tenant users
- `failureCount`: rows that failed parse validation or business execution
- `errorCodeCounts`: aggregated counts by `errorCode` for quick triage
- `sourceJobId`: `null` for original jobs, populated only for replay-derived jobs
- clean success: `status=SUCCEEDED`, `failureCount=0`, `errorSummary=null`
- partial success: `status=SUCCEEDED`, `failureCount>0`, `errorSummary="completed with some row errors"`
- full failure: `status=FAILED`
- throughput-guardrail failure: `status=FAILED`, `errorSummary="import job exceeded max row limit"`, and a queryable `MAX_ROWS_EXCEEDED` error row

## Governance Behavior

- job-level audit events remain `IMPORT_JOB_*`.
- a fresh duplicate delivery for an already-active `PROCESSING` job is acknowledged and ignored; recovery republishes only after the stale threshold is crossed.
- a stale zero-progress restart records `recoveredFromStale=true` on `IMPORT_JOB_PROCESSING_STARTED`.
- a stale in-progress failure currently surfaces through job summary plus audit error `PROCESSING_STALE`; it does not create a replayable row-level `itemError`.
- replay writes `IMPORT_JOB_REPLAY_REQUESTED` on the source job and keeps `IMPORT_JOB_CREATED` on the new replay job with `sourceJobId` in the created snapshot.
- selective replay keeps the same event types and additionally records `selectedErrorCodes` in both source and replay audit snapshots.
- edited replay keeps the same event types and additionally records `editedErrorIds`, `editedRowCount`, and `editedFields` in both source and replay audit snapshots without persisting replacement values.
- each successful user creation still emits existing `USER_CREATED` audit events through `UserCommandService`.
- created users still persist tenant/operator attribution (`tenantId`, `createdBy`, `updatedBy`) from the import request context.

## Notes

- this slice is intentionally narrow: only `USER_CSV` business-row execution plus failed-row replay, exact error-code selective replay, and edited failed-row replay are implemented.
- the current AI guidance slices are intentionally narrower still: only read-only import interaction history, error summary, mapping suggestion, and fix recommendation are public; any write-back flow remains out of scope.
- quoted CSV fields are supported by the current parser, so commas inside quoted values do not force an `INVALID_ROW_SHAPE` by themselves.
- unsupported import types currently fail before row processing begins and are recorded as terminal job failures.
- local file storage is intentionally replaceable for later object-storage rollout.
- replay uses the same storage abstraction as uploaded files so system-generated replay CSVs and user uploads follow one storage path.
- chunking is an internal execution boundary only; the public API still exposes one job, one status, and one set of counters.
- list paging now supports `page`, `size`, `status`, `importType`, `requestedBy`, and `hasFailuresOnly`; default size is `10` and max size is `100`.
- error paging now supports `page`, `size`, and `errorCode`; default size is `10` and max size is `100`.
- list items expose `requestedBy` and derived `hasFailures`; detail exposes `sourceJobId`, `errorCodeCounts`, and backward-compatible `itemErrors`; `/errors` pages the same failure rows for larger jobs.
- `itemErrors.rawPayload` remains visible on the import detail and `/errors` read surfaces for replay/reporting, but `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` do not forward those raw values to the provider.
- current list ordering remains `createdAt DESC, id DESC`.
- current failure-item ordering remains stable: null `rowNumber` first, then `rowNumber ASC, id ASC`.
- see [configuration.md](configuration.md) for the current import chunking, stale-processing, and queue-recovery controls.
- see [../../api-demo.http](../../api-demo.http) for runnable request examples.
