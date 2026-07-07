# Day 03 核心业务模块说明

## 完成范围

Day 03 在 Day 01/Day 02 的工程和数据库基础上继续完善，完成用户模块、核心业务模块 MySQL 部分、MongoDB DAO、详情模块和订单模块。

## 用户模块

| 类 | 功能 |
| --- | --- |
| `UserService` | 注册、登录、管理员权限判断、用户档案维护 |
| `UserDAO` | 用户创建、按 ID 查询、按用户名查询、用户列表、联系方式更新、状态更新 |
| `ProfileDAO` | 用户档案创建、按用户查询、档案 upsert |
| `PasswordUtil` | BCrypt 密码加密和校验 |
| `SystemLogDAO` | 注册、登录等系统操作日志写入 MongoDB |

用户注册流程：

1. 校验用户名、密码、邮箱。
2. 查询用户名是否已存在。
3. 使用 BCrypt 生成密码哈希。
4. 写入 MySQL `users`。
5. 写入 MongoDB `system_logs` 注册日志。

用户登录流程：

1. 按用户名查询用户。
2. 判断账号状态。
3. 使用 BCrypt 校验密码。
4. 登录成功或失败均写入 MongoDB `system_logs`。

## 核心业务模块

| 类 | 功能 |
| --- | --- |
| `CategoryDAO` | 分类创建、查询、树形子分类查询、更新 |
| `ItemDAO` | 景点创建、按 ID 查询、多条件分页查询、更新、状态更新 |
| `OrderDAO` | 订单创建、按 ID 查询、按用户查询、状态更新 |
| `BusinessService` | 分类创建、景点创建、景点详情查询、订单创建 |

景点创建流程：

1. 写入 MySQL `items` 基础信息。
2. 使用生成的 `item_id` 写入 MongoDB `item_details`。

景点详情查询流程：

1. 从 MySQL 查询 `items` 基础信息。
2. 从 MongoDB 查询 `item_details`。
3. 从 MongoDB 查询最新评论。
4. 记录用户浏览行为到 `action_logs`。
5. 组装 `ItemDetailDTO` 返回给界面层。

订单创建流程：

1. 校验订单金额。
2. 获取 MySQL 连接。
3. 执行 `setAutoCommit(false)`。
4. 写入 `orders`。
5. `commit()` 成功后记录 MongoDB 行为日志。
6. 出现异常时执行 `rollback()`。

## MongoDB DAO

| 类 | 集合 | 功能 |
| --- | --- | --- |
| `LogDAO` | `action_logs` | 行为日志写入、用户最近行为查询 |
| `CommentDAO` | `comments` | 评论写入、按景点查询最新评论 |
| `DetailDAO` | `item_details` | 景点详情查询、详情 upsert |
| `SystemLogDAO` | `system_logs` | 系统日志写入、最近系统日志查询 |

## DTO

| 类 | 用途 |
| --- | --- |
| `LoginResult` | 封装登录成功/失败、提示信息和用户对象 |
| `ItemDetailDTO` | 封装 MySQL 景点基础信息、MongoDB 详情和评论 |

## 验收点

- 用户注册密码使用 BCrypt，不保存明文密码。
- 登录过程写入 MongoDB 系统日志。
- 所有 MySQL 查询继续使用 `PreparedStatement`。
- 订单创建使用 JDBC 显式事务。
- MySQL 基础信息和 MongoDB 扩展信息通过 `item_id` 关联。
- Day 03 代码仍在同一项目、同一 `main` 分支上迭代。
