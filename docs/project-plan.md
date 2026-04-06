# MerchantOps SaaS（10 周）项目计划

> 一个面向跨境卖家团队的 AI-enhanced vertical SaaS 项目。  
> 目标不是只做一个能跑的 Demo，而是做出一个**符合 2025-2026 市场预期**、**可以支撑简历与面试表达**、并且**能展示 SaaS + AI 工作流整合能力**，还能按“作品集 -> 开源 -> 潜在商业化”路径推进的真实后端项目。
>
> 维护提示：本页只保留长期里程碑、阶段目标和版本节奏；当前实现状态、当前公开接口与当前限制统一以 [project-status.md](project-status.md) 为准，近期待办以 [roadmap.md](roadmap.md) 为准，避免把 slice 级现实状态和历史版本细节重复写入本页。

---

## 1. 项目定位

### 一句话定义

MerchantOps SaaS 是一个面向跨境卖家团队的多租户运营支持平台，核心价值不只是“存数据”，而是把**租户隔离、权限治理、运营工作流、异步处理、AI 辅助与 AI 审计**组合成一个可以落地的垂直应用。

### 为什么要改成 10 周计划

5 周计划适合做一个“像样的 SaaS 后端”，但不够支撑现在市场上更真实的要求。当前更有说服力的项目，不是单纯的 CRUD SaaS，也不是单纯的聊天机器人，而是：

- 有稳定的 system of record
- 有明确的 system of action
- AI 嵌入真实工作流，而不是独立悬空
- 关键操作有权限、审批、审计、评估与成本控制

这也是为什么计划需要从“5 周 SaaS 建设”升级成“10 周 workflow-first + AI-enhanced SaaS 建设”。

### 项目演进路径

- 第一步：做成能支撑简历、面试和技术表达的作品集项目
- 第二步：整理成可运行、可协作、可复用的开源 reference implementation
- 第三步：在真实 workflow、AI 与治理能力跑通后，再评估和公司合作或进一步商业化的机会

### 目标用户

- 跨境电商卖家团队
- 平台运营人员
- 商家管理员
- 内部支持 / 客服团队

### 要解决的真实问题

- 多租户团队在同一平台上的数据隔离与权限隔离
- 后台用户、角色、权限如何真正落地并可运营
- 工单流转、异步导入、异常处理等运营工作如何在线化
- AI 如何在真实工作流里提供价值，而不是只做聊天展示
- AI 输出如何被审计、评估、回放和受权限控制

---

## 2. 产品北极星

### 最终希望做成什么

不是“一个带 AI 的 SaaS”，而是一个更完整的三层结构：

1. **System of Record**
   - 租户、用户、角色、权限、工单、导入任务、操作记录
2. **System of Action**
   - 用户管理、工单流转、导入处理、审批与回滚
3. **System of Intelligence**
   - 工单 AI 摘要、分类、优先级建议、回复草稿
   - 导入错误归因、修复建议、字段映射建议
   - 低风险自动化与人机协作审批

### 项目完成后应该具备的辨识度

- 能证明你理解多租户 SaaS 的底层约束
- 能证明你不是只会做 CRUD，而是会做 workflow
- 能证明你知道 AI 应该嵌到哪里、不能嵌到哪里
- 能证明你理解 AI 项目的治理与交付，不只是模型调用
- 能证明这个项目有机会从作品集演进成公开开源项目，并保留后续商业化空间

---

## 3. 规划原则

### 核心原则

1. **先做可信系统，再做智能系统**
2. **先做有 ROI 的窄场景，再做 agent 化**
3. **AI 先做辅助，再做自动化**
4. **所有 AI 行为都要可审计、可评估、可回退**

### AI 整合原则

- AI 只能嵌入真实工作流，不单独做“聊天页”作为主卖点
- AI 输出默认是建议，不默认是执行
- 高风险动作必须 human-in-the-loop
- AI 输入范围必须受 tenant scope 和 RBAC 约束
- AI 能力要有 prompt/version/model/latency/cost 记录

### 这份计划明确不追求的东西

- 一开始就做大而全的 ERP
- 一开始就做全自动 agent
- 为了“看起来高级”而堆 RAG、向量库、Agent 框架
- 在业务闭环没站稳前，就做复杂前端和重度 K8s

---

## 4. 10 周整体推进节奏

### Week 1：Platform Foundation

做稳项目骨架、认证授权、上下文传递、统一异常、OpenAPI、基础可观测性。

### Week 2：First Business Loop - Tenant User Management

把用户管理做成第一个真实业务闭环，验证多租户、RBAC、Swagger、文档和回归流程能一起成立。

### Week 3：Ticket Workflow - System of Action

做工单主流程，让项目真正进入运营工作流场景。

### Week 4：Audit Trail and Approval Patterns

把审计日志、操作记录、审批与回退模式补齐，为后续 AI 落地准备治理骨架。

### Week 5：Async Import and Data Operations

做导入任务、异步处理、错误报告、幂等与重试，补齐后台重任务场景。

### Week 6：AI Copilot for Ticket Operations

把 AI 放进工单工作流，做摘要、分类、优先级建议、回复草稿和处理建议。

### Week 7：AI Copilot for Import and Data Quality

把 AI 放进导入与数据治理场景，做错误归因、字段映射建议、修复建议和聚类总结。

### Week 8：Agentic Workflows with Human Oversight

做低风险 agent 化操作，例如草稿生成、建议执行、审批后写回，不追求完全自动。

### Week 9：AI Governance, Eval, Cost, and Usage

把 AI 日志、评估集、回归测试、成本看板、调用限流和基础 usage 记录补齐。

### Week 10：Delivery Hardening and Portfolio Packaging

把 Feature Flag、部署、性能、故障演练、README、演示脚本和面试话术收口。

### 当前计划锚点

- 当前执行已经完成 Week 8 Agentic Workflows with Human Oversight。
- Week 1-8 当前已形成稳定 workflow + dual-workflow AI read baseline + human-reviewed execution bridge baseline，其中 Week 8 已完成 import selective replay proposal -> approval -> execution、ticket reply-draft comment proposal -> approval -> execution，以及共享 approval hardening。
- 当前 tagged baseline 已切到 `v0.6.0-beta`，对应 `Week 9 complete: AI Governance, Eval, Cost, and Usage beta baseline`。
- 当前执行已经把 Week 9 Slice A / B / C 收口成完成态的 governance / eval / cost / usage baseline。
- 当前近期待办已经从“完成 Week 9 tag 收口”切到“在 Week 10 中推进 delivery hardening、portfolio packaging 与更稳定的开源交付面”。
- 上一枚 beta tag 是 `v0.5.0-beta`，对应 `Week 8 complete: Agentic Workflows with Human Oversight beta baseline`。
- Week 10 Delivery Hardening and Portfolio Packaging 现在是 next active phase。
- 当前实现现实以 [project-status.md](project-status.md) 为准，当前近期待办和 slice 顺序以 [roadmap.md](roadmap.md) 为准。

---

## 5. 每周详细计划

### Week 1：Platform Foundation

### 目标

把系统底座做稳，让后续所有业务与 AI 能力都有可信赖的边界。

### 本周范围

- 多模块工程与仓库结构
- MySQL / Redis / RabbitMQ / Docker Compose
- 统一响应、全局异常、错误码、requestId、日志规范
- Swagger / SpringDoc OpenAPI
- 租户、用户、角色、权限、上下文基础模型
- JWT 登录、认证过滤器、RBAC 权限拦截

### 本周交付

- `/health`
- `/api/v1/auth/login`
- `/api/v1/user/me`
- `/api/v1/context`
- 基础 RBAC demo 接口
- 基础文档与 runbook

### 验收标准

- 不带 token 返回 `401`
- 越权访问返回 `403`
- 能稳定拿到 tenant/user context
- Swagger 可以完成基础联调

---

### Week 2：First Business Loop - Tenant User Management

### 目标

用用户管理作为第一个真实业务闭环，证明平台底座可承载业务写操作。

### 本周范围

- 分页查询用户列表
- 按用户名 / 状态筛选
- 查询单个用户详情
- 创建用户
- 更新用户基本信息
- 启用 / 禁用用户
- 给用户分配角色
- 所有读写都带租户隔离
- 所有写操作都受 RBAC 控制

### 关键要求

- repository 查询必须 tenant-aware
- 用户名唯一性按租户维度约束
- 禁止跨租户改用户和分配角色
- 写接口建议最少要求 `USER_WRITE`
- 角色分配独立成接口，不和基本信息更新耦合
- Swagger 中暴露的接口必须同步示例、runbook、`api-demo.http`

### 推荐交付接口

- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `PATCH /api/v1/users/{id}/status`
- `PUT /api/v1/users/{id}/roles`

### 验收标准

- 分页、筛选、详情都只返回当前租户数据
- `viewer` 无法做写操作
- `admin` 可以完成完整链路
- 文档、Swagger、HTTP 示例保持一致

---

### Week 3：Ticket Workflow - System of Action

### 目标

进入真正的运营工作流场景，做出比 CRUD 更像真实 SaaS 的模块。

### 本周范围

- 设计 `ticket`、`ticket_comment`、`ticket_operation_log`
- 创建工单
- 查询详情
- 分页列表
- 指派处理人
- 状态流转
- 评论能力

### 关键要求

- `assignee` 必须属于当前租户
- 状态机规则不能散落在 controller
- 关键字段要考虑 AI 后续接入的上下文价值
- 工单动作需要稳定的审计事件模型

### 验收标准

- 能完整创建、指派、评论、关闭一张工单
- 非法状态流转会被拒绝
- 所有动作都可追溯到 tenantId / userId / requestId

---

### Week 4：Audit Trail and Approval Patterns

### 目标

为 AI 与自动化建立治理骨架，而不是事后补治理。

### 本周范围

- 审计日志模型
- 关键动作事件记录
- 审批 / 拒绝 / 回退的基础模式
- 操作日志查询接口
- AI 未来要复用的 action envelope 设计

### 建议沉淀的能力

- `actionType`
- `entityType`
- `entityId`
- `beforeValue` / `afterValue`
- `operatorId`
- `approvalStatus`
- `requestId`

### 验收标准

- 用户写操作、工单关键流转都能记录审计事件
- 至少有一个“建议 -> 审批 -> 执行”的基础模式落地

---

### Week 5：Async Import and Data Operations

### 目标

补齐后台系统最常见的重任务场景，为后续 AI 导入能力打地基。

### 本周范围

- `import_job`
- `import_job_item_error`
- 上传文件并创建 job
- MQ 投递与 worker 消费
- 失败行 replay、全失败 source job 的 whole-file replay、按精确 errorCode selective replay、edited failed-row replay / derived-job lineage
- 顺序 chunk 分片处理与处理中计数回写
- 失败明细与错误报告
- 吞吐控制与大文件硬性上限
- 幂等控制、重试与更复杂并发限制

### 关键要求

- 导入接口要快速返回 `jobId`
- job 状态机明确
- 错误明细可供 AI 二次分析
- 所有导入任务带 tenant scope 和 operator 信息

### 验收标准

- 上传后立刻返回
- 后台任务独立推进
- 可查看进度、失败原因和错误报告
- 可基于失败行创建新的派生 replay job，并为 `FAILED` 且零成功 source job 提供 whole-file replay，同时支持按精确 errorCode 做 selective replay 以及按精确 errorId 做 edited failed-row replay，而不重置原 job
- 顺序 chunk 执行不会退回到整文件长事务

---

### Week 6：AI Copilot for Ticket Operations

### 目标

把 AI 直接嵌入工单工作流，而不是额外做一个无业务上下文的对话入口。

### 推荐 AI 用例

- 工单摘要
- 工单分类建议
- 优先级建议
- 分配建议
- 回复草稿生成
- 操作记录总结

### 关键要求

- AI 输入范围必须 tenant-scoped
- 输出默认是建议，不是直接写库
- 必须记录 prompt version、model、latency、cost、approval result
- 需要至少一个人工确认步骤

### 推荐交付接口

- `POST /api/v1/tickets/{ticketId}/ai-summary`
- `POST /api/v1/tickets/{ticketId}/ai-triage`
- `POST /api/v1/tickets/{ticketId}/ai-reply-draft`

### 验收标准

- AI 输出能提升工单处理效率
- 人工可以拒绝或修改 AI 建议
- AI 结果有审计与回放信息

---

### Week 7：AI Copilot for Import and Data Quality

### 目标

把 AI 放到导入与数据治理这种更有现实 ROI 的场景。

### 推荐 AI 用例

- 导入错误原因聚类
- 字段映射建议
- 数据异常总结
- 修复建议生成
- 失败样本摘要

### 关键要求

- 只让 AI 读被授权的导入任务数据
- AI 不能直接修改源数据
- 修复建议要和错误明细关联
- 输出最好能被人工一键采用或拒绝

### 推荐交付接口

- `POST /api/v1/import-jobs/{jobId}/ai-error-summary`
- `POST /api/v1/import-jobs/{jobId}/ai-mapping-suggestion`
- `POST /api/v1/import-jobs/{jobId}/ai-fix-recommendation`

### 验收标准

- AI 能减少人工排查成本
- 输出不是纯文本展示，而是服务于后续修复动作

---

### Week 8：Agentic Workflows with Human Oversight

### 目标

把 AI 从“建议器”推进到“低风险代理”，但保留清晰的权限与审批边界。

### 适合做的 agent 场景

- 生成工单回复草稿并提交审批
- 生成批量错误归因报告
- 根据规则 + LLM 生成处理建议清单
- 对低风险任务发起预执行计划

### 不建议现在做的事情

- 全自动改用户权限
- 全自动关闭工单
- 全自动修复导入数据并落库
- 跨租户上下文推理

### 核心交付

- tool calling 或等价执行层
- action proposal / approval / execution 三段式链路
- 角色与权限校验
- 执行失败可回退

当前已落地 slices：

- import selective replay proposal and approval flow，使用现有 approval 基线复用现有 selective replay 执行链路，把 import AI fix recommendation 的人工判断自然推进到 human-reviewed execution
- ticket reply-draft comment proposal and approval flow，使用独立 workflow endpoint 复用现有 ticket comment 写链路，把 ticket AI internal reply draft 的人工判断推进到 human-reviewed execution，同时保持公开 AI endpoint 本身仍然 suggestion-only

### 验收标准

- 至少一个 agent workflow 能稳定跑通
- agent 不越权
- 审批链和执行链都可审计

---

### Week 9：AI Governance, Eval, Cost, and Usage

### 目标

把 AI 项目最容易被忽略、但最能体现成熟度的部分补齐。

### 本周范围

- prompt version 管理
- golden set / eval dataset
- 基础 AI 回归评估
- latency / error / cost / token 统计
- AI usage event 记录
- AI 调用限流与失败降级

### 推荐交付物

- `docs/ai/` 目录下的 AI 设计说明
- 一组离线评估样本
- 一份 prompt / model 变更回归 checklist
- AI 请求审计日志与 usage 记录

### 验收标准

- 改 prompt / model 后有最小回归验证
- 能回答“这个 AI 功能是否变好了”
- 能回答“这个 AI 功能花了多少钱”

---

### Week 10：Delivery Hardening and Portfolio Packaging

### 目标

把项目从“工程练习”收口成“市场上能讲得通的作品”。

### 本周范围

- Feature Flag 控制 AI 能力灰度
- Dockerfile 与交付文档
- 最小 CI/CD
- 压测与性能优化
- 故障演练
- LICENSE、CONTRIBUTING、演示数据清理与开源发布准备
- README、架构图、时序图、演示脚本
- 面试版话术与 Demo 路线

### 需要重点准备的故事线

- 为什么这个项目不是普通 CRUD SaaS
- 为什么 AI 放在工单与导入场景最合理
- 为什么要做 human-in-the-loop
- 为什么要做 eval、cost、audit，而不是只接一个模型接口

### 验收标准

- 项目能被别人跑起来
- 项目能被别人理解
- 项目能被别人演示
- 项目能支撑现实面试问题

---

## 6. 跨周 AI 交付要求

这些要求不是某一周单独完成，而是 Week 6 之后持续生效。

### 安全与权限

- AI 读写范围必须受 tenantId 和 RBAC 限制
- AI 工具调用必须校验当前操作者权限
- 不允许跨租户拼接上下文

### 审计与合规

- 记录输入范围、prompt 版本、model、输出摘要、审批结果
- 高风险动作必须可追溯到操作者

### 质量与评估

- 至少准备一套 golden set
- 至少准备一套失败样本集
- 每次提示词或模型变更都应做最小回归

### 成本与可用性

- 记录 token、耗时、失败率
- 对 AI 调用设置降级路径
- 对关键 AI 功能设置 Feature Flag

---

## 7. 里程碑与成功标准

### Week 2 结束时

- 你有一个可信的 tenant-scoped 用户管理闭环

### Week 5 结束时

- 你有一个可信的运营工作流和异步处理链路
- 项目适合整理成下一阶段开源预览版，例如 `v0.2.0-alpha`

### Week 6 结束时

- 你有一个可信的 ticket AI Copilot baseline，包括 interaction history、摘要、分类/优先级建议和内部回复草稿
- 项目适合整理成第一条更正式的 AI-enhanced 开源发布，例如 `v0.3.0-beta`

### Week 7 结束时

- 你有 ticket workflow 和 import/data-quality 两个真实工作流里的 AI Copilot
- 项目适合把 AI-enhanced vertical SaaS 的公开叙事从 ticket baseline 扩到双场景 workflow baseline

### Week 8 结束时

- 你有两个真实工作流里的 AI Copilot，以及至少两个低风险、human-reviewed 的 agent approval flows
- 当前这一步还包含共享 approval hardening：pending proposal dedupe、resolved-request reproposal、以及 action-aware approval baseline 的收口

### Week 10 结束时

- 你有一个可以拿去讲“AI-enhanced vertical SaaS”的完整作品
- 项目适合作为稳定开源参考实现对外展示，并开始验证潜在商业合作机会

---

## 8. 开源与商业化路径

### 开源定位

更合适的定位不是“完整商用成品”，而是：

- 一个 workflow-first 的多租户 SaaS reference implementation
- 一个带 AI Copilot 与治理约束的 vertical SaaS 样例项目
- 一个可支撑作品集、开源协作和后续产品探索的工程基础

### 推荐发布时间点

- 当前 tagged baseline：`v0.6.0-beta`，对应 `Week 9 complete: AI Governance, Eval, Cost, and Usage beta baseline`
- 上一枚 beta tag：`v0.5.0-beta`，对应 `Week 8 complete: Agentic Workflows with Human Oversight beta baseline`
- 详细版本历史以 `CHANGELOG.md` 和 `docs/contributing/release-versioning.md` 为准
- Week 9 已完成：公开叙事已经从 human-reviewed workflow baseline 明确推进到更可信的 governance / eval / cost / usage beta baseline
- Week 10 后：更稳定的开源参考实现，可作为商业探索起点

### 开源前最低准备项

- 明确 License 选择
- 补齐 CONTRIBUTING、最小安全说明和本地运行文档
- 确保 demo 数据、账号和配置说明可公开
- AI provider 配置保持可插拔，不与单一厂商强绑定
- 任何模型密钥、测试密钥和私有数据都不能进仓库

### 商业化判断方式

只有当下面几项同时成立时，才值得认真评估商业合作：

- 至少一个 workflow 模块被证明有实际效率收益
- AI 不是独立聊天功能，而是真的缩短处理链路
- 审计、权限、审批、成本和回归机制已经站稳
- 开源版本已经能说明项目的工程可信度与复用价值

---

## 9. 最终交付物

### 业务与系统能力

- 多租户与 RBAC
- 用户管理闭环
- 工单工作流
- 异步导入与错误治理
- 工单 AI Copilot
- 导入 AI Copilot
- 有审批边界的低风险 agent workflow

### 工程与治理能力

- Swagger / OpenAPI
- runbook / regression checklist
- 审计日志
- AI eval 与 usage 记录
- 部署与灰度控制
- 开源发布准备材料
- 压测 / 故障演练

### 面试与作品能力

- README 完整版
- 架构图 / 时序图 / ER 图
- 演示脚本
- 面试版项目讲稿
- 面向开源发布的定位说明与版本说明

---

## 10. 风险控制建议

### 容易失控的地方

- Week 2 就把用户管理做成超大后台
- Week 3-5 一次性铺太多模块
- Week 6 以后把 AI 做成通用聊天系统
- Week 8 过早追求 fully autonomous agent
- Week 9 才想起来补 audit 和 eval

### 控制方式

- 用户管理先做最小闭环，再扩展
- 工单和导入要优先于 billing
- AI 先做窄场景、强约束、强回报
- agent 只做低风险、可审批动作
- governance 不能拖到最后才补

---

## 11. 建议的文档分工

- `README.md`：项目总入口与能力概览
- `docs/project-plan.md`：10 周总体路线图
- `docs/project-status.md`：当前真实进展
- `docs/roadmap.md`：下一阶段工作安排
- `docs/reference/`：接口、认证、配置、AI 能力说明
- `docs/runbooks/`：验证、回归、故障处理
- `docs/architecture/`：ADR、架构图、AI 治理设计

---

## 12. 一句话总结

这份 10 周计划的核心，不是“把 SaaS 拉长一点做”，而是把项目重构成：

**一个以真实工作流为中心、以租户与权限治理为底座、以 AI Copilot 与受控 agent 为增强层的 vertical SaaS 项目。**
