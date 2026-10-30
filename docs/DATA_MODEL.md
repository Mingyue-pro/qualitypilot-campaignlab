# 数据模型

| 对象 | 关键字段 | 说明 |
|---|---|---|
| SyntheticUser / `doris_user_profile` | user_id, city, member_level, is_new_user, risk_level, marketing_allowed | 已实现的模拟 Doris 画像宽表，不包含真实个人数据 |
| Audience | audience_id, name, type, refresh_frequency, created_at | 已实现；区分 STATIC/DYNAMIC |
| AudienceVersion + AudienceCondition | audience_id, rule_version, match_type, conditions, published | 已实现；表示规则版本，不表示每日刷新 |
| AudienceCalculation + AudienceMember | audience_id, rule_version, batch_number, member_count, calculated_at, members | 已实现；动态客群同一规则可产生 R1/R2 等计算批次 |
| MarketingPlan | plan_id, audience_id, channel, time range, cap, exclusion window, control ratio, status, version | 营销计划 |
| ApprovalRecord | approval_id, plan_id, decision, reason, actor, time | 审批证据 |
| StatusHistory | event_id, plan_id, from/to, action, actor, time | 状态时间线 |
| ExecutionRun | run_id, plan_id, started_at, status | 一次可追踪执行 |
| UserExecution | run_id, user_id, group, result, reason | 用户级节点结果 |
| TouchEvent | event_id, run_id, user_id, channel, delivery_status | 模拟下游渠道事件 |
| ExecutionMetrics | run_id and counts | 从 UserExecution/TouchEvent 汇总的监控值 |

唯一性约束：`TouchEvent(run_id, user_id, channel)`，用于阻止执行重试造成重复触达。
