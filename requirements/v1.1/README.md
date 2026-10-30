# CampaignLab V1.1 需求包：用户日触达频控

- 需求编号：`CL-V1.1-FC`
- 状态：已确认需求基线，尚未实现
- 基线日期：2026-10-28
- 产品范围：CampaignLab 合成营销流程平台

## 需求包目的

本需求包供产品、开发、测试和 QualityPilot 共同使用，描述 V1.1 新增的“单计划用户日触达频控”。它只固定正确需求和验收标准，不代表代码、数据库、前端或 OpenAPI 已经具备该能力。

## 文件索引

| 文件 | 作用 |
|---|---|
| [`PRD.md`](PRD.md) | 需求背景、用户故事、功能范围和主流程 |
| [`BUSINESS_RULES.md`](BUSINESS_RULES.md) | 可执行的业务规则与明确边界 |
| [`CHANGE_IMPACT.md`](CHANGE_IMPACT.md) | 对页面、API、数据、执行和监控的预计影响 |
| [`ACCEPTANCE_CRITERIA.md`](ACCEPTANCE_CRITERIA.md) | Given/When/Then 验收标准和可观察结果 |
| [`TEST_DATA.md`](TEST_DATA.md) | 固定用户、计划、时间和场景数据 |

## 与 V1.0 的关系

当前代码和 [`../../openapi/openapi.json`](../../openapi/openapi.json) 是已实现事实：V1.0 还没有触达频控配置与执行拦截。早期 `docs/PRODUCT_SPEC_V1.md` 和 `docs/BUSINESS_RULES.md` 曾把频控写入目标蓝图，但这些目标没有在当前运行代码中实现；本需求包将其正式收敛为 V1.1 新需求。

V1.1 实现完成前：

- 不修改 V1.0 OpenAPI；
- 不声称页面或执行引擎已经支持频控；
- 不把任何受控缺陷答案写入本需求包；
- QualityPilot 可以使用本文分析需求，但不能执行尚不存在的 V1.1 接口字段。

## 核心定义

```text
频控统计键 = marketing_plan + user + Asia/Shanghai自然日
计数事件   = 实际发起一次模拟渠道调用
拦截证据   = node_execution.result = FREQUENCY_CAPPED
禁止结果   = 被拦截时仍新增 channel_receipt
```

## 完成定义

只有在代码、Flyway、页面、OpenAPI、自动化测试和真实 MySQL 验证全部完成后，才能把本需求状态改为“已实现”。
