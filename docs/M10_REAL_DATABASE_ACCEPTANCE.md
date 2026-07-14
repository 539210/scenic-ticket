# M10 真实数据库集成验收

验收日期：2026-07-14（Asia/Shanghai）

## 安全边界

- MySQL 与 MongoDB 仅使用 `scenic_ticket_test`。
- `verify-mysql-test-database.ps1` 和 `verify-mysql-upgrade.ps1` 均拒绝其他数据库名，并要求显式 `-Reset`。
- 未读取、打印或提交 `db.properties` 中的凭据；本地业务库 `scenic_ticket` 未被测试脚本操作。

## MySQL 全新安装

从删除并重建的 `scenic_ticket_test` 顺序执行全部全新安装、Day07、Day08 和 Day09 脚本，结果：

| 对象/数据 | 数量 |
| --- | ---: |
| 表 | 10 |
| 外键 | 12 |
| 视图 | 2 |
| 存储过程 | 2 |
| 触发器 | 2 |
| 迁移记录 | 4 |
| 票种 | 60 |
| 每日库存 | 420 |
| 非法库存记录 | 0 |

## Day08 旧库升级

旧结构夹具执行 Day09 迁移后保留 1 个历史用户、1 份历史档案和 1 张历史订单；升级后得到 10 张表、3 个票种、21 条库存、2 个视图、2 个存储过程和 2 个触发器，课程原字段和历史记录未丢失。

## Java 21 完整真实套件

```powershell
mvn clean test -DintegrationTests=true `
  -Dscenic.ticket.mysql.url=jdbc:mysql://localhost:3306/scenic_ticket_test `
  -Dscenic.ticket.mongodb.database=scenic_ticket_test
```

结果：`167 tests, 0 failures, 0 errors, 1 skipped`，`BUILD SUCCESS`。

唯一跳过项为显式开启的 Fake DAO 压力测试，不属于真实数据库功能验收。真实套件覆盖 MySQL、MongoDB、跨库联查、数据库对象、库存并发、订单事务、退款、核销、评论和管理员用户管理。

随后再次执行默认 Java 21 `mvn clean test`：`153 tests, 0 failures, 0 errors, 2 skipped`，`BUILD SUCCESS`；跳过真实数据库 opt-in 测试和 Fake DAO 压力测试符合默认配置。

## 双角色完整流程

`RoleBusinessFlowIntegrationTest` 使用唯一临时数据串联并验证：

- 管理员登录，创建分类、景点、两种票和今天/未来库存。
- 普通用户注册登录，保存档案，搜索景点并读取 MongoDB 中文详情，查询可售库存。
- 用户创建待支付订单、主动支付、查看订单、发布中文评论、退出并重登。
- 用户对未来且未核销订单退款；另一张当天订单支付后由管理员部分和全部核销。
- 管理员查询用户详情和订单，禁用/恢复账号，查看热门排行、综合统计、月报和组合审计。
- 最终再次登录，重新从数据库读取档案、退款订单、已完成订单和评论，确认数据一致。
- 普通用户直接调用管理员景点查询被服务层拒绝。
- 测试结束按外键顺序清理临时 MySQL 数据，并按用户/景点清理临时 MongoDB 数据。

## 明确保留到 M11 的项目

- 当前环境没有 `mongosh`，MongoDB JS 脚本由 Java Driver 在空隔离库等效验证；尚未声称实际客户端命令执行通过。
- 自动化已覆盖 Swing 页面组件、按钮策略、异步代次、输入和显示格式，但真实桌面的快速连点、断库恢复、滚动视觉、窗口布局和完整点击重登仍需发布前手工冒烟。
