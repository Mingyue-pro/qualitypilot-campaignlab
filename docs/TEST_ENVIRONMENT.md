# CampaignLab 测试环境

- 运行架构：Vue 3 前端（5173）与 Spring Boot API（8080）独立运行。
- 默认运行数据库：MySQL 8.4，使用 Flyway 管理表结构。
- Java 自动化测试：当前使用 H2 的 MySQL兼容模式，以 `test` profile 隔离运行。
- 真实数据库验证：Docker Desktop 29.8.1 中的 MySQL 8.4.11 容器已健康运行；Flyway V1已执行，后端启动、客群创建、数据库查询和应用重启后的持久化均已通过。
- 用户画像：V1.0 在业务数据库中用 `doris_user_profile` 表模拟 Doris，不代表已经接入 Doris。
- 测试数据：均为合成数据；测试可调用重置入口，正常应用重启只在画像表为空时初始化，不会清空已创建的客群。
- 禁止使用真实手机号、真实客户标签、公司接口地址或生产凭据。

## 验证命令

```bash
./mvnw test
cd frontend && npm run build
```

真实 MySQL冒烟证据：6条合成画像已初始化；API创建“ MySQL冒烟客群”得到2名成员；后端重启后客群仍可查询。
