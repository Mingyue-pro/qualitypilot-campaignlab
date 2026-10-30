# V1.1 固定测试数据

本文件定义后续实现和QualityPilot测试使用的确定性数据。符号ID表示业务别名；实际运行产生UUID后，测试需要记录别名与真实ID的映射。

## 1. 固定时钟

自动化场景使用可注入`Clock`，参考时区固定为`Asia/Shanghai`。

| 时间别名 | 上海时间 | UTC时间 | 用途 |
|---|---|---|---|
| `D1_MORNING` | 2026-11-03 10:00:00+08:00 | 2026-11-03 02:00:00Z | 同日正常执行 |
| `D1_LAST_SECOND` | 2026-11-03 23:59:59+08:00 | 2026-11-03 15:59:59Z | 自然日结束边界 |
| `D2_START` | 2026-11-04 00:00:00+08:00 | 2026-11-03 16:00:00Z | 新自然日起点 |

## 2. 用户

复用V1.0合成画像：

| 用户 | 关键画像 | 用途 |
|---|---|---|
| `U001` | Shanghai、VIP、newUser=true、marketingAllowed=true | 主要频控用户 |
| `U002` | Shanghai、NORMAL、newUser=false、marketingAllowed=true | 验证不同用户额度独立 |

## 3. 客群

| 别名 | 类型 | 规则 | 预期成员 |
|---|---|---|---|
| `AUD-FC-ALL-SH` | STATIC | `city EQUALS Shanghai` | 至少包含U001、U002 |

客群规则版本发布为V1，并使用创建时R1成员快照。

## 4. 计划

### `PLAN-FC-ONE`

- `dailyContactLimit = 1`
- 目标客群：`AUD-FC-ALL-SH / V1`
- 主验证计划。

流程：

```text
START
→ LABEL判断 city=Shanghai
→ SMS营销节点
→ END
```

### `PLAN-FC-TWO`

- `dailyContactLimit = 2`
- 用于验证前两次允许、第三次拦截。

### `PLAN-FC-OTHER`

- `dailyContactLimit = 1`
- 与主计划配置相同但计划ID不同，用于验证计划间额度隔离。

### `PLAN-FC-TWO-NODES`

- `dailyContactLimit = 1`
- 同一路径包含两个营销节点，用于验证单次运行内共享额度。

```text
START → 判断 → SMS → PLACEHOLDER → COUPON → END
```

## 5. 预置执行事实

需求包不预置隐藏缺陷答案。需要历史触达的场景通过公开API先执行一次计划，再执行第二次，从而形成可审计前置数据。

每个场景开始前调用：

```http
POST /api/test/reset
X-Role: operator
```

然后重新创建场景所需客群和计划，避免其他场景遗留回执影响计数。

## 6. 核心场景数据

| 场景 | 计划 | 用户 | 时间 | 前置回执 | 预期 |
|---|---|---|---|---:|---|
| `FC-001` | PLAN-FC-ONE | U001 | D1_MORNING | 0 | 允许，新增1条回执 |
| `FC-002` | PLAN-FC-ONE | U001 | D1_MORNING | 1 | 拦截，不新增回执 |
| `FC-003` | PLAN-FC-TWO | U001 | D1_MORNING | 1 | 允许，累计2条 |
| `FC-004` | PLAN-FC-TWO | U001 | D1_MORNING | 2 | 拦截，保持2条 |
| `FC-005` | PLAN-FC-OTHER | U001 | D1_MORNING | 主计划1条 | 允许，不共享主计划额度 |
| `FC-006` | PLAN-FC-ONE | U002 | D1_MORNING | U001有1条 | 允许，不共享用户额度 |
| `FC-007` | PLAN-FC-ONE | U001 | D2_START | D1有1条 | 允许，新自然日重新计数 |
| `FC-008` | PLAN-FC-TWO-NODES | U001 | D1_MORNING | 0 | 第一个允许，第二个拦截 |

## 7. 确定性要求

- 测试不得依赖执行机器当前时间；
- 测试不得要求渠道一定成功，成功和失败都计数；
- 判断是否触达以`channel_receipt`是否新增为准；
- 判断是否拦截以`node_execution.result = FREQUENCY_CAPPED`为准；
- 测试之间必须重置数据，不能依赖执行顺序。
