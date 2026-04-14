# Product Strategy

This page defines the long-term product strategy from the `v0.7.0-beta` foundation baseline. It is intentionally separate from the archived foundation plan, the current-status page, and the near-term roadmap.

## Strategic Baseline

- Current foundation baseline: `v0.7.0-beta`, `Week 10 complete: Delivery Hardening and Portfolio Packaging beta baseline`.
- Completed foundation: tenant isolation, RBAC, user management, ticket workflow, audit/approval, async import, ticket/import AI copilot surfaces, human-reviewed execution bridges, AI governance reads, feature flags, Dockerized API delivery, minimal CI, and portfolio/open-source handoff packaging.
- Planning mode: select narrow, evidence-backed release-line slices from the tracks below.

## Planning Model

The archived foundation plan used a fixed week-by-week cadence. Future planning should use release-line milestones instead:

- Define the next milestone by release line, such as a future beta tag target, not by assuming every body of work must fit one calendar week.
- Choose one or two strategic tracks for the milestone, then break the work into narrow `Slice A`, `Slice B`, and follow-on slices only as needed.
- Give each slice a stop condition that covers implementation, tests, public docs, runbooks, examples, and release evidence when applicable.
- Keep only the active milestone and the next few candidate slices in [roadmap.md](roadmap.md); do not rebuild another long week-by-week plan here.
- If a slice finishes earlier or later than expected, advance the roadmap by evidence rather than by calendar label.

## Long-Term Tracks

### Release And Deployment Maturity

- Turn the current CI and Docker baseline into repeatable release operations.
- Add release evidence expectations for exact tag commits, image publishing decisions, deployment documentation, and production-like smoke boundaries.
- Keep live AI provider checks, real MySQL migration verification, and Dockerized API live smoke explicit opt-in gates until they are stable enough for the default release path.

### Access Control And Tenant Integrity

- Strengthen authorization from endpoint-level permissions into a clearer product-grade access-control model.
- Prioritize database-level same-tenant integrity for role bindings and workflow actors before adding broader admin surfaces.
- Use [architecture/access-control-evolution-plan.md](architecture/access-control-evolution-plan.md) as the dedicated access-control strategy.

### Workflow Depth

- Deepen the existing user, ticket, import, audit, and approval workflows rather than adding unrelated modules.
- Candidate areas include approval history UX, workflow notifications, SLA/priority enrichment, import reporting, and safer operator-facing recovery tools.
- Keep AI-generated writes behind human-reviewed proposal and approval boundaries unless a future strategy explicitly changes that risk model.

### AI Governance Maturity

- Grow the Week 9 eval and usage baseline into a stronger review workflow for prompt changes, provider changes, and output-policy changes.
- Improve dataset coverage, prompt-version review expectations, and usage/cost interpretation without turning governance reads into billing or ledger infrastructure prematurely.
- Keep public AI endpoints read-only or suggestion-only unless the workflow approval model is deliberately widened.

### Commercial Discovery

- Treat commercialization as a discovery track, not a default implementation mandate.
- Validate whether tenant usage, approval throughput, import pain points, or AI-assisted support workflows provide enough signal for a minimal usage / ledger / invoice loop.
- Avoid building billing infrastructure before a concrete product question requires it.

## Strategic Horizons

These horizons describe the recommended execution order. They do not replace the strategic tracks above, and they should not be treated as fixed calendar commitments.

### Horizon 1: Productization Baseline

- Expand the minimal admin console from the current login/context shell into useful ticket, approval, import, AI interaction, and proposal review flows.
- Complete the basic authentication lifecycle with refresh, logout, and revocation behavior.
- Strengthen deployment and runtime readiness through documented environment, secret-management, bootstrap, and smoke-test paths.
- Move high-value tenant consistency guarantees from service-only checks toward database-backed constraints where practical.

### Horizon 2: SaaS Operations And Commercial Loop

- Turn existing AI interaction and usage metadata into tenant-level operational visibility.
- Introduce minimal tenant plan, quota or feature-gating, usage aggregation, invoice-preview, and internal usage-dashboard concepts.
- Keep the first commercial loop explanatory and operational; do not turn it into a full billing platform before the product question is clearer.

### Horizon 3: Workflow Depth Expansion

- Expand ticket realism through priority, SLA, notification, and attachment-oriented workflow depth.
- Broaden approval action coverage while preserving action-aware authorization and audit evidence.
- Add additional business import types only when they deepen merchant-operations workflows rather than increasing demo breadth for its own sake.

### Horizon 4: Reference Implementation Maturity

- Make the repository easier to run, demo, review, and extend through demo tenant data, quickstart paths, cloud deployment guidance, and clearer architecture diagrams.
- Add demo scripts and contributor onboarding that explain how AI, approvals, imports, feature flags, and usage governance fit together.
- Treat packaging work as product work when it improves external adoption or reviewability.

## Planning Rules

- Use [roadmap.md](roadmap.md) to choose the next concrete slice from these tracks.
- Use [project-status.md](project-status.md) to confirm what is already implemented before changing this page.
- Use [archive/completed-10-week-foundation-plan.md](archive/completed-10-week-foundation-plan.md) only for historical rationale, not future sequencing.
- Do not add public API commitments here unless they are already implemented or clearly marked as future direction.
- Do not default to more AI endpoints or higher-autonomy agents; AI should deepen workflow support, governance, and human-reviewed execution unless a future strategy deliberately changes that boundary.
