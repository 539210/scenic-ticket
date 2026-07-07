# Day 07 安全检查清单

## 已完成

| 检查项 | 状态 | 说明 |
| --- | --- | --- |
| SQL 注入防护 | 已完成 | DAO 层继续使用 `PreparedStatement` 和 `CallableStatement` |
| 密码加密 | 已完成 | 注册密码继续使用 BCrypt 哈希 |
| 配置隔离 | 已完成 | `db.properties` 不提交，仓库只提交 `db.properties.example` |
| 输入长度限制 | 已完成 | 新增 `SecurityUtil`，限制用户名、邮箱、标题、评论、备注等长度 |
| 邮箱校验 | 已完成 | 注册邮箱使用正则格式校验 |
| IP 规范化 | 已完成 | 非法或空 IP 默认转为 `127.0.0.1` |
| 分页参数限制 | 已完成 | `limit` 和 `offset` 统一归一化，避免过大查询 |
| 系统日志审计 | 已完成 | `SystemLogService` 支持 INFO/WARN/ERROR 操作日志记录和查询 |
| 连接泄漏排查 | 已准备 | HikariCP 支持 `leakDetectionThresholdMs` 配置 |

## 重点代码

| 文件 | 作用 |
| --- | --- |
| `SecurityUtil.java` | 输入校验、邮箱校验、IP 规范化、分页参数归一化 |
| `UserService.java` | 注册/登录输入校验，密码长度控制 |
| `BusinessService.java` | 分类、景点、查询、订单参数校验 |
| `BehaviorLogService.java` | 行为日志和评论输入校验 |
| `SystemLogService.java` | 系统日志字段校验和审计入口 |

## 待 Day 08/Day 09 继续完善

- 补充 JUnit 单元测试覆盖异常输入。
- 对登录失败次数做限制或记录连续失败次数。
- Swing 管理员功能后续应按 `ADMIN` 角色控制入口。
- 答辩前检查 `db.properties`、日志文件、`target/` 未进入 Git 暂存区。
