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

Worker behavior (Slice B):

1. consumes `jobId` and transitions `QUEUED -> PROCESSING`
2. validates fixed header + row shape
3. parses each row and performs field-level checks (`username`, `displayName`, `email`, `password`, `roleCodes`)
4. executes one row in one independent transaction via the existing user-create service chain
5. records row errors in `import_job_item_error` for both parse-level and business-level failures
6. writes terminal status + audit events (`IMPORT_JOB_COMPLETED` / `IMPORT_JOB_FAILED`)

Current error-code examples in `itemErrors`:

- parse/header: `EMPTY_FILE`, `INVALID_HEADER`, `INVALID_ROW_SHAPE`, `FILE_READ_ERROR`
- business-row: `DUPLICATE_USERNAME`, `UNKNOWN_ROLE`, `INVALID_EMAIL`, `INVALID_PASSWORD`

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
- local file storage is intentionally replaceable for later object-storage rollout.
- list paging currently supports `page` and `size` only; default size is `10` and max size is `100`.
- current list ordering is `createdAt DESC, id DESC`.
- see [../../api-demo.http](../../api-demo.http) for runnable request examples.
