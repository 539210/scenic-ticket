# Day 08 测试与压力测试报告

## 目标

Day 08 按项目要求完成单元测试补充、事务回滚测试、10000 条日志 + 50 并发压力测试，并对影响可测性的代码做小范围重构。

## 本次代码重构

| 文件 | 调整 |
| --- | --- |
| `src/main/java/com/scenicticket/util/ConnectionProvider.java` | 新增连接提供器函数接口，便于测试中注入可观测连接 |
| `src/main/java/com/scenicticket/service/BusinessService.java` | `createOrder` 默认仍使用 `MySQLDBUtil`，测试可注入连接提供器验证事务行为 |
| `src/main/java/com/scenicticket/service/BatchLogService.java` | 批量导入前过滤空日志，返回真实导入数量，避免统计值包含无效文档 |

## 单元测试补充

| 测试类 | 覆盖内容 |
| --- | --- |
| `SecurityUtilTest` | 文本、邮箱、IP、分页参数校验 |
| `UserServiceTest` | 注册、重复用户名、登录成功/失败、禁用用户、档案更新、管理员判断 |
| `BehaviorLogServiceTest` | 浏览、搜索、评论、查询参数校验和字段归一化 |
| `SystemLogServiceTest` | 系统审计日志字段校验、IP 归一化、查询 limit 限制 |
| `BatchLogServiceTest` | 批量分批、空日志过滤、默认批大小、最大批大小限制 |
| `BusinessServiceTransactionTest` | 订单创建成功提交、插入失败回滚、非法参数不打开连接 |
| `BatchLogServiceStressTest` | 10000 条行为日志 + 50 并发导入压力测试 |

`UserDAOTest` 仍保持数据库集成测试定位，默认跳过，避免普通 `mvn test` 强依赖本机 MySQL 初始化状态。

## 事务回滚测试

测试入口：

```text
src/test/java/com/scenicticket/service/BusinessServiceTransactionTest.java
```

验证点：

- 成功路径：`setAutoCommit(false)` -> `OrderDAO#create` -> `commit()` -> 恢复 `setAutoCommit(true)` -> 关闭连接 -> 写入 MongoDB 行为日志。
- 失败路径：`OrderDAO#create` 抛出 `SQLException` 后触发 `rollback()`，不执行 `commit()`，不写入行为日志，并向上抛出 `DBException`。
- 非法参数路径：用户 ID、景点 ID、金额非法时直接抛出 `BusinessException`，不会获取数据库连接。

## 压力测试设计

测试入口：

```text
src/test/java/com/scenicticket/service/BatchLogServiceStressTest.java
```

压测参数：

| 参数 | 值 |
| --- | --- |
| 日志总数 | 10000 |
| 并发线程 | 50 |
| 每线程日志 | 200 |
| 每批大小 | 100 |
| 预期批次数 | 100 |
| 测试类型 | 服务层并发压测，使用线程安全 Fake DAO，避免污染本机 MongoDB 数据 |

## 验证环境

| 项目 | 结果 |
| --- | --- |
| Java | Zulu JDK 21.0.8 |
| Maven | IntelliJ IDEA bundled Maven 3.9.9 |
| 操作系统 | Windows |
| 时间 | 2026-07-08 |

## 默认测试命令

```powershell
$env:JAVA_HOME='D:\zulu21.44.17-ca-jdk21.0.8-win_x64\zulu21.44.17-ca-jdk21.0.8-win_x64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& "D:\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" test
```

结果：

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 2
BUILD SUCCESS
```

跳过项：

- `UserDAOTest`：需 `-DintegrationTests=true` 并初始化本地 MySQL。
- `BatchLogServiceStressTest`：需 `-DstressTests=true` 显式开启。

## 压力测试命令

```powershell
$env:JAVA_HOME='D:\zulu21.44.17-ca-jdk21.0.8-win_x64\zulu21.44.17-ca-jdk21.0.8-win_x64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& "D:\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" test -DstressTests=true
```

结果：

```text
Day08 stress test imported 10,000 logs with 50 workers in 51 ms.
Tests run: 28, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 结论

- 普通单元测试已覆盖核心服务输入校验、日志服务、批量导入、用户服务和事务行为。
- 事务回滚路径已通过可观测连接验证，确认失败时执行 `rollback()` 且不会写入后续行为日志。
- 批量日志服务可在 50 并发下完成 10000 条日志分批导入，批次大小符合预期。
- 真实 MySQL/MongoDB 端到端验证仍需本机数据库启动并导入初始化脚本后执行。
