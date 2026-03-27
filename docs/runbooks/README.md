# Runbooks

Runbooks are task-oriented documents for verification and operational debugging.

## Pages

- [automated-tests.md](automated-tests.md): fastest regression entry point and current automated coverage scope
- [local-smoke-test.md](local-smoke-test.md): current PowerShell-first end-to-end happy-path smoke flow for the public workflow surface
- [ai-live-smoke-test.md](ai-live-smoke-test.md): local `.env`-driven provider live smoke path for the current public AI surface, starting with ticket summary and expanding to import AI error summary plus mapping suggestion only after the first live pass succeeds
- [regression-checklist.md](regression-checklist.md): broader sign-off checklist for infra, auth, RBAC, tenant isolation, business APIs, and release-ready regression review
- [ai-regression-checklist.md](ai-regression-checklist.md): baseline validation checklist for the current public AI workflow endpoints plus later AI runtime, audit-log, and eval changes
