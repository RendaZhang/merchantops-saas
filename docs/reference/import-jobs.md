# Import Jobs

Last updated: 2026-03-12

## Public API Surface

Week 5 now exposes six async import endpoints:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/import-jobs` | Bearer JWT | `USER_WRITE` | Accepts multipart request + CSV file, creates a `QUEUED` job, and schedules the MQ publish after transaction commit |
| `GET` | `/api/v1/import-jobs` | Bearer JWT | `USER_READ` | Pages current-tenant import jobs with optional queue filters |
| `GET` | `/api/v1/import-jobs/{id}` | Bearer JWT | `USER_READ` | Returns one current-tenant import job overview with `errorCodeCounts` plus backward-compatible `itemErrors` |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures` | Bearer JWT | `USER_WRITE` | Creates a new derived `QUEUED` job from the source job's replayable failed rows only |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures/selective` | Bearer JWT | `USER_WRITE` | Creates a new derived `QUEUED` job from the source job's replayable failed rows whose `errorCode` exactly matches one of the requested values |
| `GET` | `/api/v1/import-jobs/{id}/errors` | Bearer JWT | `USER_READ` | Pages current-tenant failure items for one job with optional `errorCode` filter |

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

Worker behavior (Week 5 Slice E runtime):

1. consumes `jobId` and transitions `QUEUED -> PROCESSING`
2. parses the file through the current CSV parser, strips a UTF-8 BOM on the first header cell when present, and validates fixed header + row shape
3. keeps one worker per job and processes the file in internal sequential chunks; chunking is not a public API concept and does not change the current status model
4. executes each row in one independent transaction via the existing user-create service chain, so row-level partial success still holds across chunk boundaries
5. records row errors in `import_job_item_error` for both parse-level and business-level failures
6. flushes `totalCount`, `successCount`, and `failureCount` back to `import_job` after each chunk so `GET /api/v1/import-jobs/{id}` can show real progress during `PROCESSING`
7. fails files that exceed the configured row guardrail with `MAX_ROWS_EXCEEDED`
8. writes terminal status + audit events (`IMPORT_JOB_COMPLETED` / `IMPORT_JOB_FAILED`)

Current internal processing controls:

- one import job is still consumed by one worker
- default `merchantops.import.processing.chunk-size=100`
- default `merchantops.import.processing.max-rows-per-job=1000`
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

- replaying the entire original file
- editing failed rows before replay
- broader import types beyond `USER_CSV`
- automatic dedupe or idempotency ledger behavior beyond current business validation
- parallel chunk execution, sub-job / shard tables, and retry orchestration

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

Example selective replay request:

```http
POST /api/v1/import-jobs/1201/replay-failures/selective
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "errorCodes": ["UNKNOWN_ROLE"]
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
- replay writes `IMPORT_JOB_REPLAY_REQUESTED` on the source job and keeps `IMPORT_JOB_CREATED` on the new replay job with `sourceJobId` in the created snapshot.
- selective replay keeps the same event types and additionally records `selectedErrorCodes` in both source and replay audit snapshots.
- each successful user creation still emits existing `USER_CREATED` audit events through `UserCommandService`.
- created users still persist tenant/operator attribution (`tenantId`, `createdBy`, `updatedBy`) from the import request context.

## Notes

- this slice is intentionally narrow: only `USER_CSV` business-row execution plus failed-row replay and exact error-code selective replay are implemented.
- quoted CSV fields are supported by the current parser, so commas inside quoted values do not force an `INVALID_ROW_SHAPE` by themselves.
- unsupported import types currently fail before row processing begins and are recorded as terminal job failures.
- local file storage is intentionally replaceable for later object-storage rollout.
- replay uses the same storage abstraction as uploaded files so system-generated replay CSVs and user uploads follow one storage path.
- chunking is an internal execution boundary only; the public API still exposes one job, one status, and one set of counters.
- list paging now supports `page`, `size`, `status`, `importType`, `requestedBy`, and `hasFailuresOnly`; default size is `10` and max size is `100`.
- error paging now supports `page`, `size`, and `errorCode`; default size is `10` and max size is `100`.
- list items expose `requestedBy` and derived `hasFailures`; detail exposes `sourceJobId`, `errorCodeCounts`, and backward-compatible `itemErrors`; `/errors` pages the same failure rows for larger jobs.
- current list ordering remains `createdAt DESC, id DESC`.
- current failure-item ordering remains stable: null `rowNumber` first, then `rowNumber ASC, id ASC`.
- see [configuration.md](configuration.md) for the current import chunk-size and row-limit settings.
- see [../../api-demo.http](../../api-demo.http) for runnable request examples.
