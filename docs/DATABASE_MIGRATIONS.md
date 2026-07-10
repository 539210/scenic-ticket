# 数据库迁移计划与记录

更新时间：2026-07-10

## 当前基线

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

## 计划脚本顺序

| 顺序 | 计划文件 | 内容 | 回滚原则 |
| --- | --- | --- | --- |
| 01 | `mysql_day09_migration_baseline.sql` | 迁移记录表、现有列/索引兼容检查 | 仅删除迁移记录对象，不碰业务数据 |
| 02 | `mysql_day09_ticket_types.sql` | `ticket_types`，保留 items.price/discount_rate 作为兼容字段 | 新业务未启用前可 drop 新表 |
| 03 | `mysql_day09_inventory.sql` | `ticket_inventory`、日期/库存约束与索引 | 先停用新流程并导出数据后回滚 |
| 04 | `mysql_day09_order_lifecycle.sql` | orders 新增票种、日期、金额快照、支付/过期时间等字段 | 保留新增字段，必要时回退应用版本，不丢订单 |
| 05 | `mysql_day09_refunds_admissions.sql` | `refunds`、`admissions`、约束与索引 | 不自动删除已有退款/核销证据 |
| 06 | `mysql_day09_operational_objects.sql` | 更新实际使用的视图、存储过程、触发器 | 恢复上一版本定义 |
| 07 | `mongodb_day09_id_compatibility.js` | 检测/迁移字符串 ID、补 updated_at、评论唯一性准备 | 保留迁移前备份字段/报告 |
| 08 | `mongodb_day09_indexes.js` | 新索引与评论唯一索引（数据清理后） | 可删除新索引，不删除文档 |

实际文件名可在 M1 调整，但必须保持顺序和文档一致。

## 测试数据库策略

- MySQL：创建 `scenic_ticket_test`，所有 DDL/DML 验证只在该库运行。
- MongoDB：使用 `scenic_ticket_test`，测试前后只清理该库。
- 测试配置通过测试 resources 或系统属性覆盖，不复制/提交真实密码。
- 启动测试时打印数据库名但绝不打印用户名密码或 URI 凭据。

## 待验证项

- 空库执行全套脚本。
- 已有 Day08 结构升级到新结构。
- 外键、CHECK、唯一约束、索引。
- 两个视图、两个存储过程、两个触发器实际使用。
- 库存并发、支付/退款/核销事务回滚。
- MongoDB 四集合、索引、三条聚合、ID 兼容迁移。
