# Import Jobs

Last updated: 2026-03-12

## Public API Surface

Week 5 exposes three async import endpoints:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/import-jobs` | Bearer JWT | `USER_WRITE` | Accepts multipart request + CSV file, creates a `QUEUED` job, and schedules the MQ publish after transaction commit |
| `GET` | `/api/v1/import-jobs` | Bearer JWT | `USER_READ` | Pages current-tenant import jobs |
| `GET` | `/api/v1/import-jobs/{id}` | Bearer JWT | `USER_READ` | Returns one current-tenant import job with item errors |

## Current Week 5 Slice B Contract (`USER_CSV`)

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

## Job Counter Semantics

After Slice B:

- `totalCount`: total data rows read (excluding header)
- `successCount`: rows that really created tenant users
- `failureCount`: rows that failed parse validation or business execution
- terminal `SUCCEEDED`: job finished with at least one success (partial success allowed)
- terminal `FAILED`: all rows failed, or pre-row fatal validation failed (for example empty file/header)

## Governance Behavior

- job-level audit events remain `IMPORT_JOB_*`.
- each successful user creation still emits existing `USER_CREATED` audit events through `UserCommandService`.
- created users still persist tenant/operator attribution (`tenantId`, `createdBy`, `updatedBy`) from the import request context.

## Notes

- this slice is intentionally narrow: only `USER_CSV` business-row execution is implemented.
- quoted CSV fields are supported by the current parser, so commas inside quoted values do not force an `INVALID_ROW_SHAPE` by themselves.
- unsupported import types currently fail before row processing begins and are recorded as terminal job failures.
- local file storage is intentionally replaceable for later object-storage rollout.
- list paging currently supports `page` and `size` only; default size is `10` and max size is `100`.
- current list ordering is `createdAt DESC, id DESC`.
- see [../../api-demo.http](../../api-demo.http) for runnable request examples.
