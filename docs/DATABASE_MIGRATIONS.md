# 数据库迁移计划与记录

更新时间：2026-07-14

## 历史基线

- MySQL 全新安装脚本硬编码创建/使用 `scenic_ticket`。
- Day07 增加索引；Day08 增加 items 价格/优惠和 orders 数量/单价/优惠/支付方式。
- MongoDB 初始化脚本直接 drop 四个集合，只适合明确授权的全新初始化，不适合升级。
- 本地配置当前指向 `scenic_ticket`；在提供隔离机制前不运行写入型集成测试。

## 迁移纪律

1. 课程原表/字段只保留和扩展，不删除/重命名。
2. 每次结构变化同时提供：全新安装、顺序化正向迁移、回滚方案、数据兼容说明、文档更新。
3. 迁移以 `schema_migrations` 或等效记录表防止重复执行。
4. 测试工具必须拒绝除 `scenic_ticket_test` 外的数据库名。
5. MongoDB 数字/字符串 ID 先兼容读，再迁移数据，最后建立约束/唯一索引。
6. MongoDB JS 脚本必须使用连接 URI 当前数据库，不得在文件内硬编码切换到业务库；只允许 `scenic_ticket` 与 `scenic_ticket_test`。

## 实际迁移脚本顺序

| 顺序 | 文件 | 内容 | 回滚原则 |
| --- | --- | --- | --- |
| 01 | `mysql_day09_migration_baseline.sql` | 迁移记录表、现有列/索引兼容检查 | 仅删除迁移记录对象，不碰业务数据 |
| 02 | `mysql_day09_ticket_types_inventory.sql` | `ticket_types`、`ticket_inventory`、初始票种/日期库存与约束 | 先停用新流程并导出新增表，不能删除已有业务证据 |
| 03 | `mysql_day09_order_lifecycle_refunds_admissions.sql` | 订单票种/日期/价格/时间快照、`refunds`、`admissions` | 保留新增字段和审计性数据，必要时仅回退应用 |
| 04 | `mysql_views.sql` | 安装/更新两个课程视图 | 恢复上一版本定义 |
| 05 | `mysql_procedures.sql` | 安装/更新两个课程存储过程 | 恢复上一版本定义 |
| 06 | `mysql_triggers.sql` | 安装/更新两个课程触发器 | 恢复上一版本定义 |
| 07 | `mongodb_day09_id_compatibility.js` | 检测/迁移字符串 ID、补 updated_at、评论唯一性准备 | 备份后回退字段类型；脚本不删评论 |
| 08 | `mongodb_day09_indexes.js` | 新复合/文本索引与评论唯一索引（数据清理后） | 可删除新索引，不删除文档 |

## 测试数据库策略

- MySQL：创建 `scenic_ticket_test`，所有 DDL/DML 验证只在该库运行。
- MongoDB：使用 `scenic_ticket_test`，测试前后只清理该库。
- 测试配置通过测试 resources 或系统属性覆盖，不复制/提交真实密码。
- 启动测试时打印数据库名但绝不打印用户名密码或 URI 凭据。

## Day09 已实现脚本

- `mysql_day09_migration_baseline.sql`
- `mysql_day09_ticket_types_inventory.sql`
- `mysql_day09_order_lifecycle_refunds_admissions.sql`
- `mongodb_day09_id_compatibility.js`
- `mongodb_day09_indexes.js`

回滚策略：新业务启用前可回退应用并保留新增表/列；退款、核销、订单快照等审计性数据不得自动删除。需要物理回滚时先导出新增表并确认没有新流程数据，再由管理员执行独立回滚脚本。MongoDB 迁移只做可逆类型规范化/补字段和索引，不自动删除重复评论。

## 已验证项（2026-07-14）

- 全新 MySQL `scenic_ticket_test`：10 表、12 外键、2 视图、2 存储过程、2 触发器、60 票种、420 库存行、4 迁移记录；库存不变量违规 0。
- Day08 旧结构夹具升级：原 users/profiles/orders 记录保留，订单成功回填成人票和 100.00/90.00 价格快照；升级后 10 表、2 视图、2 过程、2 触发器。
- MongoDB `scenic_ticket_test`：四集合、索引、中文文本、数值 ID 和至少四类真实聚合通过 Java Driver 集成测试。
- M10 完整 Java 21 集成命令：167 tests，0 failures，0 errors，1 skipped（仅 Fake DAO 压力测试）。
- M11 加入 5 项文档一致性回归后的最终完整命令：172 tests，0 failures，0 errors，1 skipped。
- 库存并发、支付/退款/核销、Mongo 降级和普通用户/管理员完整流程均在空测试库最终重跑通过。

## mongosh 实跑（2026-07-14）

- 安装官方 `mongosh 2.9.2` 后，以 `mongodb://localhost:27017/scenic_ticket_test` 连接空测试库，顺序执行初始化、Day07 优化、Day09 ID 兼容和 Day09 索引脚本。
- 四脚本均从连接 URI 获取当前数据库并执行允许名单校验，不再硬编码切换到 `scenic_ticket`。
- 实跑结果：4 个受管集合；120 条行为日志、30 条评论、20 条详情、120 条系统日志；索引数分别为 7、6、3、7；评论重复组为 0，唯一索引创建成功。

## M8 对象实际使用补充

- `ReportDAO` 已调用 `sp_monthly_order_report` 和 `sp_update_inactive_items`。
- `ReportDAO` 已查询 `v_user_profile` 和 `v_item_order_summary`。
- `ReportDatabaseObjectsIntegrationTest` 在 `scenic_ticket_test` 事务内验证两个视图和第二个存储过程，测试结束回滚对景点状态的改动。
