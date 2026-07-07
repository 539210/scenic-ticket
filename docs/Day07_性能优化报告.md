# Day 07 性能优化报告

## 优化目标

Day 07 重点围绕索引、SQL、连接池和批量写入做性能优化，使 Day 01 到 Day 06 的核心模块具备更稳定的集成基础。

## MySQL 优化

| 优化项 | 说明 |
| --- | --- |
| 月度报表 SQL 优化 | `sp_monthly_order_report` 从 `YEAR(created_at)`/`MONTH(created_at)` 改为日期范围查询，避免对索引列使用函数 |
| 新增景点列表索引 | `idx_items_status_updated_at(status, updated_at DESC, item_id DESC)` 支持可售景点按更新时间排序 |
| 新增订单报表索引 | `idx_orders_created_status(created_at, status)` 支持月度报表按时间范围过滤 |
| 预编译缓存 | HikariCP 数据源增加 MySQL Connector/J 预编译语句缓存参数 |
| 批处理参数 | 增加 `rewriteBatchedStatements=true`，为后续 JDBC 批处理保留优化能力 |

新增脚本：

```text
src/main/resources/sql/mysql_day07_optimization.sql
```

## MongoDB 优化

| 集合 | 新增/强化索引 |
| --- | --- |
| `action_logs` | `{ created_at: -1, action_type: 1, item_id: 1 }` |
| `comments` | `{ rating: -1, item_id: 1 }` |
| `system_logs` | `{ timestamp: -1, log_type: 1, log_level: 1 }` |

新增脚本：

```text
src/main/resources/sql/mongodb_day07_optimization.js
```

## 批量写入优化

| 类/方法 | 说明 |
| --- | --- |
| `LogDAO#insertActionLogs` | 使用 MongoDB `insertMany` 批量写入行为日志 |
| `SystemLogDAO#insertSystemLogs` | 使用 MongoDB `insertMany` 批量写入系统日志 |
| `BatchLogService` | 按默认 500 条分批导入，单批上限 2000 条 |

批量导入采用 `ordered(false)`，单条异常不影响后续文档继续写入，更适合日志类高写入场景。

## 连接池优化

`MySQLDBUtil` 增加：

- `idleTimeout`
- `maxLifetime`
- `validationTimeout`
- `leakDetectionThreshold`
- `connectionTestQuery`
- MySQL 预编译语句缓存参数

`db.properties.example` 已同步新增连接池参数，真实本地配置仍放在 `db.properties`，不提交仓库。

## 后续可继续优化

- Day 08 可用批量日志导入生成 10000 条测试数据。
- 对 `EXPLAIN` 结果进行截图或记录，补充到压力测试报告。
- 为 Swing 查询分页增加上一页/下一页，减少大结果集一次性展示。
