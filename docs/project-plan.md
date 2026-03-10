# MerchantOps SaaS（5 周）项目计划

> 面向跨境卖家团队的多租户运营支持平台  
> 目标不是只做一个能跑的 Demo，而是做出一个**像真实 SaaS 后端项目**、**能支撑简历经历**、**适合放 GitHub / 作品集**、并且**可以在面试中完整讲清业务、技术、稳定性与工程化**的成品项目。

---

## 目录

- [1. 项目定位](#1-项目定位)
- [2. 项目目标](#2-项目目标)
- [3. 技术栈建议](#3-技术栈建议)
- [4. 推荐仓库结构](#4-推荐仓库结构)
- [5. 5 周整体推进节奏](#5-5-周整体推进节奏)
- [6. 第 1 周计划：平台底座](#6-第-1-周计划平台底座)
- [7. 第 2 周计划：第一个真实业务闭环租户用户管理](#7-第-2-周计划第一个真实业务闭环租户用户管理)
- [8. 第 3 周计划：工单与审计](#8-第-3-周计划工单与审计)
- [9. 第 4 周计划：异步导入与后台处理](#9-第-4-周计划异步导入与后台处理)
- [10. 第 5 周计划：SaaS 差异化能力与工程化收口](#10-第-5-周计划saas-差异化能力与工程化收口)
- [11. 5 周后的完成状态](#11-5-周后的完成状态)
- [12. 迭代原则](#12-迭代原则)
- [13. 每周里程碑](#13-每周里程碑)
- [14. 风险控制建议](#14-风险控制建议)
- [15. 执行优先级分层](#15-执行优先级分层)
- [16. 每周执行建议](#16-每周执行建议)

---

## 1. 项目定位

### 项目名称

**MerchantOps SaaS**

### 项目一句话介绍

一个面向跨境卖家团队的多租户运营支持平台，提供租户用户管理、工单处理、异步导入、账单追溯、功能开关、审计日志和基础工程化能力，模拟真实 SaaS 后端研发场景。

### 目标用户

- 跨境电商卖家团队
- 平台运营人员
- 商家管理员
- 内部支持 / 客服团队

### 项目要解决的问题

- 多租户团队在同一平台上的数据隔离与权限隔离
- 用户、角色、权限等后台管理能力如何落地
- 运营工单与任务的统一管理
- 大批量数据导入与异步处理
- 租户维度的用量统计、账单生成与对账追溯
- 某租户功能灰度、白名单与快速回滚
- 后端系统的基础可观测性、稳定性与部署流程

---

## 2. 项目目标

### 核心目标

这个项目最终要满足以下四个方向：

1. **像真实 SaaS 后端项目**
2. **能支撑简历里相关 SaaS 经验的表达**
3. **能在面试中完整讲清楚业务与技术细节**
4. **适合放 GitHub / 作品集展示**

### 核心能力

- 多租户隔离
- JWT / Spring Security / RBAC
- 租户用户管理闭环
- 工单 / 运营任务管理
- CSV 异步导入
- Redis 缓存
- MQ 异步处理
- 账单 / usage / ledger
- Feature Flag / 白名单
- Docker / K8s / CI/CD
- Prometheus Metrics / 结构化日志
- 压测 / 故障演练 / 轻量 AI 增强

### 最终交付物

- 完整后端项目代码
- 可运行的本地开发环境
- 完整 README
- SQL 初始化脚本
- 架构图 / ER 图 / 时序图
- 压测报告
- 故障演练文档
- 面试版项目讲稿

---

## 3. 技术栈建议

### 后端

- Java 17+
- Spring Boot 3.x
- Spring Web
- Spring Security
- Spring Validation
- Spring Data JPA / MyBatis（二选一，建议按你熟悉的技术栈）

### 数据与中间件

- MySQL 8
- Redis
- RabbitMQ

### 工程化

- Docker / Docker Compose
- GitHub Actions 或 Jenkins
- K8s（Minikube / Kind / 轻量云环境）
- Prometheus + Actuator
- Grafana（可选加分）

### 文档与协作

- Markdown
- Mermaid
- Swagger / SpringDoc OpenAPI
- Postman / Bruno / Apifox（任选其一）

---

## 4. 推荐仓库结构

```text
merchantops-saas/
├── merchantops-api/
├── merchantops-domain/
├── merchantops-infra/
├── merchantops-common/
├── docs/
│   ├── architecture/
│   ├── runbooks/
│   ├── incidents/
│   ├── performance/
│   └── diagrams/
├── scripts/
├── sql/
├── deploy/
│   ├── docker/
│   └── k8s/
├── .github/workflows/
├── docker-compose.yml
├── README.md
└── CHANGELOG.md
```

### 模块职责建议

- `merchantops-api`：Controller、DTO、鉴权入口、接口层
- `merchantops-domain`：核心业务模型、服务、规则、状态机
- `merchantops-infra`：数据库、Redis、MQ、外部依赖适配
- `merchantops-common`：通用工具类、异常码、响应对象、上下文与常量
- `docs/`：架构说明、故障案例、压测报告、运行手册、计划文档
- `scripts/`：启动脚本、初始化脚本、压测辅助脚本
- `sql/`：建表 SQL、初始化数据脚本
- `deploy/`：Docker 与 K8s 部署文件

---

## 5. 5 周整体推进节奏

### Week 1：Platform Foundation

做出项目骨架、租户体系、鉴权权限、API 统一规范和 OpenAPI 基础能力。

### Week 2：First Business Loop - Tenant User Management

把用户管理做成第一个真实业务闭环，验证多租户、RBAC、Swagger、回归文档是否能一起成立。

### Week 3：Ticket Workflow and Audit Trail

做工单领域模型、状态流转和审计记录，让项目开始具备真实运营后台味道。

### Week 4：Async Import and Background Processing

做 CSV 导入、任务状态机、MQ 异步消费和错误报告，补齐后台系统常见重活场景。

### Week 5：SaaS Differentiators and Delivery Hardening

按优先级补 Feature Flag、Metrics、部署交付、压测、故障演练、作品化文档，并视时间引入 usage / ledger / invoice。

### 当前现实推进建议

- Week 1 已完成
- Week 2 正在进行或应作为当前主线推进
- Week 3 到 Week 5 应建立在 Week 2 这个业务闭环已经站稳的前提上

---

## 6. 第 1 周计划：平台底座

> 目标：**先把系统底座做稳，让后续业务开发不建立在沙地上**

### 本周范围

- 初始化多模块工程与仓库结构
- 接入 MySQL / Redis / RabbitMQ / Docker Compose
- 建立统一响应、全局异常、错误码、requestId、日志规范
- 接入 Swagger / SpringDoc OpenAPI
- 建立租户、用户、角色、权限的核心表结构和 seed data
- 完成 JWT 登录、认证过滤器、上下文传递、RBAC 权限拦截

### 本周交付

- `/health`、`/api/v1/auth/login`、`/api/v1/user/me`、`/api/v1/context`
- Swagger 可访问且可调试
- Demo 租户、用户、角色、权限数据
- `@RequirePermission` 或等价机制
- README 和基础架构文档初版

### 验收标准

- 不带 token 返回 `401`
- 越权访问返回 `403`
- 认证后能拿到当前租户和用户上下文
- Swagger / OpenAPI 可作为联调入口
- 核心基础能力可通过 runbook 手工验证

### 面试可讲点

- 多租户上下文怎么传递
- JWT + Spring Security 怎么落地
- RBAC 为什么放在接口层拦截
- 为什么先做 Swagger、统一异常、requestId

---

## 7. 第 2 周计划：第一个真实业务闭环（租户用户管理）

> 目标：**用用户管理作为第一个真实业务闭环，验证平台底座真的能支撑业务**

### 本周范围

- 分页查询用户列表
- 按用户名 / 状态筛选
- 查询单个用户详情
- 创建用户
- 更新用户基本信息
- 启用 / 禁用用户
- 给用户分配角色
- 所有操作都带租户隔离
- 所有写操作都受 RBAC 控制

### 建议推进顺序

1. 先补齐只读能力：
   - `GET /api/v1/users`
   - 支持 `page`、`size`、`username`、`status`
   - 排序字段白名单
2. 再补齐单体查询：
   - `GET /api/v1/users/{userId}`
   - 严格 tenant-scoped 查询
3. 再做写接口：
   - `POST /api/v1/users`
   - `PUT /api/v1/users/{userId}`
   - `PATCH /api/v1/users/{userId}/status`
   - `PUT /api/v1/users/{userId}/roles`
4. 最后补验收与文档：
   - Swagger 示例
   - `api-demo.http`
   - 回归清单
   - project-status / roadmap / reference 文档

### 关键设计要求

- 所有 repository 查询都必须带 `tenantId`
- 用户名唯一性按租户维度约束
- 禁止跨租户修改用户和角色分配
- 所有写操作至少要求 `USER_WRITE`
- 角色分配建议拆成独立接口，避免和基本信息更新耦合
- 禁用用户后要明确其登录与已有 token 的预期行为
- Swagger 中公开的接口必须有示例，请求路径使用真实路径，例如 `/api/v1/users`

### 本周交付

- 用户管理的读写接口初版
- Swagger 中可见的用户管理接口分组
- `api-demo.http` 中可直接验证的用户管理请求
- 用户管理参考文档与回归检查项

### 验收标准

- 分页、筛选、详情都只返回当前租户数据
- 创建 / 更新 / 状态变更 / 角色分配都带权限校验
- `viewer` 不可执行写操作
- `admin` 可执行完整用户管理链路
- Swagger 示例和实际返回保持一致

### 面试可讲点

- 为什么用户管理适合作为第一个业务闭环
- 租户隔离在 repository、service、controller 三层怎么兜底
- 为什么角色分配要单独建接口
- Swagger 和回归清单为什么要和业务闭环一起交付

---

## 8. 第 3 周计划：工单与审计

> 目标：**在已有用户体系之上，做出更像真实运营后台的核心业务模块**

### 本周范围

- 设计并实现 `ticket`、`ticket_comment`、`ticket_operation_log`
- 创建工单、查询详情、分页列表
- 工单指派、状态流转、评论
- 审计日志记录关键操作
- 为工单接口补参数校验与业务校验分层

### 关键细节

- `assignee_id` 必须属于当前租户
- 状态机规则要显式建模，禁止非法状态流转
- 审计日志至少记录 `requestId`、`tenantId`、`userId`
- 评论、操作记录、状态流转都要可追溯

### 本周交付

- 工单主流程接口
- 工单状态流转和操作记录
- 审计日志模型与基础查询能力
- 工单相关文档、时序图或状态说明

### 验收标准

- 能完整创建并推进一张工单
- 非法状态流转会被拒绝
- 审计记录能追溯是谁在什么租户下做了什么操作

### 面试可讲点

- 参数校验与业务校验如何分层
- 工单状态机为什么不能散落在 controller 里
- 审计日志如何为排障和合规服务

---

## 9. 第 4 周计划：异步导入与后台处理

> 目标：**补齐 SaaS 后台最常见的重任务场景：导入、异步、失败追踪**

### 本周范围

- 设计并实现 `import_job`、`import_job_item_error`
- 上传文件后创建 job，并立即返回 `jobId`
- MQ 投递与 worker 消费
- chunk 分片处理与失败记录
- 进度查询、重试、幂等控制、错误报告导出

### 关键细节

- 每批 chunk 大小要可配置
- 单租户并发导入数需要限制
- 失败明细保留上限要明确
- job 状态机建议至少包含：
  - `PENDING`
  - `RUNNING`
  - `PARTIAL_FAILED`
  - `SUCCEEDED`
  - `FAILED`

### 本周交付

- 导入任务接口与异步消费链路
- import job 状态查询
- 错误报告或错误明细查看能力
- 导入流程文档和时序图

### 验收标准

- 上传后请求可快速返回
- 后台任务能独立推进
- 能明确看到任务进度和失败原因

### 面试可讲点

- 为什么不能同步导入 20 万条
- 为什么要 chunk + MQ
- 导入失败如何做到可定位、可重试

---

## 10. 第 5 周计划：SaaS 差异化能力与工程化收口

> 目标：**把项目从“有模块”推进到“像交付过的 SaaS 系统”**

### 必做范围

- Feature Flag / 白名单能力
- Metrics 与结构化日志增强
- Dockerfile 与基础容器化交付
- CI/CD 基础流水线
- README、架构图、运行手册、回归文档、故障演练文档收口

### 应做范围

- K8s 最小部署样例
- 压测与一轮性能优化
- 发布 / 回滚 Runbook

### 视时间推进

- usage / ledger / invoice 最小闭环
- Grafana 展示
- 轻量 AI 增强能力

### 关键细节

- Feature Flag 禁止写死 `if tenantId == xxx`
- Metrics 至少覆盖 HTTP、导入任务、MQ 消费失败
- 日志字段至少包括 `requestId`、`tenantId`、`userId`
- 部署文档必须能支持本地或最小测试环境复现

### 本周交付

- 一套最小可交付工程化能力
- 故障案例 / 性能说明 / 发布说明
- README 完整版
- 面试版讲稿素材

### 验收标准

- 项目能以文档驱动方式被别人跑起来
- 至少有一个明确的性能优化案例
- 至少有一个明确的故障排查案例
- 代码、文档、Swagger、runbook 之间没有明显冲突

### 面试可讲点

- 为什么 Feature Flag 是 SaaS 差异化能力
- 为什么 Metrics、日志、Runbook 也算项目核心交付
- 如何把一个项目从“能跑”做成“能讲、能演示、能维护”

---

## 11. 5 周后的完成状态

### 业务层面

- 有真实客户画像
- 有租户用户管理闭环
- 有工单、导入、功能开关等真实 SaaS 场景

### 技术层面

- Spring Boot + MySQL + Redis + MQ
- JWT + Spring Security + RBAC
- 多租户隔离
- 用户管理、工单、导入等业务模块
- OpenAPI / Swagger 联调能力
- Metrics / 日志 / Docker / CI/CD 基础能力

### 面试层面

你可以用这个项目回答以下问题：

- 你做过什么业务系统？
- 多租户怎么做？
- 用户、角色、权限如何在 SaaS 里落地？
- 大批量导入怎么处理？
- 工单状态流转怎么设计？
- 某客户要单独开功能怎么办？
- 日志、监控、排障怎么做？
- 为什么你说自己做过 SaaS 后端？

---

## 12. 迭代原则

这 5 周里，不要一开始就追求“全都最好”。

按以下顺序推进最稳：

1. **先跑通**
2. **再做第一个真实业务闭环**
3. **再扩展更复杂业务模块**
4. **最后补工程化、作品化和面试化**

---

## 13. 每周里程碑

### 第 1 周末

系统能登录、能鉴权、能隔离租户、能通过 Swagger 联调基础接口。

### 第 2 周末

有完整租户用户管理闭环，能证明平台底座已经可承载真实业务。

### 第 3 周末

有工单主流程和审计记录。

### 第 4 周末

有异步导入和后台处理链路。

### 第 5 周末

有 Feature Flag、基础可观测性、交付文档和作品化材料。

---

## 14. 风险控制建议

### 容易超时的地方

- 在 Week 2 就把用户管理做成“超完整后台”
- 工单状态机做得过度复杂
- 导入模块一次性追求超大吞吐
- K8s 花太多时间
- usage / ledger / invoice 做得太深
- AI 功能喧宾夺主

### 建议控制方式

- Week 2 先做用户管理最小闭环，不先追求组织架构、批量导入用户等扩展能力
- 工单先做核心状态流转，不上来就做复杂 SLA
- 导入先证明异步架构正确，再追求极限性能
- K8s 只做最小可演示版本
- 账单先做最小模型，作为 Week 5 视时间推进项
- AI 只做 1 个小功能

---

## 15. 执行优先级分层

### 必做

- 多租户
- 鉴权权限
- 用户管理闭环
- 工单
- 导入
- Feature Flag
- README / docs / runbooks

### 应做

- Docker
- CI/CD
- Metrics
- 审计日志
- 压测

### 加分

- K8s
- usage / ledger / invoice
- 故障演练
- AI 小功能
- Grafana 展示

---

## 16. 每周执行建议

### 每周固定动作

- 先明确本周唯一主目标
- 开发前写本周接口与文档清单
- 开发中同步维护 Swagger 示例和 `api-demo.http`
- 开发后补最小可用文档
- 周末更新 `project-status`、`roadmap`、README

### 每周建议产物

- 代码提交
- 一组可验证接口
- 一段对应文档
- 一份回归或 smoke test 记录
- 一张图或一段时序说明

### 周末收口动作

- 更新 README
- 整理 `docs/`
- 复盘本周完成 / 未完成项
- 把本周内容转成“可面试表达”的话术

---

## 附：这份计划的使用方式

建议把这份文件放进仓库，并作为 README 之后的详细规划入口，例如：

```text
docs/project-plan.md
```

同时建议保持以下分工：

- `README.md`：面向面试官和仓库访客的总入口
- `docs/project-plan.md`：5 周整体推进计划
- `docs/project-status.md`：当前真实进展与已完成范围
- `docs/roadmap.md`：下一阶段计划与近期待办
- `docs/architecture/`：架构设计与 ADR

这样项目会更像一个真正交付过、维护过、可讲可演示的 SaaS 后端系统。
