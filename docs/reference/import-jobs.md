# Import Jobs

Last updated: 2026-03-12

## Public API Surface

Week 5 Slice A exposes three async import endpoints:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/import-jobs` | Bearer JWT | `USER_WRITE` | Accepts multipart request + CSV file, creates a `QUEUED` job, and publishes MQ message |
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
4. publishes `ImportJobMessage(jobId, tenantId)`
5. writes audit event `IMPORT_JOB_CREATED`

Worker behavior (Slice A):

- consumes `jobId`
- transitions `QUEUED -> PROCESSING`
- validates CSV header and row shape
- writes parse-level errors to `import_job_item_error`
- updates `totalCount`, `successCount`, `failureCount`
- writes terminal status + audit events (`IMPORT_JOB_COMPLETED` / `IMPORT_JOB_FAILED`)

## Notes

- This slice is an async backbone only. It does **not** write user/ticket business data yet.
- Local file storage is intentionally replaceable for later object-storage rollout.
- See [../../api-demo.http](../../api-demo.http) for runnable request examples.
