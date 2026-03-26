# API Conventions

## Response Wrapper

The project uses `ApiResponse<T>` as the unified response envelope.

Fields:

- `code`: business-level result code
- `message`: human-readable message
- `data`: payload, nullable

Example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "message": "hello"
  }
}
```

## Error Model

- `ApiResponse<T>` in `merchantops-api`
- `ErrorCode` in `merchantops-domain`
- `BizException` for business-layer failures
- `GlobalExceptionHandler` in `merchantops-api` for HTTP mapping

## Mapped Error Behavior

- Validation failures return `VALIDATION_ERROR`
- Malformed JSON returns `BAD_REQUEST`
- Business errors map to the appropriate HTTP status such as `401`, `403`, or `404`
- Unexpected server exceptions return a generic message and do not leak internal details

Malformed JSON example:

```json
{
  "code": "BAD_REQUEST",
  "message": "invalid request body",
  "data": null
}
```
