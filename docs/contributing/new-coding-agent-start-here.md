# New Coding Agent Start Here

Use this page as the shortest onboarding path for a new coding agent. It is a checklist, not a replacement for the detailed guidance pages.

## Checklist

- Read [../../AGENTS.md](../../AGENTS.md) first for repository roles, shortcuts, and cross-role rules.
- Read [development-agent-guidance.md](development-agent-guidance.md) before changing tenant-scoped code, public contracts, repositories, services, or DTOs.
- Read [java-code-style.md](java-code-style.md) before package refactors, shared-support extraction, or Java style cleanup.
- Read [../architecture/java-architecture-map.md](../architecture/java-architecture-map.md) before placing new Java types or moving code across modules or capability packages.
- Read [../../merchantops-api/README.md](../../merchantops-api/README.md) before editing API module code.
- Read [../../merchantops-domain/README.md](../../merchantops-domain/README.md) before editing domain module code.
- Read [../../merchantops-infra/README.md](../../merchantops-infra/README.md) before editing infrastructure module code.
- If a change affects a public contract, update the matching reference docs, runbooks, and examples in the same change.
- Keep comments focused on non-obvious intent, constraints, or tradeoffs. Do not add narration comments for self-explanatory code.
- Use `.\mvnw.cmd -pl merchantops-api -am test` as the default regression entry point unless the task calls for a narrower or broader verification set.
- Before proposing a commit, check [../../.github/pull_request_template.md](../../.github/pull_request_template.md) and make sure the staged change meets the same review expectations.

## When To Go Deeper

- Use [documentation-maintenance.md](documentation-maintenance.md) when code changes require doc-routing decisions.
- Use [testing-agent-guidance.md](testing-agent-guidance.md) when verification scope, test coverage notes, or smoke guidance must change.
- Use [review-release-agent-guidance.md](review-release-agent-guidance.md) when the task includes staged review, release notes, or tag work.
