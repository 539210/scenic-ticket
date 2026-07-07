# Day 04 行为日志与统计模块说明

## 完成范围

Day 04 重点完成 MongoDB 行为日志模块，补充评论管理，并实现统计服务初版。代码继续基于 Day 03 的 DAO/Service 架构扩展。

## 行为日志模块

| 类 | 功能 |
| --- | --- |
| `LogDAO` | 行为日志写入、按用户查询最近行为、按景点查询行为、用户行为聚合、热门景点聚合、行为类型统计、每日趋势 |
| `BehaviorLogService` | 封装浏览、搜索、评论行为记录和最近行为查询 |
| `BehaviorLogQuery` | 行为日志查询条件 DTO |

行为类型约定：

| 类型 | 说明 |
| --- | --- |
| `VIEW` | 浏览景点详情 |
| `SEARCH` | 搜索景点 |
| `ORDER` | 创建订单 |
| `COMMENT` | 发表评论 |

行为日志写入集合：

```text
action_logs
```

## 评论管理

| 类 | 功能 |
| --- | --- |
| `CommentDAO` | 评论写入、按景点查询、按用户查询、评分汇总、评分分布、热门标签 |
| `BehaviorLogService#addComment` | 校验评分和内容，写入评论，同时记录 `COMMENT` 行为日志 |

评论写入集合：

```text
comments
```

评分校验规则：

- `rating` 必须在 1 到 5 之间。
- 评论内容不能为空。
- 发表评论后同步写入一条行为日志。

## 聚合管道

Day 04 已实现以下 MongoDB 聚合：

| 方法 | 集合 | 说明 |
| --- | --- | --- |
| `aggregateUserBehavior` | `action_logs` | 按用户和行为类型统计次数、总时长、平均时长 |
| `aggregateHotItems` | `action_logs` | 按景点统计总行为数、浏览数、订单数、平均停留时长 |
| `aggregateActionTypeSummary` | `action_logs` | 按行为类型统计行为数和用户数 |
| `aggregateDailyTrend` | `action_logs` | 按日期和行为类型统计趋势 |
| `aggregateRatingByItem` | `comments` | 按景点统计评论数、平均分、最高分、最低分 |
| `aggregateRatingDistribution` | `comments` | 按景点评分分布 |
| `aggregateHotTags` | `comments` | 全局热门评论标签 |

## 统计服务初版

| 类 | 功能 |
| --- | --- |
| `StatisticsService` | 对外提供用户行为报告、热门景点排行、行为类型统计、每日趋势、评分统计、热门标签 |
| `StatisticsReportDTO` | 聚合首页/报表页需要的热门景点、行为类型、趋势、热门标签 |

统计服务目前返回 MongoDB `Document` 结果，便于后续 Swing 报表界面直接展示或转换成表格模型。

## 验收点

- MongoDB 行为日志模块支持写入和查询。
- 评论模块支持写入、查询和评分统计。
- 至少 3 个 MongoDB 聚合管道已实现；当前实际实现 7 个。
- `StatisticsService` 初版已可提供报表数据入口。
- 不修改 Day 02 已验证的 MySQL schema。
