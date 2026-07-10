# MongoDB 集合设计文档

## 1. 设计目标

MongoDB 用于存储高写入、半结构化、便于聚合分析的数据，包括用户行为日志、评论互动、景点详情和系统操作日志。MySQL 中的 `user_id` 与 `item_id` 作为 MongoDB 文档的引用字段，避免跨库数据重复维护。

## 2. 数据库

```text
scenic_ticket
```

## 3. 集合设计

### 3.1 action_logs

用途：记录用户浏览、查询、下单等行为，用于热门排行、用户行为分析和推荐。

示例文档：

```json
{
  "user_id": 10001,
  "item_id": 2001,
  "action_type": "VIEW",
  "duration_seconds": 120,
  "client_info": {
    "client_type": "SWING",
    "ip": "127.0.0.1"
  },
  "created_at": "ISODate(...)"
}
```

索引：

| 字段 | 类型 | 用途 |
| --- | --- | --- |
| user_id, created_at | 复合索引 | 用户行为时间线 |
| item_id, action_type | 复合索引 | 景点热度统计 |
| created_at | 普通索引 | 日志时间范围查询 |

### 3.2 comments

用途：存储用户对景点的评分、文字评论和标签。

示例文档：

```json
{
  "user_id": 10001,
  "item_id": 2001,
  "content": "景区环境很好，入园很方便。",
  "rating": 5,
  "tags": ["环境好", "适合亲子"],
  "created_at": "ISODate(...)",
  "updated_at": "ISODate(...)"
}
```

索引：

| 字段 | 类型 | 用途 |
| --- | --- | --- |
| item_id, created_at | 复合索引 | 景点评论列表 |
| item_id, rating | 复合索引 | 评分统计 |
| user_id | 普通索引 | 用户评论历史 |
| user_id, item_id | 唯一索引 | 每个用户对同一景点只保留一条评论 |

### 3.3 item_details

用途：存储景点长文本详情、图片和扩展元数据。

示例文档：

```json
{
  "item_id": 2001,
  "description": "景点详细描述",
  "images": [
    "https://example.com/scenic-2001-1.jpg"
  ],
  "metadata": {
    "language": "zh-CN",
    "open_time": "08:00-18:00",
    "address": "示例景区地址"
  },
  "updated_at": "ISODate(...)"
}
```

索引：

| 字段 | 类型 | 用途 |
| --- | --- | --- |
| item_id | 唯一索引 | 按景点查询详情 |
| metadata.language | 普通索引 | 多语言扩展查询 |

### 3.4 system_logs

用途：记录登录、后台管理、系统错误等操作审计日志。

示例文档：

```json
{
  "user_id": 10001,
  "log_type": "LOGIN",
  "log_level": "INFO",
  "message": "登录成功",
  "action_detail": {
    "ip": "127.0.0.1",
    "operation": "POST /login"
  },
  "timestamp": "ISODate(...)"
}
```

索引：

| 字段 | 类型 | 用途 |
| --- | --- | --- |
| user_id, timestamp | 复合索引 | 用户操作审计 |
| log_type, log_level | 复合索引 | 日志筛选 |
| timestamp | 普通索引 | 时间范围查询 |

## 4. 聚合管道规划

| 名称 | 集合 | 说明 |
| --- | --- | --- |
| 用户行为统计 | action_logs | 按用户统计浏览、查询、下单次数 |
| 热门景点排行 | action_logs, comments | 按浏览量、评论数、评分计算热度 |
| 操作审计统计 | system_logs | 按操作类型和日志级别统计系统事件 |

## 5. 初始化数据要求

- `action_logs`：不少于 100 条。
- `comments`：不少于 20 条。
- `item_details`：不少于 20 条。
- `system_logs`：不少于 100 条。

## 6. 与 MySQL 的关系

| MongoDB 字段 | 引用 MySQL 表 | 说明 |
| --- | --- | --- |
| user_id | users.user_id | 关联用户 |
| item_id | items.item_id | 关联景点 |

跨数据库查询由 Java Service 层完成：先从 MySQL 查询结构化主数据，再使用 `user_id` 或 `item_id` 查询 MongoDB 扩展文档，最后组装 DTO 返回给界面层。

## 7. 初始化与迁移

- `mongodb_init.js` 仅用于空数据库全新安装；检测到四个受管集合中的任意一个已存在时会拒绝执行，避免误删数据。
- `mongodb_day09_id_compatibility.js` 将可安全识别的数字字符串 ID 转为 long，并为历史评论补 `updated_at`；不会删除重复评论。
- `mongodb_day09_indexes.js` 增加审计文本/复合索引。只有不存在重复 user_id + item_id 评论时才创建唯一索引；否则报告并跳过，不自动删用户数据。
- 真实集成测试只重建 `scenic_ticket_test`，不会清理 `scenic_ticket`。
- Java Driver 集成测试已验证四集合、唯一/复合/文本索引、中文 UTF-8 往返、数值 ID，以及热门、用户行为、评分和审计聚合。
