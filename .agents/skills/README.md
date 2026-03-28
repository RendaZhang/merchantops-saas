# Repo Skills

This directory stores repo-local skills for workflows that are stable, repeated, and too detailed to keep directly inside [AGENTS.md](../../AGENTS.md).

Current skills:

- [doc-staged-sync/SKILL.md](doc-staged-sync/SKILL.md): route staged or recent implementation changes into the right documentation updates, or run a repo-wide documentation maintenance pass when no staged diff exists.
- [phase-status-sync/SKILL.md](phase-status-sync/SKILL.md): keep `project-status`, `roadmap`, and `project-plan` aligned without mixing their responsibilities.
- [release-tag-prep/SKILL.md](release-tag-prep/SKILL.md): prepare or finalize release-tag documentation, changelog text, milestone baselines, and open-source release-cut docs.
- [import-surface-sync/SKILL.md](import-surface-sync/SKILL.md): align import workflow docs, import AI docs, examples, runbooks, and milestone text with the current import implementation.
- [ai-ticket-surface-sync/SKILL.md](ai-ticket-surface-sync/SKILL.md): align public ticket AI docs, provider/runtime wording, runbooks, examples, and milestone text with the current Week 6 implementation.
- [tdr-last-cycle/SKILL.md](tdr-last-cycle/SKILL.md): iterate `TT last`, `DOC last`, and `RR last` against the most recent commit until no fixable findings remain.

Keep repo-local skills concise. Prefer linking existing repository docs over copying long rules into skill files.

Validation note:

- Repo-local skill validation currently uses `quick_validate.py` from the system `skill-creator` skill and expects `PyYAML` to be available in the active Python interpreter.
