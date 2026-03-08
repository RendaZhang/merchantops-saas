# MerchantOps SaaS

一个面向跨境卖家团队的多租户运营支持平台，提供工单处理、异步导入、账单追溯、功能开关、审计日志和基础工程化能力，模拟真实 SaaS 后端研发场景。

## Project Goals

- Build a realistic multi-tenant SaaS backend project
- Support resume/project portfolio presentation
- Be able to explain business design, technical architecture, and engineering decisions in interviews
- Be suitable for GitHub showcase

## Core Capabilities

- Multi-tenant isolation
- JWT + Spring Security + RBAC
- Ticket / task management
- CSV async import
- Redis cache
- MQ async processing
- Usage / ledger / invoice
- Feature flag / whitelist
- Docker / K8s / CI/CD
- Metrics / structured logging

## Planned Tech Stack

- Java 17
- Spring Boot 3.x
- MySQL 8
- Redis
- RabbitMQ
- Docker Compose
- Prometheus + Actuator

## Repository Structure

```text
merchantops-saas/
├── docs/
├── scripts/
├── sql/
├── deploy/
├── README.md
└── CHANGELOG.md
```

## Current Status

Day 1 - repository initialization in progress.