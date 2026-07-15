# Day 06 统计报表模块说明

## 完成范围

Day 06 在 Day 04 统计初版和 Day 05 推荐/跨库联查基础上，补齐数据统计与报表模块。模块覆盖 MongoDB 行为聚合统计、MongoDB 系统操作审计、MySQL 月度订单报表存储过程调用，并提供统一的服务层入口。

## MySQL 月度报表

| 类 | 功能 |
| --- | --- |
| `ReportDAO` | 使用 `CallableStatement` 调用 `sp_monthly_order_report` |
| `MonthlyOrderReportDTO` | 封装月度订单日报结果：日期、订单数、总金额 |
| `MonthlyOrderDetailDTO` | 封装指定日期的购买用户、景点、票种、金额和状态明细 |
| `StatisticsService#getMonthlyOrderReport` | 对外提供月度订单报表查询入口 |
| `StatisticsService#getMonthlyOrderDetails` | 管理员按日期钻取已支付/已完成订单明细 |

调用流程：

1. 服务层校验报表年份和月份。
2. `ReportDAO` 通过 `{CALL sp_monthly_order_report(?, ?)}` 调用 MySQL 存储过程。
3. 将结果集映射为 `MonthlyOrderReportDTO` 列表。
4. Swing 月度订单表按日展示汇总；管理员单击日期行可弹出当天订单明细。
5. 汇总和明细均仅统计已支付、已完成订单，确保订单数与弹窗记录数一致。

## MongoDB 行为统计

| 方法 | 说明 |
| --- | --- |
| `getHotItemRanking` | 基于 `action_logs` 聚合热门景点排行 |
| `getUserBehaviorReport` | 按行为类型统计指定用户行为 |
| `getUserReport` | 汇总用户访问次数、访问景点数、停留时长、浏览/搜索/评论/订单数量 |
| `getActionTypeSummary` | 全局行为类型统计 |
| `getDailyActionTrend` | 按日期和行为类型生成趋势 |

Day 06 新增 `LogDAO#aggregateUserReport`，用于生成用户报告核心指标，避免只做简单列表查询。

## 系统操作审计

| 类/方法 | 功能 |
| --- | --- |
| `SystemLogDAO#findByCondition` | 按用户、日志类型、级别、时间范围查询系统日志 |
| `SystemLogDAO#aggregateAuditSummary` | 按日志类型和级别聚合操作审计摘要 |
| `SystemLogDAO#aggregateDailyAuditTrend` | 按日期统计系统操作趋势 |
| `SystemLogDAO#aggregateUserOperationSummary` | 按用户统计操作次数、告警数、错误数 |
| `SystemLogService` | 提供记录 INFO/WARN/ERROR 操作日志和审计查询服务 |

系统日志记录字段继续使用 MongoDB `system_logs` 集合，核心字段包括：

- `user_id`
- `log_type`
- `log_level`
- `message`
- `action_detail`
- `timestamp`

## 统计服务整合

`StatisticsService` 已整合以下报表入口：

- 热门景点排行
- 用户行为报告
- 用户综合报告
- 行为类型摘要
- 每日行为趋势
- 系统操作审计摘要
- 系统操作审计趋势
- 用户操作审计汇总
- MySQL 月度订单报表

`StatisticsReportDTO` 增加：

- `systemAuditSummary`
- `systemAuditTrend`
- `monthlyOrderReport`

## 验收点

- MongoDB 聚合统计覆盖热门排行、用户报告、操作审计。
- MySQL 月度订单报表通过存储过程调用完成。
- 系统操作日志有独立服务层，支持记录和审计查询。
- 普通 Maven 编译测试通过。
