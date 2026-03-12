# Import Jobs

Last updated: 2026-03-12

## Public API Surface

Week 5 now exposes five async import endpoints:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/import-jobs` | Bearer JWT | `USER_WRITE` | Accepts multipart request + CSV file, creates a `QUEUED` job, and schedules the MQ publish after transaction commit |
| `GET` | `/api/v1/import-jobs` | Bearer JWT | `USER_READ` | Pages current-tenant import jobs with optional queue filters |
| `GET` | `/api/v1/import-jobs/{id}` | Bearer JWT | `USER_READ` | Returns one current-tenant import job overview with `errorCodeCounts` plus backward-compatible `itemErrors` |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures` | Bearer JWT | `USER_WRITE` | Creates a new derived `QUEUED` job from the source job's replayable failed rows only |
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

Worker behavior (Slice B):

1. consumes `jobId` and transitions `QUEUED -> PROCESSING`
2. parses the file through the current CSV parser, strips a UTF-8 BOM on the first header cell when present, and validates fixed header + row shape
3. parses each row and performs field-level checks (`username`, `displayName`, `email`, `password`, `roleCodes`)
4. executes one row in one independent transaction via the existing user-create service chain
5. records row errors in `import_job_item_error` for both parse-level and business-level failures
6. writes terminal status + audit events (`IMPORT_JOB_COMPLETED` / `IMPORT_JOB_FAILED`)

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
- replaying only selected `errorCode` values
- editing failed rows before replay
- automatic dedupe or idempotency ledger behavior beyond current business validation

Current error-code examples in `itemErrors`:

- parse/header: `EMPTY_FILE`, `INVALID_HEADER`, `INVALID_ROW_SHAPE`, `FILE_READ_ERROR`, `UNSUPPORTED_IMPORT_TYPE`
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

- `totalCount`: total data rows read (excluding header)
- `successCount`: rows that really created tenant users
- `failureCount`: rows that failed parse validation or business execution
- `errorCodeCounts`: aggregated counts by `errorCode` for quick triage
- `sourceJobId`: `null` for original jobs, populated only for replay-derived jobs
- clean success: `status=SUCCEEDED`, `failureCount=0`, `errorSummary=null`
- partial success: `status=SUCCEEDED`, `failureCount>0`, `errorSummary="completed with some row errors"`
- full failure: `status=FAILED`

## Governance Behavior

- job-level audit events remain `IMPORT_JOB_*`.
- replay writes `IMPORT_JOB_REPLAY_REQUESTED` on the source job and keeps `IMPORT_JOB_CREATED` on the new replay job with `sourceJobId` in the created snapshot.
- each successful user creation still emits existing `USER_CREATED` audit events through `UserCommandService`.
- created users still persist tenant/operator attribution (`tenantId`, `createdBy`, `updatedBy`) from the import request context.

## Notes

- this slice is intentionally narrow: only `USER_CSV` business-row execution plus failed-row replay is implemented.
- quoted CSV fields are supported by the current parser, so commas inside quoted values do not force an `INVALID_ROW_SHAPE` by themselves.
- unsupported import types currently fail before row processing begins and are recorded as terminal job failures.
- local file storage is intentionally replaceable for later object-storage rollout.
- replay uses the same storage abstraction as uploaded files so system-generated replay CSVs and user uploads follow one storage path.
- list paging now supports `page`, `size`, `status`, `importType`, `requestedBy`, and `hasFailuresOnly`; default size is `10` and max size is `100`.
- error paging now supports `page`, `size`, and `errorCode`; default size is `10` and max size is `100`.
- list items expose `requestedBy` and derived `hasFailures`; detail exposes `sourceJobId`, `errorCodeCounts`, and backward-compatible `itemErrors`; `/errors` pages the same failure rows for larger jobs.
- current list ordering remains `createdAt DESC, id DESC`.
- current failure-item ordering remains stable: null `rowNumber` first, then `rowNumber ASC, id ASC`.
- see [../../api-demo.http](../../api-demo.http) for runnable request examples.
