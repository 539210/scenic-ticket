# scenic-ticket

景点售票系统，数据库系统开发集训项目第一版工程骨架。

## 环境要求

- JDK 21
- Maven 3.8+
- MySQL 8.0.45
- MongoDB 8.3.2
- Git + Gitee

## 技术栈

- Java 21
- Swing
- JDBC
- MySQL Connector/J
- MongoDB Java Sync Driver
- HikariCP
- jBCrypt
- SLF4J + Logback
- JUnit 5

## 项目结构

```text
scenic-ticket/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/scenicticket/
│   │   └── resources/
│   │       ├── db.properties.example
│   │       ├── logback.xml
│   │       └── sql/
│   └── test/java/com/scenicticket/
├── docs/
└── README.md
```

## 本地配置

仓库只提交 `src/main/resources/db.properties.example`。本地运行前复制为：

```text
src/main/resources/db.properties
```

真实数据库账号密码只写入 `db.properties`，该文件已加入 `.gitignore`，不得提交到 Gitee。

默认数据库名称：

- MySQL：`scenic_ticket`
- MongoDB：`scenic_ticket`

## 数据库脚本

SQL 和 MongoDB 初始化脚本统一放在：

```text
src/main/resources/sql/
```

Day 02 已补充具体建表、索引、视图、存储过程、触发器、初始化数据和 MongoDB 初始化脚本。
Day 07 增加了索引优化脚本和 MongoDB 聚合索引补充脚本。

MySQL 建议按以下顺序执行：

```text
mysql -uroot -p < src/main/resources/sql/mysql_schema.sql
mysql -uroot -p < src/main/resources/sql/mysql_init_data.sql
mysql -uroot -p < src/main/resources/sql/mysql_views.sql
mysql -uroot -p < src/main/resources/sql/mysql_procedures.sql
mysql -uroot -p < src/main/resources/sql/mysql_triggers.sql
mysql -uroot -p < src/main/resources/sql/mysql_day07_optimization.sql
mysql -uroot -p < src/main/resources/sql/mysql_day08_pricing_update.sql
mysql -uroot -p < src/main/resources/sql/mysql_day09_migration_baseline.sql
mysql -uroot -p < src/main/resources/sql/mysql_day09_ticket_types_inventory.sql
mysql -uroot -p < src/main/resources/sql/mysql_day09_order_lifecycle_refunds_admissions.sql
```

MongoDB 初始化：

```text
mongosh src/main/resources/sql/mongodb_init.js
mongosh src/main/resources/sql/mongodb_day07_optimization.js
mongosh src/main/resources/sql/mongodb_day09_id_compatibility.js
mongosh src/main/resources/sql/mongodb_day09_indexes.js
```

`mongodb_init.js` 只用于空数据库；检测到受管集合已经存在时会拒绝执行。已有数据库必须使用 Day09 迁移脚本，不能通过重新运行初始化脚本清空数据。

MySQL 全新安装和 Day08 升级可在隔离测试库验证（只允许操作 `scenic_ticket_test`）：

```powershell
.\scripts\verify-mysql-test-database.ps1 -Reset
.\scripts\verify-mysql-upgrade.ps1 -Reset
```

## 版本控制规范

- 主分支：`main`
- 提交格式：`[Day XX] 功能描述`
- 每天至少提交一次
- 功能节点必须提交
- `main` 分支保持可运行

Day 01 初始提交信息：

```text
[Day 01] 初始化项目结构与需求设计文档
```

## 运行说明

当前已完成项目骨架、数据库脚本、DAO 基础类、用户模块、核心业务模块、订单事务、MongoDB 日志/详情 DAO、推荐与跨库联查、统计报表与系统审计模块、性能优化、Swing 前端页面，以及 Day 08 单元测试、事务回滚测试和压力测试。安装并配置 Maven 后，可使用 Maven 编译和测试：

```text
mvn test
```

启动 Swing 应用：

```text
.\start-system.cmd
```

Swing 前端采用统一浅色业务风格，包含登录入口、独立注册页、首页、个人档案、景点浏览、我的订单、后台管理、统计报表和系统审计。后台管理包含景点、分类和用户管理；用户管理支持条件查询、档案与概况查看、启禁和角色维护。景点推荐入口已合并到景点浏览页，购票与评论使用独立确认窗口，统计和审计结果使用中文表格展示。
普通用户登录后只显示用户侧页面；管理员登录后显示完整页面，并额外包含后台管理和系统审计。
管理员权限同时在服务层校验；退出会写入审计日志并隔离旧异步任务，账号切换后不会回写上一会话的数据。
后台分类支持修改父级并阻止循环；景点支持名称、分类、价格、状态、简介、图片地址和 JSON 扩展属性维护。用户侧的概览、简介和评论使用独立页签，历史字符串 `item_id` 详情/评论可兼容读取并在编辑时规范化。评论提交由服务层重查已支付未退款订单，同一用户对同一景点再次提交会修改原评论；列表显示脱敏用户、评分、正文、标签和创建/更新时间。
后台“票种与库存”支持多票种价格/优惠/状态及每日总库存维护；用户可按未来日期范围查看可售票种、折后价和剩余数量。库存写入采用 MySQL 事务和行锁，管理员不能把总库存调到已预留与已售数量之下。
购票会创建保留 15 分钟库存的待支付订单，不会直接显示已支付；用户在“我的订单”主动确认模拟支付。待支付订单可取消，已支付且未到游玩日期、未完成、未核销的订单可申请模拟退款，退款记录、订单状态和库存恢复在同一 MySQL 事务完成。
管理员可在“后台管理 → 门票核销”按订单 ID 和数量分批核销游玩日期当天的已支付门票；全部数量核销后订单自动完成。已有任何核销记录的订单均不能退款。
景点门票原价和优惠减免比例由管理员维护，界面同时展示原价、优惠、折后单价和实付总额；普通用户购买时只填写票数并选择模拟付款方式。

初始化管理员账号：

```text
用户名：kongsc
密码：ksc123456
```

`UserDAOTest` 默认跳过数据库集成测试；初始化本地 MySQL 数据库后，可显式开启：

```text
mvn test -DintegrationTests=true
```

真实集成测试必须显式覆盖为测试数据库；代码中的保护器会拒绝其他数据库名：

```powershell
mvn clean test -DintegrationTests=true `
  -Dscenic.ticket.mysql.url=jdbc:mysql://localhost:3306/scenic_ticket_test `
  -Dscenic.ticket.mongodb.database=scenic_ticket_test
```

Day 08 压力测试默认跳过；需要执行 10000 条日志 + 50 并发测试时，可显式开启：

```text
mvn test -DstressTests=true
```

## 当前进度

- Day 01：项目结构、需求规格说明、MySQL E-R 图、MongoDB 集合设计。
- Day 02：MySQL/MongoDB 初始化脚本、DAO 基础结构、`UserDAO`。
- Day 03：用户注册登录、权限判断、用户档案维护、分类/景点/订单 DAO、订单事务、景点详情与日志 MongoDB DAO。
- Day 04：行为日志模块、评论管理、MongoDB 聚合管道、统计服务初版。
- Day 05：推荐功能、跨数据库联查服务、推荐结果 DTO 和跨库详情 DTO。
- Day 06：数据统计报表、MySQL 月度订单存储过程调用、MongoDB 系统操作审计聚合。
- Day 07：索引与 SQL 优化、批量日志写入、输入安全校验、Swing 系统集成入口、性能优化报告和安全检查清单。
- Day 08：补充 JUnit 单元测试、订单事务回滚测试、批量日志压力测试和测试报告。
- Swing 前端：补齐可演示页面，覆盖登录入口、独立注册、景点浏览、推荐入口、订单、评论、后台管理、统计和审计。
