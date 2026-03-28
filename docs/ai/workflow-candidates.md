# AI Workflow Candidates

Last updated: 2026-03-28

## Purpose

This document tracks which workflow-first AI candidates have already graduated into public APIs and which still remain future planning inputs.

The goal is not to list every possible AI feature. The goal is to identify a small set of workflow-first candidates that:

- fit the tenant-scoped SaaS architecture
- have clear operator value
- can be reviewed by humans
- do not require unsafe autonomy

## Current Boundary

As of today:

- eight public AI endpoints are already live in Swagger: ticket and import interaction-history reads plus six suggestion-generating endpoints
- ticket summary, ticket triage, ticket reply draft, import error summary, import mapping suggestion, and import fix recommendation are already committed public contracts
- only items explicitly marked as remaining candidates below should be treated as future planning input rather than current API contract

## Selection Criteria

Prefer candidates that are:

- attached to an existing workflow
- narrow in scope
- easy to verify manually
- low or medium risk
- helpful even when used only as a suggestion

Avoid candidates that:

- require cross-tenant context
- directly mutate critical business state without review
- depend on broad agent autonomy to be useful

## Public Baseline Already Landed

### 1. Ticket Summary

- Workflow area: ticket operations
- Primary value: reduce reading time for operators
- Typical input:
  - ticket title
  - ticket description
  - recent comments
  - current status and assignee
- Typical output:
  - concise summary
  - unresolved issues
  - next suggested action
- Risk level: low
- Approval required: no, if read-only
- Current status: public suggestion-only endpoint

### 2. Ticket Triage Suggestion

- Workflow area: ticket operations
- Primary value: suggest category and urgency
- Typical input:
  - ticket title
  - ticket description
  - recent context
- Typical output:
  - category suggestion
  - priority suggestion
  - short rationale
- Risk level: low to medium
- Approval required: yes, if the system later writes triage decisions automatically
- Current status: public suggestion-only endpoint

### 3. Reply Draft Generation

- Workflow area: ticket operations
- Primary value: speed up operator responses
- Typical input:
  - ticket content
  - recent comments
  - allowed response style or template
- Typical output:
  - draft reply
  - optional recommended follow-up question
- Risk level: medium
- Approval required: yes before using the draft as a final operator response
- Current status: public suggestion-only endpoint

### 4. Import Error Summary

- Workflow area: import operations
- Primary value: explain dominant failure patterns quickly
- Typical input:
  - import job metadata
  - representative error rows
  - error messages
- Typical output:
  - top failure categories
  - human-readable summary
  - likely next steps
- Risk level: low
- Approval required: no, if read-only
- Current status: public suggestion-only endpoint

### 5. Field Mapping Suggestion

- Workflow area: import setup and correction
- Primary value: reduce mapping setup friction
- Typical input:
  - source column headers
  - sample values
  - target schema description
- Typical output:
  - suggested field mapping
  - ambiguous fields requiring manual review
- Risk level: medium
- Approval required: yes before applying mappings
- Current status: public suggestion-only endpoint

### 6. Fix Recommendation

- Workflow area: import error handling
- Primary value: suggest what operators should correct
- Typical input:
  - failed rows
  - validation errors
  - schema rules
- Typical output:
  - grouped fix suggestions
  - examples of corrected values
  - rows that still require manual inspection
- Risk level: medium
- Approval required: yes before any downstream corrective action
- Current status: public suggestion-only endpoint

The current public baseline also includes `GET /api/v1/tickets/{id}/ai-interactions` and `GET /api/v1/import-jobs/{id}/ai-interactions` so operators can review stored AI interaction history without re-triggering generation.

## Remaining Ticket Workflow Candidates

### 7. Assignee Suggestion

- Workflow area: ticket operations
- Primary value: propose the right owner
- Typical input:
  - ticket category or issue type
  - ticket urgency
  - current assignee availability data if available later
- Typical output:
  - suggested assignee
  - confidence or short rationale
- Risk level: medium
- Approval required: yes
- Why it is valuable:
  - fits operations workflows
  - more valuable once team or ownership data is richer
- Suggested priority: next ticket-focused candidate, not part of the current public baseline

## Agentic Workflow Candidates

These should only follow after copilot-style read and suggestion features are stable.

### 8. Draft Then Approve Reply

- Workflow area: ticket operations
- Agent action: create a reply draft and submit it for approval
- Risk level: medium
- Approval required: always
- Why it is viable:
  - bounded write surface
  - clear manual checkpoint
- Suggested priority: first agentic candidate

### 9. Pre-Execution Recommendation Plan

- Workflow area: import or ticket operations
- Agent action: propose a multi-step remediation plan without executing it
- Risk level: medium
- Approval required: yes before any step execution
- Why it is viable:
  - demonstrates agent thinking without unsafe autonomy
- Suggested priority: later candidate

## Not Recommended Early Candidates

Avoid these in the first AI wave:

- automatic user creation or role assignment
- automatic ticket closing
- automatic import data repair with direct database writes
- any AI action that can cross tenant boundaries
- open-ended generic assistant with broad repo or database access

## Recommended Next Candidate Order

The early workflow-first baseline is already public. The next realistic order is:

- assignee suggestion
- draft then approve reply
- pre-execution recommendation plan

## Minimal Readiness Checklist Per Candidate

Before promoting a candidate into implementation, confirm:

- the source workflow already exists in code
- tenant scope is explicit
- permission boundaries are clear
- output can be reviewed by a human
- regression examples can be prepared
- audit logging fields are known

## Related Documents

- [README.md](README.md): AI docs navigation
- [prompt-versioning.md](prompt-versioning.md): how workflow prompts should be versioned
- [eval-dataset-guidelines.md](eval-dataset-guidelines.md): how to prepare small eval sets
- [../reference/ai-integration.md](../reference/ai-integration.md): AI guardrails and current public contract
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): validation checklist once AI endpoints land
