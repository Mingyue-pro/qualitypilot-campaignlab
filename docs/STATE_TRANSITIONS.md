# 营销计划状态流转

```text
DRAFT ──submit──> PENDING_APPROVAL ──approve──> APPROVED ──start──> RUNNING
  ↑                       │ reject                    │ pause       │ end
  └────edit REJECTED <────┘                           ↓             ↓
                                                    PAUSED ─resume> RUNNING
                                                      └────end────> ENDED
```

| 当前状态 | 允许动作 | 下一状态 |
|---|---|---|
| DRAFT | edit / submit | DRAFT / PENDING_APPROVAL |
| PENDING_APPROVAL | approve / reject | APPROVED / REJECTED |
| REJECTED | edit / submit | REJECTED / PENDING_APPROVAL |
| APPROVED | start | RUNNING |
| RUNNING | pause / end / execute | PAUSED / ENDED / RUNNING |
| PAUSED | resume / end | RUNNING / ENDED |
| ENDED | query only | ENDED |

所有未列出的转换必须返回 `INVALID_STATE`，且不能修改数据库。
