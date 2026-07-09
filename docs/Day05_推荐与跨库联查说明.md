# Day 05 推荐与跨数据库联查说明

## 完成范围

Day 05 在 Day 04 行为日志和统计模块基础上，完成推荐功能和跨数据库联查。MySQL 继续负责景点基础信息，MongoDB 负责行为日志、评论、详情等扩展数据。

## 推荐功能

| 类 | 功能 |
| --- | --- |
| `RecommendService` | 个性化推荐、热门景点推荐、高评分景点推荐 |
| `RecommendationDTO` | 推荐结果 DTO，包含景点、详情、评分摘要、分数和推荐理由 |
| `LogDAO#aggregateUserItemScores` | 根据用户行为计算景点兴趣分 |
| `CommentDAO#aggregateTopRatedItems` | 根据评论平均分和评论数生成高评分景点 |
| `ItemDAO#findActiveByCategoryIds` | 根据用户偏好分类查找候选景点 |

个性化推荐流程：

1. 从 MongoDB `action_logs` 聚合用户行为，按 `VIEW`、`COMMENT`、`ORDER` 计算兴趣分。
2. 根据用户已交互景点回查 MySQL `items`，提取偏好分类。
3. 从 MySQL 查询同分类且未交互过的可售景点。
4. 不足数量时用 MongoDB 热门景点聚合结果补充。
5. 最后补充最新上架可售景点。
6. 每条推荐组装 MongoDB 详情和评分摘要。

推荐分数规则：

| 推荐类型 | 分数规则 |
| --- | --- |
| 个性化推荐 | 同类景点默认按 85 分展示，最新上架补充推荐按 60 分展示 |
| 热门推荐 | 按当前热门列表最高热度归一化到 0-100 分 |
| 高评分推荐 | 按平均评分折算到 0-100 分 |

Swing 前端统一显示 0-100 的推荐分，保留 1 位小数，避免不同推荐入口使用不同尺度。

## 跨数据库联查

| 类 | 功能 |
| --- | --- |
| `CrossDatabaseQueryService` | 跨库详情查询、搜索结果跨库组装、按 ID 批量跨库组装 |
| `CrossDatabaseItemDTO` | 包含 MySQL 景点基础信息、MongoDB 详情、评分、评论、行为摘要 |
| `ItemDAO#findByIds` | 按多个 MySQL `item_id` 批量查询景点 |
| `DetailDAO#findByItemId` | 查询 MongoDB 景点详情 |
| `CommentDAO#aggregateRatingByItem` | 查询评论评分摘要 |
| `LogDAO#findByItemId` | 查询景点相关行为日志 |

跨库详情查询流程：

1. 使用 MySQL `item_id` 查询 `items` 基础信息。
2. 使用同一个 `item_id` 查询 MongoDB `item_details`。
3. 使用 `item_id` 查询 MongoDB `comments` 和评分摘要。
4. 使用 `item_id` 查询 MongoDB `action_logs` 行为摘要。
5. 组装 `CrossDatabaseItemDTO` 返回给服务层或界面层。

## 验收点

- 推荐模块不复制 MySQL 主数据，MongoDB 只保存引用 ID 和扩展信息。
- 推荐功能至少支持个性化、热门、高评分三类结果。
- 跨数据库联查以 MySQL `item_id` 作为 MongoDB 文档引用字段。
- MySQL 查询仍使用 `PreparedStatement`。
- 普通 Maven 编译测试通过。
