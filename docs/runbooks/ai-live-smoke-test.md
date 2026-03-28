# AI Live Smoke Test

Last updated: 2026-03-28

> Maintenance note: keep this page focused on the current low-cost local provider live smoke path for the public AI surface. It is intentionally ticket-summary-first and budget-limited, with ticket triage, ticket reply-draft, and import AI error summary plus mapping suggestion plus fix recommendation treated as later expansions only after the first summary succeeds. Broader AI regression scope still belongs in [ai-regression-checklist.md](ai-regression-checklist.md).

Use this runbook when local AI provider wiring, `.env` loading, provider normalization, or vendor compatibility changed and you need one real local provider pass.

## Scope And Stop Rules

This runbook is intentionally narrow:

- prepare `.env` with local provider settings
- start Docker dependencies
- start the API with repository-root local `.env` auto-loading
- log in as `demo-shop/admin`
- create one fresh smoke ticket and add enough context for summary generation
- call `POST /api/v1/tickets/{id}/ai-summary` exactly once
- if that succeeds, immediately call `GET /api/v1/tickets/{id}/ai-interactions`
- only after summary succeeds, optionally expand the same session to `ai-triage`, `GET /ai-interactions?interactionType=TRIAGE`, `ai-reply-draft`, `GET /ai-interactions?interactionType=REPLY_DRAFT`, one import `ai-error-summary` call plus `GET /api/v1/import-jobs/{id}/ai-interactions?interactionType=ERROR_SUMMARY`, and when eligible one import `ai-mapping-suggestion` call plus one import `ai-fix-recommendation` call, each followed by the matching import history read, against known failed import jobs
- if any live endpoint fails, stop immediately and do not continue to the next endpoint

Budget guard for the first live pass:

- keep spend at or below `1 RMB`
- use one summary call only
- expand to `ai-triage`, `ai-reply-draft`, import `ai-error-summary`, import `ai-mapping-suggestion`, or import `ai-fix-recommendation` only after the summary path is proven locally

## 1. Run Automated Tests First

Preferred command:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

If the change also touched broader non-AI workflow wiring, run [local-smoke-test.md](local-smoke-test.md) for the normal public baseline before this AI-specific live pass.

## 2. Prepare `.env`

If you do not already have a local `.env`, create one from the example:

```powershell
Copy-Item .env.example .env
```

Add local AI provider settings to `.env`. Recommended provider-neutral DeepSeek example:

```dotenv
MERCHANTOPS_AI_ENABLED=true
MERCHANTOPS_AI_PROVIDER=DEEPSEEK
MERCHANTOPS_AI_BASE_URL=https://api.deepseek.com
MERCHANTOPS_AI_MODEL_ID=deepseek-chat
MERCHANTOPS_AI_API_KEY=replace-with-local-deepseek-key
```

DeepSeek local convenience aliases are also supported when `MERCHANTOPS_AI_PROVIDER=DEEPSEEK` and the provider-neutral key is blank:

```dotenv
DEEPSEEK_API_KEY=replace-with-local-deepseek-key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
```

Keep real provider keys only in the gitignored local `.env`.

## 3. Start Docker Dependencies

```powershell
docker compose up -d
docker compose ps
```

## 4. Build And Start The API

Refresh local SNAPSHOT dependencies first:

```powershell
.\mvnw.cmd -pl merchantops-api -am install -DskipTests
```

Then start the API from the module directory:

```powershell
Set-Location .\merchantops-api
..\mvnw.cmd spring-boot:run
```

The main app entrypoint auto-loads the repository-root `.env` before Spring Boot starts for local dev-profile `spring-boot:run`.

## 5. Prepare Reusable Variables

Open a second PowerShell session from the repository root:

```powershell
$baseUrl = "http://localhost:8080"
$tenantCode = "demo-shop"
$adminUsername = "admin"
$adminPassword = "123456"
$smokePrefix = "ai-live-{0}" -f [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$ticketTitle = "$smokePrefix provider compatibility smoke"
$ticketDescription = "Verify local provider-normalized AI summary wiring."
$summaryRequestId = "$smokePrefix-summary"
```

## 6. Login As Admin

```powershell
$adminLogin = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    tenantCode = $tenantCode
    username = $adminUsername
    password = $adminPassword
  } | ConvertTo-Json -Compress)

$token = $adminLogin.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $token" }
```

## 7. Create A Fresh Smoke Ticket And Add Context

```powershell
$ticketCreate = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/tickets" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    title = $ticketTitle
    description = $ticketDescription
  } | ConvertTo-Json -Compress)

$ticketId = $ticketCreate.data.id

$ticketStatus = Invoke-RestMethod `
  -Method Patch `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/status" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    status = "IN_PROGRESS"
  } | ConvertTo-Json -Compress)

$ticketComment = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/comments" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body (@{
    content = "Local AI live smoke: confirm provider compatibility for ticket summary."
  } | ConvertTo-Json -Compress)
```

This creates the fresh ticket plus one status change and one comment so the summary path has enough current context.

## 8. Call `POST /ai-summary` Exactly Once

```powershell
$summaryHeaders = @{
  Authorization = "Bearer $token"
  "X-Request-Id" = $summaryRequestId
}

$summaryResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/ai-summary" `
  -Headers $summaryHeaders
```

Expected result:

- HTTP `200`
- non-blank `data.summary`
- non-blank `data.modelId`
- `data.requestId` equals `$summaryRequestId`

If this request fails, stop here. Do not run `ai-triage` or `ai-reply-draft` in the same session.

## 9. Immediately Verify `GET /ai-interactions`

```powershell
$interactionHistory = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/ai-interactions?page=0&size=10&interactionType=SUMMARY" `
  -Headers $adminHeaders
```

Expected result:

- the first returned item is the new summary interaction
- `status=SUCCEEDED`
- `requestId` equals `$summaryRequestId`
- `modelId` matches the resolved provider model
- usage fields are present when the provider returns them, otherwise `null`

## 10. Optional Stage 2 Expansion After Summary Success

Use this stage only after the summary call and the matching `SUMMARY` interaction-history read both succeed.

Prepare request ids:

```powershell
$triageRequestId = "$smokePrefix-triage"
$replyDraftRequestId = "$smokePrefix-reply-draft"
```

Run triage first:

```powershell
$triageHeaders = @{
  Authorization = "Bearer $token"
  "X-Request-Id" = $triageRequestId
}

$triageResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/ai-triage" `
  -Headers $triageHeaders

$triageHistory = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/ai-interactions?page=0&size=10&interactionType=TRIAGE" `
  -Headers $adminHeaders
```

Expected triage result:

- HTTP `200`
- non-blank `data.classification`
- `data.priority` is `LOW`, `MEDIUM`, or `HIGH`
- non-blank `data.reasoning`
- `data.modelId` matches the resolved provider model
- the first `TRIAGE` interaction-history row is `SUCCEEDED` and matches `$triageRequestId`

Only if triage succeeds, run reply-draft:

```powershell
$replyDraftHeaders = @{
  Authorization = "Bearer $token"
  "X-Request-Id" = $replyDraftRequestId
}

$replyDraftResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/ai-reply-draft" `
  -Headers $replyDraftHeaders

$replyDraftHistory = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/tickets/$ticketId/ai-interactions?page=0&size=10&interactionType=REPLY_DRAFT" `
  -Headers $adminHeaders
```

Expected reply-draft result:

- HTTP `200`
- non-blank `data.opening`, `data.body`, `data.nextStep`, `data.closing`, and `data.draftText`
- `data.modelId` matches the resolved provider model
- the first `REPLY_DRAFT` interaction-history row is `SUCCEEDED` and matches `$replyDraftRequestId`

Stop immediately if triage fails. Do not continue to reply-draft in the same session.

## 11. Optional Import AI Pass

Use this stage only after the summary call and the matching `SUMMARY` interaction-history read both succeed.

Pick a current-tenant import job that already has row-level failures or a partial-failure terminal result. The existing import requests in [../../api-demo.http](../../api-demo.http) are the fastest way to create one locally if needed.

```powershell
$importJobId = 1201
$importSummaryRequestId = "$smokePrefix-import-error-summary"
$importSummaryHeaders = @{
  Authorization = "Bearer $token"
  "X-Request-Id" = $importSummaryRequestId
}

$importSummaryResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId/ai-error-summary" `
  -Headers $importSummaryHeaders
```

Expected result:

- HTTP `200`
- non-blank `data.summary`
- non-empty `data.topErrorPatterns`
- non-empty `data.recommendedNextSteps`
- `data.requestId` equals `$importSummaryRequestId`

If this request fails, stop here. Do not keep spending tokens on more live AI calls in the same session.

Immediately verify the stored import history row:

```powershell
$importSummaryHistory = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId/ai-interactions?page=0&size=10&interactionType=ERROR_SUMMARY" `
  -Headers $adminHeaders
```

Expected result:

- the first returned item is the new import error-summary interaction
- `status=SUCCEEDED`
- `requestId` equals `$importSummaryRequestId`
- `modelId` matches the resolved provider model

If the selected import job also has parseable sanitized header/global signal, an optional follow-up mapping-suggestion call is:

```powershell
$importMappingRequestId = "$smokePrefix-import-mapping-suggestion"
$importMappingHeaders = @{
  Authorization = "Bearer $token"
  "X-Request-Id" = $importMappingRequestId
}

$importMappingResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId/ai-mapping-suggestion" `
  -Headers $importMappingHeaders
```

Expected result:

- HTTP `200`
- non-blank `data.summary`
- exactly five canonical `data.suggestedFieldMappings`
- non-empty `data.confidenceNotes`
- non-empty `data.recommendedOperatorChecks`
- `data.requestId` equals `$importMappingRequestId`

Immediately verify the stored mapping-suggestion history row:

```powershell
$importMappingHistory = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId/ai-interactions?page=0&size=10&interactionType=MAPPING_SUGGESTION" `
  -Headers $adminHeaders
```

Expected result:

- the first returned item is the new import mapping-suggestion interaction
- `status=SUCCEEDED`
- `requestId` equals `$importMappingRequestId`
- `modelId` matches the resolved provider model

If the selected import job also has grounded row-level failure signal, an optional follow-up fix-recommendation call is:

```powershell
$importFixRequestId = "$smokePrefix-import-fix-recommendation"
$importFixHeaders = @{
  Authorization = "Bearer $token"
  "X-Request-Id" = $importFixRequestId
}

$importFixResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId/ai-fix-recommendation" `
  -Headers $importFixHeaders
```

Expected result:

- HTTP `200`
- non-blank `data.summary`
- non-empty `data.recommendedFixes`
- non-empty `data.confidenceNotes`
- non-empty `data.recommendedOperatorChecks`
- `data.requestId` equals `$importFixRequestId`

Immediately verify the stored fix-recommendation history row:

```powershell
$importFixHistory = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/v1/import-jobs/$importJobId/ai-interactions?page=0&size=10&interactionType=FIX_RECOMMENDATION" `
  -Headers $adminHeaders
```

Expected result:

- the first returned item is the new import fix-recommendation interaction
- `status=SUCCEEDED`
- `requestId` equals `$importFixRequestId`
- `modelId` matches the resolved provider model

## 12. What Counts As A Stop Condition

Stop the live pass immediately if any of these happen:

- summary returns `503`
- summary throws a provider timeout or unavailable error
- summary returns malformed or blank JSON and is recorded as `INVALID_RESPONSE`
- the interaction-history read does not show the matching `SUMMARY` row after a successful summary call
- triage returns a non-`200` response, missing `classification`, missing `reasoning`, or a `priority` outside `LOW|MEDIUM|HIGH`
- the interaction-history read does not show the matching `TRIAGE/SUCCEEDED` row after a successful triage call
- reply draft returns a non-`200` response or is missing any of `opening`, `body`, `nextStep`, `closing`, or `draftText`
- the interaction-history read does not show the matching `REPLY_DRAFT/SUCCEEDED` row after a successful reply-draft call
- import error summary returns a non-`200` response or is missing `summary`, `topErrorPatterns`, or `recommendedNextSteps`
- the import interaction-history read does not show the matching `ERROR_SUMMARY`, `MAPPING_SUGGESTION`, or `FIX_RECOMMENDATION` `SUCCEEDED` row after a successful import AI generation call

When that happens, capture the response or log evidence, update the relevant AI docs if the behavior changed, and do not spend more tokens by continuing to the next live endpoint.

## Related Documents

- [ai-regression-checklist.md](ai-regression-checklist.md): AI-specific sign-off checklist after the live pass
- [local-smoke-test.md](local-smoke-test.md): broader non-AI happy-path smoke flow
- [../reference/ai-provider-configuration.md](../reference/ai-provider-configuration.md): supported provider keys, resolution order, and DeepSeek convenience aliases
