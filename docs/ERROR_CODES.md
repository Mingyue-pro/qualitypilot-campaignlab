# CampaignLab V1.0 当前错误码

错误响应统一采用：

```json
{"code":"INVALID_STATE","message":"...","timestamp":"2026-10-28T11:00:00Z"}
```

| Code | HTTP | 当前触发场景 |
|---|---:|---|
| `INVALID_REQUEST` | 400 | Bean Validation 请求字段校验失败 |
| `INVALID_AUDIENCE_RULE` | 400 | 客群必填条件缺失、字段不在白名单或条件不完整 |
| `INVALID_REFRESH_FREQUENCY` | 400 | 动态客群未使用 `DAILY` |
| `INVALID_CAMPAIGN` | 400 | 遗留活动名称、时间范围或库存非法 |
| `INVALID_PLAN` | 400 | 计划名称、节点或边基础结构缺失 |
| `INVALID_GRAPH` | 409 | 发布时流程图、节点配置或分支规则不合法 |
| `USER_REQUIRED` | 400 | 权益领取缺少 `X-User-Id` |
| `PERMISSION_DENIED` | 403 | `X-Role` 缺失或角色无权操作 |
| `RESOURCE_NOT_FOUND` | 404 | 客群、客群版本或计算批次不存在 |
| `CAMPAIGN_NOT_FOUND` | 404 | 遗留权益活动不存在 |
| `PLAN_NOT_FOUND` | 404 | 营销计划不存在 |
| `RUN_NOT_FOUND` | 404 | 执行批次不存在 |
| `STATIC_AUDIENCE_NOT_REFRESHABLE` | 409 | 尝试自动刷新静态客群 |
| `INVALID_STATE` | 409 | 遗留活动当前状态不允许激活或领取 |
| `OUTSIDE_CLAIM_PERIOD` | 409 | 当前时间不在权益领取期 |
| `INVENTORY_EXHAUSTED` | 409 | 权益库存为零 |
| `DUPLICATE_CLAIM` | 409 | 同一用户重复领取同一活动 |
| `AUDIENCE_VERSION_NOT_FOUND` | 409 | 发布计划时引用的客群规则版本不存在 |
| `AUDIENCE_NOT_PUBLISHED` | 409 | 发布计划时引用了未发布客群版本 |
| `PLAN_NOT_PUBLISHED` | 409 | 尝试执行草稿计划 |
| `AUDIENCE_BATCH_NOT_FOUND` | 409 | 执行计划时找不到对应客群计算批次 |

本表只列当前代码明确产生的业务错误。V1.1 计划中的错误码不得提前加入 V1.0 基线。
