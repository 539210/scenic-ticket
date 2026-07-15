# Day 07 性能优化报告

更新时间：2026-07-15（当前版本补充）

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

## 2026-07-15 当前版本性能与体验补充

| 场景 | 当前处理 | 作用 |
| --- | --- | --- |
| 景点简介与评论加载 | 选中景点时后台预取，并按景点 ID 缓存详情、评论和图片 | 避免用户每次点击简介/评论页签都重新查询数据库，减少约一秒的重复等待 |
| 图片展示 | 后台读取第一张有效图片并按区域等比缩放；无图、失效图或非图片响应时隐藏图片区 | 避免把长图片地址渲染到详情文本中，也降低无效图片造成的界面干扰 |
| 异步请求 | 使用会话代次与“同名最新请求”保护 | 快速切换景点、报表或账号时，旧请求完成后不会覆盖新页面数据 |
| 月度订单报表 | 汇总仍使用按日期范围过滤的存储过程；明细按用户、景点、票种和日期查询 | 在保留汇总性能的同时，支持管理员点击日期行查看订单明细 |
| 评分推荐 | 直接聚合 `comments` 的平均评分并按分数排序 | 前台推荐统一为评分排序，避免重复计算个性化与热门推荐结果 |
| 日期输入 | 统一使用月历选择组件 | 消除手工输入日期格式错误导致的无结果查询和重复校验 |

## 当前性能结论

- MySQL 的订单、库存与报表查询继续依赖索引、日期范围条件和连接池；库存更新仍使用单连接事务与行锁。
- MongoDB 的评论评分、热门排行和审计查询继续使用对应复合索引与聚合管道；已移除评论标签写入和标签统计，减少无业务价值的字段与聚合分支。
- Swing 侧重点从“仅提高吞吐”扩展为“避免重复请求和错误回写”，使小窗口操作、页签切换和表格选择更稳定。
