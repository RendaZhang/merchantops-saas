# Project Guidance

## Documentation Quick Entry

- Read [docs/reference/documentation-maintenance.md](docs/reference/documentation-maintenance.md) before making documentation changes.
- Keep `README.md` high-level and move detail into `docs/`.
- Do not document an endpoint as public unless it is visible in Swagger.
- Public API change: update reference docs, `api-demo.http`, runbooks, and `docs/project-status.md`.
- Internal groundwork only: update `docs/project-status.md` and `docs/roadmap.md`, not public API reference.
- New architecture decision: add or update an ADR and `docs/architecture/README.md`.
- New tag or release milestone: update `CHANGELOG.md` and `docs/reference/release-versioning.md`.
