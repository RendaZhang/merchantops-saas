# Import Jobs

Last updated: 2026-03-12

## Public API Surface

Week 5 Slice A exposes three async import endpoints:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/import-jobs` | Bearer JWT | `USER_WRITE` | Accepts multipart request + CSV file, creates a `QUEUED` job, and schedules the MQ publish after transaction commit |
| `GET` | `/api/v1/import-jobs` | Bearer JWT | `USER_READ` | Pages current-tenant import jobs |
| `GET` | `/api/v1/import-jobs/{id}` | Bearer JWT | `USER_READ` | Returns one current-tenant import job with item errors |

## Current Slice A Contract

- supported `sourceType`: `CSV`
- supported `importType`: `USER_CSV`
- status model: `QUEUED`, `PROCESSING`, `SUCCEEDED`, `FAILED`
- tenant scope always comes from JWT context

`POST /api/v1/import-jobs` accepts multipart form data:

- `request` (JSON): `{ "importType": "USER_CSV" }`
- `file` (CSV): uploaded source file

On submit, API:

1. validates tenant/operator/requestId context
2. stores file to local import storage path (`data/imports` by default)
3. inserts `import_job` with `QUEUED`
4. registers an after-commit publish for `ImportJobMessage(jobId, tenantId)`
5. writes audit event `IMPORT_JOB_CREATED`

Worker behavior (Slice A):

- consumes `jobId`
- transitions `QUEUED -> PROCESSING`
- validates CSV header and row shape
- writes parse-level errors to `import_job_item_error`
- updates `totalCount`, `successCount`, `failureCount`
- writes terminal status + audit events (`IMPORT_JOB_COMPLETED` / `IMPORT_JOB_FAILED`)

## Example Requests And Responses

Example multipart request:

```http
POST /api/v1/import-jobs
Authorization: Bearer <admin-token>
Content-Type: multipart/form-data

request={"importType":"USER_CSV"}
file=@users.csv
```

Example create response:

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
    "status": "QUEUED",
    "requestedBy": 1,
    "requestId": "req-import-create-1201",
    "totalCount": 0,
    "successCount": 0,
    "failureCount": 0,
    "errorSummary": null,
    "createdAt": "2026-03-12T16:20:00",
    "startedAt": null,
    "finishedAt": null,
    "itemErrors": []
  }
}
```

Example list response:

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
        "totalCount": 1,
        "successCount": 1,
        "failureCount": 0,
        "errorSummary": null,
        "createdAt": "2026-03-12T16:20:00",
        "startedAt": "2026-03-12T16:20:02",
        "finishedAt": "2026-03-12T16:20:03"
      }
    ],
    "page": 0,
    "size": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

Example detail response with parse-level errors:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 1202,
    "tenantId": 1,
    "importType": "USER_CSV",
    "sourceType": "CSV",
    "sourceFilename": "users-invalid.csv",
    "storageKey": "1/660e8400-e29b-41d4-a716-446655440000-users-invalid.csv",
    "status": "FAILED",
    "requestedBy": 1,
    "requestId": "req-import-create-1202",
    "totalCount": 1,
    "successCount": 0,
    "failureCount": 1,
    "errorSummary": "all rows failed validation",
    "createdAt": "2026-03-12T16:25:00",
    "startedAt": "2026-03-12T16:25:02",
    "finishedAt": "2026-03-12T16:25:03",
    "itemErrors": [
      {
        "id": 31,
        "rowNumber": 2,
        "errorCode": "INVALID_ROW_SHAPE",
        "errorMessage": "column count mismatch",
        "rawPayload": "broken-only-one-column",
        "createdAt": "2026-03-12T16:25:03"
      }
    ]
  }
}
```

## Notes

- This slice is an async backbone only. It does **not** write user/ticket business data yet.
- Local file storage is intentionally replaceable for later object-storage rollout.
- `import_job_item_error` now keeps DB-level same-tenant linkage to its parent `import_job`.
- list paging currently supports `page` and `size` only; default size is `10` and max size is `100`
- current list ordering is `createdAt DESC, id DESC`
- See [../../api-demo.http](../../api-demo.http) for runnable request examples.
