# Changelog

## 2026-03-08

- Initialize MerchantOps SaaS repository
- Add base directory structure
- Add initial README
- Add initial .gitignore
- Initialize multi-module Spring Boot backend skeleton
- Add Maven Wrapper
- Finalize base package structure
- Improve README with run instructions and module responsibilities
- Update project compiler baseline to Java 21
- Record current local environment as Java 21.0.10
- Reorganize README sections and consolidate version policy, prerequisites, and stack details
- Improve startup reliability guidance (`install` + module-level `spring-boot:run`)
- Add health check expected output examples
- Add Docker Compose documentation for local MySQL/Redis/RabbitMQ dependencies
- Document Docker startup, shutdown, access endpoints, and common port-conflict troubleshooting
- Move Docker credentials to `.env`-based configuration
- Add `.env.example` and document local environment initialization
- Add `application-dev.yml` for local MySQL/Redis/RabbitMQ integration settings
- Make active Spring profile configurable via `SPRING_PROFILES_ACTIVE` (default: `dev`)
- Update README with profile/configuration usage guidance
- Replace hardcoded `application-dev.yml` credentials with environment-variable overrides and local defaults
