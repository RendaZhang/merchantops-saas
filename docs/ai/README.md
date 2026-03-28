# AI Docs

This section collects AI-specific design, prompt, and evaluation documents that sit between roadmap intent and implementation-facing reference docs.

## Pages

- [prompt-versioning.md](prompt-versioning.md): how the project versions the current ticket and import AI generation prompts plus future workflow variants
- [eval-dataset-guidelines.md](eval-dataset-guidelines.md): how to build and maintain lightweight evaluation datasets for current public AI workflows and future candidates
- [workflow-candidates.md](workflow-candidates.md): which workflow-first AI candidates are already public and which remain future planning inputs

## Scope

These documents now support the live ticket and import AI surfaces plus the future AI roadmap.

Current reality:

- the ticket AI public surface now includes `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft`
- the import AI public surface now includes `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`
- the current public AI workflows are still read-only or suggestion-only; no public AI endpoint mutates ticket or import business state directly
- prompt versioning and eval guidance already apply to the live ticket and import AI paths, not just future design work

## Related Documents

- [../reference/ai-integration.md](../reference/ai-integration.md): current AI workflow guardrails and public contract
- [../reference/ai-provider-configuration.md](../reference/ai-provider-configuration.md): current provider ownership and config keys
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): regression checklist for public AI workflow changes
- [../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md](../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md): AI placement and approval model
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): audit and eval baseline decision
