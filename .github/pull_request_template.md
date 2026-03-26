## Summary

- What changed?
- Why was this change needed?

## Testing

- [ ] `.\mvnw.cmd -pl merchantops-api -am test`
- [ ] Local smoke or manual verification, if needed
- [ ] Not run, with reason explained below

## Java Structure

- [ ] Capability package placement checked for new or moved Java types
- [ ] No new direct `merchantops-api -> merchantops-infra` dependency or capability logic under `api.platform`
- [ ] `docs/contributing/java-code-style.md` and `docs/architecture/java-architecture-map.md` updated if Java structure rules changed

## Documentation

- [ ] README updated if repository entry-point messaging changed
- [ ] Reference docs updated if public API or workflow behavior changed
- [ ] Runbooks updated if verification reality changed
- [ ] `api-demo.http` updated if public request or response examples changed

## API / Contract Impact

- [ ] Swagger-visible API changed
- [ ] Request or response shape changed
- [ ] No public API impact

## Breaking Changes

- [ ] No
- [ ] Yes, described below

## Notes

Add any reviewer context, rollout notes, or follow-up items here.
