# Day 02 数据库脚本验证记录

## 验证环境

| 项目 | 结果 |
| --- | --- |
| Java | 21.0.8 |
| MySQL | 8.0.45 |
| MongoDB Shell | 本机未检测到 `mongosh` 命令 |
| Maven | 本机未检测到 `mvn` 命令 |

## MySQL 验证

已使用本机 MySQL 8.0.45 执行以下脚本：

```text
src/main/resources/sql/mysql_schema.sql
src/main/resources/sql/mysql_init_data.sql
src/main/resources/sql/mysql_views.sql
src/main/resources/sql/mysql_procedures.sql
src/main/resources/sql/mysql_triggers.sql
```

验证结果：

| 检查项 | 结果 |
| --- | --- |
| users | 10 条 |
| categories | 10 条 |
| items | 10 条 |
| orders | 10 条 |
| profiles | 10 条 |
| 视图 | 2 个：`v_user_profile`、`v_item_order_summary` |
| 存储过程 | 2 个：`sp_monthly_order_report`、`sp_update_inactive_items` |
| 触发器 | 2 个：`trg_orders_before_insert`、`trg_items_before_update` |

## MongoDB 验证

已完成 `mongodb_init.js`，包含：

- 4 个集合：`action_logs`、`comments`、`item_details`、`system_logs`
- 集合索引
- `action_logs` 120 条初始化数据
- `comments` 20 条初始化数据
- `item_details` 20 条初始化数据
- `system_logs` 120 条初始化数据
- 热门景点排行聚合管道示例

由于本机未检测到 `mongosh` 命令，本轮未实际导入 MongoDB。安装或配置 `mongosh` 后执行：

```text
mongosh src/main/resources/sql/mongodb_init.js
```

## Java 验证

已完成 DAO 基础结构、MySQL `UserDAO`、MongoDB 基础 DAO 和 `UserDAOTest`。

由于本机未检测到 `mvn` 命令，本轮未运行 Maven 编译和测试。安装或配置 Maven 后执行：

```text
mvn test
mvn test -DintegrationTests=true
```
