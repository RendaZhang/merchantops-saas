# Development Agent Guidance

Last updated: 2026-03-10

## Purpose

This page records the current implementation guidance for developers and coding agents working inside this repository.

Use it when changing code or development-facing documentation.

## Documentation Concerns

- Root `README.md` stays high-level. Do not use it as the place for detailed development rules.
- Put development-facing detail under `docs/`.
- Public API changes must update Swagger-facing reference docs, `api-demo.http`, runbooks, and `docs/project-status.md`.
- Internal groundwork must not be described as public API.
- If a new document is intended to be reused by later developers or agents, link it from `docs/README.md` and the appropriate docs index page.
- Read [documentation-maintenance.md](documentation-maintenance.md) before routing documentation updates.

## Testing Concerns

- Read [testing-agent-guidance.md](testing-agent-guidance.md) for the current verification baseline and preferred regression commands.
- Start with [../runbooks/automated-tests.md](../runbooks/automated-tests.md) for the current automated regression entry point.
- Public API changes require test updates and doc/runbook updates in the same change.
- Swagger rendering, live infra health, and uncovered authenticated paths still require manual follow-up through the linked testing guidance and runbooks.

## Development Concerns

### Core Rules

- Query model and write model must stay separate.
- Repository methods for tenant business data must always include `tenantId`.
- Service-layer public methods for tenant business data must always accept `tenantId` explicitly.
- Service-layer public methods for tenant-scoped write operations should also accept `operatorId` explicitly when operator attribution matters.
- Controllers may resolve `tenantId` from request context, but lower layers must not rely on implicit thread-local access.
- Query DTOs must describe read results only.
- Command DTOs must describe allowed write inputs only.
- Unimplemented business flows must throw unified business exceptions, not raw framework or JDK exceptions.

### Tenant Scope Rules

Apply these rules to user-management code and future tenant-scoped modules:

- do not expose `tenantId` as a public query parameter for tenant business APIs
- do resolve `tenantId` at the controller edge from authenticated context
- do pass `tenantId` explicitly into service methods
- do require `tenantId` in repository queries
- do not add repository methods such as `findByUsername(...)` for tenant-owned data without a tenant constraint
- do not load a user by `id` alone when the use case is tenant business access; use `id + tenantId`

Recommended shape:

1. controller resolves `tenantId`
2. controller resolves `operatorId` for write flows when attribution is required
3. controller calls service with explicit `tenantId` and, for writes, explicit `operatorId`
4. service calls repository with explicit `tenantId`
5. repository query constrains by `tenantId`

### Query Model Rules

Current user-management query model lives under:

- `merchantops-api/src/main/java/com/renda/merchantops/api/dto/user/query`
- `merchantops-api/src/main/java/com/renda/merchantops/api/service/UserQueryService.java`

Use query DTOs for:

- list items
- detail views
- detail role-code hydration
- page query objects
- page response objects

Do not use query DTOs for:

- create requests
- update requests
- password changes
- role assignment commands

Current `/api/v1/users` contract is the baseline example:

- tenant-scoped page query
- companion detail query at `GET /api/v1/users/{id}`
- explicit filters: `username`, `status`, `roleCode`
- response object with `items`, `page`, `size`, `total`, `totalPages`
- companion role lookup at `GET /api/v1/roles`
- companion write entrypoints at `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles` with explicit command DTO mapping

### Write Model Rules

Current user-management write model lives under:

- `merchantops-api/src/main/java/com/renda/merchantops/api/dto/user/command`
- `merchantops-api/src/main/java/com/renda/merchantops/api/service/UserCommandService.java`

Write DTOs should be split by intent rather than using one mutable all-fields object.

Current field policy for `users`:

- immutable after creation: `id`, `tenantId`, `username`, `createdAt`
- updateable profile fields: `displayName`, `email`
- dedicated status-management field: `status`
- credential-managed fields: persisted `passwordHash` plus create/password-update input `password`
- internal operator-attribution fields: `createdBy`, `updatedBy`
- system-managed only: `updatedAt`
- create-only inputs: raw `password`, `roleCodes`

Implications:

- profile updates must not accept `tenantId` or `username`
- profile updates must not accept `status`
- status updates should remain isolated from general profile updates
- password changes should remain isolated from profile updates
- role reassignment should use a dedicated command object instead of overloading user profile update
- role lookup and role reassignment should stay tenant-scoped and should not be hidden inside profile DTOs
- create flows should accept raw `password`, but service code must persist only BCrypt hashes
- raw passwords may contain internal spaces, but leading and trailing whitespace must be rejected consistently across create and login flows
- Week 2 operator attribution is internal only: write flows should persist `createdBy` / `updatedBy`, but those fields are not part of the current public Swagger contract

### Repository Rules For Users

Current `UserRepository` must continue to support:

- `id + tenantId` detail lookup
- `tenantId + username` unique lookup
- `tenantId + username` existence check
- `tenantId + username + id != currentId` uniqueness check for updates
- tenant-scoped list
- tenant-scoped status list
- tenant-scoped paged search

For future repository additions:

- prefer derived query methods when the filter is simple and fully tenant-scoped
- use explicit `@Query` when filtering crosses join tables, such as role-based filtering
- if a query joins tenant-owned tables, keep tenant consistency in the join condition

### Validation And Error Rules

- uniqueness checks belong in service logic even if the database also has a unique constraint
- not-found business reads should return `BizException(ErrorCode.NOT_FOUND, ...)`
- not-yet-implemented business write flows should return `BizException`, not `UnsupportedOperationException`
- permission enforcement stays at controller/interceptor level, not repository level
- protected JWT requests that depend on current roles or permissions must reject stale claims after status, role, or permission changes

### Extension Checklist

When extending user-management or another tenant-scoped module:

1. define whether the change is `public` or `internal`
2. add or update query DTOs and command DTOs separately
3. make `tenantId` explicit in service and repository signatures
4. update Swagger contract only when the endpoint is actually public
5. update [../reference/user-management.md](../reference/user-management.md) and related auth or role reference docs for public contract changes
6. update [documentation-maintenance.md](documentation-maintenance.md) if the routing rules themselves changed
7. update `AGENTS.md` if the new rule should guide future agents by default

## Related Documents

- [../reference/user-management.md](../reference/user-management.md): current public `/api/v1/users` contract
- [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md): authentication and permission behavior
- [documentation-maintenance.md](documentation-maintenance.md): which docs must change for which change type
- [testing-agent-guidance.md](testing-agent-guidance.md): verification and regression guidance for testing-focused work
- [review-release-agent-guidance.md](review-release-agent-guidance.md): staged review and release guidance
- [../runbooks/automated-tests.md](../runbooks/automated-tests.md): automated verification entry
- [../project-status.md](../project-status.md): implemented reality and current limitations
