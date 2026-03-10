# AI Docs

This section collects AI-specific design and operating documents that sit between high-level roadmap intent and future implementation details.

## Pages

- [prompt-versioning.md](prompt-versioning.md): how to version prompts and workflow variants before public AI APIs are considered stable
- [eval-dataset-guidelines.md](eval-dataset-guidelines.md): how to build and maintain lightweight evaluation datasets for future AI workflow features
- [workflow-candidates.md](workflow-candidates.md): prioritized AI workflow candidates for ticket, import, and low-risk agent use cases

## Scope

These documents are preparatory architecture and operations guidance.

As of today:

- no AI endpoints are public in Swagger
- no AI workflow is yet part of the released HTTP contract
- these pages exist so Week 6+ AI work lands with repeatable review and regression practices

## Related Documents

- [../reference/ai-integration.md](../reference/ai-integration.md): AI workflow guardrails and planned endpoint direction
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): regression checklist once AI endpoints start landing
- [../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md](../architecture/adr/0007-embed-ai-into-tenant-scoped-workflows-with-human-oversight.md): AI placement and approval model
- [../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md](../architecture/adr/0008-establish-ai-audit-and-evaluation-baseline-before-public-ai-apis.md): audit and eval baseline decision
