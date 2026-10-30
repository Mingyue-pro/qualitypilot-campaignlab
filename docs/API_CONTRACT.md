# CampaignLab V1.0 HTTP API 合同

机器可读的正式合同是 [`../openapi/openapi.json`](../openapi/openapi.json)，采用 OpenAPI 3.1.0。

本文只提供给人快速阅读的索引。接口字段、枚举、必填项、请求体、响应体和错误响应应以 OpenAPI 文件与当前代码共同为准。合同测试会比较 OpenAPI 操作与 Spring 实际注册的 `/api/**` 路由，防止接口增删后文档静默漂移。

## CDP 客群

| 方法 | 路径 | 角色 | 当前行为 |
|---|---|---|---|
| `POST` | `/api/audiences/preview` | operator | 使用当前合成画像预览匹配结果，不保存 |
| `POST` | `/api/audiences` | operator | 重新计算并创建客群、V1 规则、R1 成员快照 |
| `GET` | `/api/audiences` | any | 查询所有客群的最新规则版本与计算批次 |
| `POST` | `/api/audiences/{id}/refresh` | operator | 动态客群规则不变并产生下一个 R 批次；静态客群拒绝刷新 |
| `POST` | `/api/audiences/{id}/versions/{version}/publish` | operator | 发布指定客群规则版本 |
| `GET` | `/api/audiences/{id}/versions/{version}` | any | 查询指定规则版本及其最新计算批次 |
| `GET` | `/api/audiences/{id}/versions/{version}/members` | any | 查询该版本最新计算批次的成员画像 |

## MDP 营销计划

| 方法 | 路径 | 角色 | 当前行为 |
|---|---|---|---|
| `GET` | `/api/marketing-plans` | any | 查询营销计划及完整画布列表 |
| `POST` | `/api/marketing-plans` | operator | 创建草稿计划；只做基础节点和边端点校验 |
| `GET` | `/api/marketing-plans/{id}` | any | 查询计划及完整画布 |
| `POST` | `/api/marketing-plans/{id}/publish` | operator | 完整校验客群版本、节点配置与流程图后发布 |

## 执行监控

| 方法 | 路径 | 角色 | 当前行为 |
|---|---|---|---|
| `POST` | `/api/marketing-plans/{id}/runs` | operator | 启动已发布计划，固定最新客群计算批次并同步执行用户流程 |
| `GET` | `/api/runs` | any | 查询全部执行批次 |
| `GET` | `/api/runs/{runId}/journeys` | any | 查询指定批次的逐用户流程实例；不存在的批次当前返回空数组 |
| `GET` | `/api/runs/{runId}/journeys/{userId}/trace` | any | 查询用户的有序节点轨迹；运行不存在返回 404 |

## 遗留权益领取技术切片

这组接口不属于当前 MDP 主链路，保留用于库存、幂等和事务回归。

| 方法 | 路径 | 角色 | 当前行为 |
|---|---|---|---|
| `POST` | `/api/campaigns` | operator | 创建权益活动草稿 |
| `GET` | `/api/campaigns/{id}` | any | 查询权益活动 |
| `POST` | `/api/campaigns/{id}/activate` | operator | 激活草稿活动 |
| `POST` | `/api/campaigns/{id}/claims` | user | 使用 `X-User-Id` 领取并扣减库存 |
| `GET` | `/api/campaigns/{id}/claims` | any | 查询领取记录 |

## 测试支持

| 方法 | 路径 | 角色 | 当前行为 |
|---|---|---|---|
| `POST` | `/api/test/reset` | operator | 清空业务数据并重新装载固定合成画像，返回 204 |

## 当前明确未实现

以下内容不得写入 V1.0 OpenAPI：计划编辑、审批、驳回、暂停、恢复、主动结束、延迟自动恢复、聚合指标、真实 Doris 和真实渠道。

## 合同验证

```bash
cd campaignlab
./mvnw -Dtest=OpenApiContractTests test
```

该测试验证：

1. 文件声明为 OpenAPI 3.1.0；
2. 所有本地 `$ref` 都能定位到真实组件；
3. 21 个 OpenAPI 操作与 Spring Controller 注册的 `/api/**` 方法和路径完全一致；
4. 每个操作都有唯一 `operationId`、业务标签和响应定义。
