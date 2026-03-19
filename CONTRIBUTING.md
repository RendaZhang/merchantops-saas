# Contributing to MerchantOps SaaS

Thank you for considering a contribution to MerchantOps SaaS.

This repository is an open-source reference implementation for a workflow-first, AI-enhanced vertical SaaS backend. Contributions that improve correctness, documentation quality, developer experience, and workflow credibility are welcome.

## Before You Start

- Read [README.md](README.md) for the current release framing and repository entry points.
- Use [docs/getting-started/README.md](docs/getting-started/README.md) for local setup.
- Use [docs/contributing/README.md](docs/contributing/README.md) for repository-specific contributor and agent workflow guidance.
- If you are reporting a security issue, do not open a public issue first. Follow [SECURITY.md](SECURITY.md).

## Local Setup

The shortest path to a running local environment is documented here:

- [docs/getting-started/README.md](docs/getting-started/README.md)

Default automated regression command:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

If you need more test and smoke-test context, see:

- [docs/runbooks/automated-tests.md](docs/runbooks/automated-tests.md)
- [docs/runbooks/local-smoke-test.md](docs/runbooks/local-smoke-test.md)
- [docs/runbooks/regression-checklist.md](docs/runbooks/regression-checklist.md)

## Pull Request Expectations

Please keep pull requests focused and reviewable.

- Explain the problem and the change in the PR summary.
- Call out any API, schema, or workflow behavior changes.
- Include the verification you ran.
- Keep unrelated cleanup out of the same PR unless it is required for correctness.

## Documentation And API Sync Rules

If your change affects a public API, Swagger-visible contract, workflow behavior, or verification reality, update the related docs in the same change.

Typical sync expectations include:

- `docs/reference/` pages for the affected public surface
- `api-demo.http` when request or response examples changed
- `docs/runbooks/` when verification steps or coverage reality changed
- `docs/project-status.md` when the current repository baseline changed

For the full routing rules, see:

- [docs/contributing/documentation-maintenance.md](docs/contributing/documentation-maintenance.md)

## Coding And Review Guidance

Repository-specific contribution guidance lives in:

- [docs/contributing/README.md](docs/contributing/README.md)
- [AGENTS.md](AGENTS.md)

These pages define the repository's role-based workflow, documentation rules, testing expectations, and release-note alignment rules.

## Community Standards

By participating in this project, you agree to follow:

- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## Questions And Discussion

- Use GitHub issues for bug reports and feature requests.
- Use pull requests for concrete changes.
- Use [SECURITY.md](SECURITY.md) for private security reporting.
