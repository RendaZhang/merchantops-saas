# AI Docs

This section collects AI-specific design and operating documents that sit between roadmap intent and implementation-facing reference docs.

## Pages

- [prompt-versioning.md](prompt-versioning.md): how the project versions the current ticket-summary and ticket-triage prompts plus future workflow variants
- [eval-dataset-guidelines.md](eval-dataset-guidelines.md): how to build and maintain lightweight evaluation datasets for current and future AI workflow features
- [workflow-candidates.md](workflow-candidates.md): prioritized AI workflow candidates for ticket, import, and low-risk agent use cases

## Scope

These documents now support the live Week 6 ticket summary and ticket triage slices plus the future AI roadmap.

Current reality:

- `POST /api/v1/tickets/{id}/ai-summary` and `POST /api/v1/tickets/{id}/ai-triage` are public in Swagger
- the current public AI workflows are still suggestion-only and read-only
- prompt versioning and eval guidance already apply to the live ticket summary and ticket triage paths, not just future design work

## Related Documents

- [../reference/ai-integration.md](../reference/ai-integration.md): current AI workflow guardrails and public contract
- [../reference/ai-provider-configuration.md](../reference/ai-provider-configuration.md): current provider ownership and config keys
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): regression checklist for public AI workflow changes
- [../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md](../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md): AI placement and approval model
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): audit and eval baseline decision
