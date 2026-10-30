# V1.1 变更影响分析

> 本文描述预计改动，不代表已实现。真实完成状态必须由代码、迁移和测试结果证明。

## 1. 页面

营销计划创建区域预计新增：

```text
每日触达上限：[1] 次/用户/计划/上海自然日
```

- 输入控件只允许1～5的整数；
- 计划列表或详情需要展示该值；
- 执行轨迹需要识别 `FREQUENCY_CAPPED`。

## 2. API与OpenAPI

预计在以下请求和响应增加 `dailyContactLimit`：

- `POST /api/marketing-plans` 请求体；
- 营销计划创建响应；
- `GET /api/marketing-plans` 响应；
- `GET /api/marketing-plans/{id}` 响应。

字段尚未实现，因此当前 V1.0 OpenAPI 不应提前增加。代码完成后必须同步更新 OpenAPI，并通过路由与Schema合同测试。

预计新增错误语义：

- 缺失、非整数、0或大于5：400；
- 错误码是否复用 `INVALID_PLAN`，在实现评审时确认，需求不新增无必要的错误码。

## 3. 数据库

预计新增 Flyway V4：

```text
marketing_plan.daily_contact_limit INT NOT NULL DEFAULT 1
```

V1.1不新增频控计数表。已用次数从以下现有事实表计算：

```text
campaign_run.plan_id
→ channel_receipt.run_id
+ channel_receipt.user_id
+ channel_receipt.created_at
```

需要关注索引与查询成本，但本地合成数据阶段不引入缓存。

## 4. 后端职责

| 组件 | 预计职责变化 |
|---|---|
| `MarketingPlanController` | 接收并返回 `dailyContactLimit` |
| `MarketingPlanService` | 校验1～5并持久化 |
| `MarketingPlan` | 保存计划级日上限 |
| `ExecutionService` | 营销节点前按计划、用户、上海自然日跨批次统计 |
| Repository | 提供跨 `campaign_run` 与 `channel_receipt` 的计数查询 |
| `NodeExecution` | 复用 `result` 保存 `FREQUENCY_CAPPED` |

建议为时间边界注入 `Clock`，避免自动化测试依赖机器当前时间。

## 5. 数据与状态

- `channel_receipt`：只有真实渠道调用才新增；频控拦截不写。
- `node_execution`：频控拦截仍写一条事件，结果为 `FREQUENCY_CAPPED`。
- `user_journey`：频控本身不终止旅程，沿现有图继续。
- `campaign_run`：批次级状态规则不因单个用户频控而新增状态。

## 6. 回归范围

- 客群创建、发布、成员快照不应变化；
- 计划图发布校验继续生效；
- A/B与标签判断不应因频控改变；
- 未达到上限时模拟渠道结果与原逻辑一致；
- 回执判断需要新增频控后的`FAILURE`路径验证；
- 遗留`campaign/claim`权益领取不在本需求影响范围内；
- V1.0全部自动化测试必须继续通过。

## 7. 风险目录

| 风险 | 典型后果 |
|---|---|
| 只统计当前运行批次 | 重复启动同一计划可绕过频控 |
| 只统计成功回执 | 失败调用后可无限重试 |
| 使用UTC自然日 | 上海00:00附近错误拦截或放行 |
| 使用过去24小时 | 与自然日业务口径不一致 |
| 频控后仍写渠道回执 | 数据显示已发生未授权调用 |
| 不区分计划 | 一个计划错误占用另一个计划额度 |
| 不区分用户 | 某用户触达影响其他用户 |
| 先调用渠道再判断 | 轨迹显示拦截但实际已经触达 |
