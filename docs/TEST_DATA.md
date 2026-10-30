# 固定合成测试数据

| user_id | city | member_level | is_new_user | 预期用途 |
|---|---|---|:---:|---|
| U001 | Shanghai | VIP | true | 命中上海新客/VIP |
| U002 | Shanghai | NORMAL | false | 命中城市，不命中新客 |
| U003 | Beijing | VIP | false | 命中VIP，不命中上海 |
| U004 | Shanghai | VIP | false | 用于互斥历史 |
| U005 | Shanghai | NORMAL | true | 用于频控历史 |
| U006 | Shenzhen | NORMAL | true | 不命中上海客群 |

固定历史：U004 在互斥窗口内已有成功短信触达；U005 在频控窗口内已达到上限。重置接口必须恢复这些数据，以便每次演示结果一致。
