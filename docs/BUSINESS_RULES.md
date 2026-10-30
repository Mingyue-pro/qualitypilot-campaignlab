# CampaignLab V1.0 业务规则

| ID | 规则 | 失败结果 |
|---|---|---|
| BR-AUD-001 | 客群只能使用允许的合成标签字段和操作符。 | `INVALID_AUDIENCE_RULE` |
| BR-AUD-002 | 客群预估人数必须来自当前合成用户数据，不能由调用方填写。 | 拒绝输入人数 |
| BR-AUD-003 | 静态客群保存时按最新画像重新计算，保存后成员固定，不能自动刷新。 | `STATIC_AUDIENCE_NOT_REFRESHABLE` |
| BR-AUD-004 | 动态客群必须配置更新周期；每次成功 Task 在同一规则版本下产生新的计算批次。 | `INVALID_REFRESH_FREQUENCY` |
| BR-AUD-005 | 预览不落库，正式保存不能直接采用旧预览结果，必须重新计算。 | 以保存时画像为准 |
| BR-PLAN-001 | 只有 `operator` 可以创建和编辑计划。 | `PERMISSION_DENIED` |
| BR-PLAN-002 | 只有 `DRAFT` 或 `REJECTED` 计划可以编辑。 | `INVALID_STATE` |
| BR-PLAN-003 | 结束时间必须晚于开始时间；对照组比例为 0–100。 | `INVALID_PLAN` |
| BR-APP-001 | `operator` 提交计划，`approver` 批准或拒绝。 | `PERMISSION_DENIED` |
| BR-APP-002 | 只有 `PENDING_APPROVAL` 计划可以批准或拒绝。 | `INVALID_STATE` |
| BR-RUN-001 | 只有已批准且当前处于有效期的计划可以启动。 | `INVALID_STATE` / `OUTSIDE_PLAN_PERIOD` |
| BR-RUN-002 | 暂停或结束的计划不产生新的执行和渠道事件。 | `INVALID_STATE` |
| BR-EXE-001 | 用户必须属于绑定客群才能进入后续节点。 | `AUDIENCE_FILTERED` |
| BR-EXE-002 | 在互斥窗口内已有成功触达的用户必须被拦截。 | `MUTUAL_EXCLUSION` |
| BR-EXE-003 | 达到频控次数的用户必须被拦截。 | `FREQUENCY_CAPPED` |
| BR-EXE-004 | 对照组由 `plan_id + user_id` 的稳定指纹确定；重复运行分组不变。 | 不允许随机漂移 |
| BR-EXE-005 | 对照组不调用模拟渠道。 | 不产生 `TouchEvent` |
| BR-EXE-006 | 同一 `run_id + user_id + channel` 只能产生一个渠道事件。 | 幂等返回已有结果 |
| BR-MET-001 | 监控汇总必须由用户级执行结果计算。 | `METRIC_MISMATCH` |

## 明确边界

- V1.0 的频控含义是“在计划配置的小时窗口内最多成功触达 N 次”。
- V1.0 不存在“每日领取权益”规则。
- 同一用户命中多个拦截条件时，按客群 → 互斥 → 频控顺序记录第一个主原因。
- 该顺序是 V1.0 基线；V1.1 将以“多层频控和互斥优先级”作为新需求重新评审。
