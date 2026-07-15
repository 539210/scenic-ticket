# scenic-ticket

景点售票系统，面向数据库课程现场答辩的 Java Swing 桌面项目。当前版本已完成数据库迁移、完整票务生命周期、双数据库联查、权限加固、Swing 页面拆分及真实数据库端到端验收。

## 环境要求

- JDK 21
- Maven 3.8+
- MySQL 8.0+
- MongoDB 5.0+
- Git + GitHub/Gitee

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

真实数据库账号密码只写入 `db.properties`，该文件已加入 `.gitignore`，不得提交到 GitHub 或 Gitee。

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
Day 09 增加版本化迁移、票种/每日库存、待支付订单、退款和核销结构；全新安装与已有 Day08 数据升级使用不同的安全验证入口。

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
mongosh "mongodb://localhost:27017/scenic_ticket" --file src/main/resources/sql/mongodb_init.js
mongosh "mongodb://localhost:27017/scenic_ticket" --file src/main/resources/sql/mongodb_day07_optimization.js
mongosh "mongodb://localhost:27017/scenic_ticket" --file src/main/resources/sql/mongodb_day09_id_compatibility.js
mongosh "mongodb://localhost:27017/scenic_ticket" --file src/main/resources/sql/mongodb_day09_indexes.js
mongosh "mongodb://localhost:27017/scenic_ticket" --file src/main/resources/sql/mongodb_day11_repair_seed_encoding.js
```

脚本以连接 URI 中的数据库为目标，只允许 `scenic_ticket` 或 `scenic_ticket_test`，不再在脚本内部切换数据库。`mongodb_init.js` 只用于空数据库；检测到受管集合已经存在时会拒绝执行。已有数据库必须使用 Day09 迁移脚本，不能通过重新运行初始化脚本清空数据。Day11 编码修复脚本只恢复能同时匹配旧初始化序号、用户、景点和评分公式的损坏样例；执行前把原 BSON 文档写入 `DATA_REPAIR_BACKUP` 审计记录，不改写无法证明来源的真实评论。

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

当前已完成用户、分类、景点、票种/日期库存、待支付/支付/取消/退款、评论、评分推荐、统计、审计及管理员后台。历史核销数据结构和退款限制继续保留，但后台界面不提供核销入口。Swing 页面、表格、弹窗、输入解析和异步任务均已拆成可测试组件。安装并配置 Maven 后，可使用 Java 21 执行默认测试：

```text
mvn test
```

启动 Swing 应用：

```text
.\start-system.cmd
```

启动脚本优先使用标准 `JAVA_HOME`、`MAVEN_HOME` 和 PATH；Windows 上也会从注册表自动发现 IntelliJ IDEA 自带的 Maven。需要显式指定时可设置
`SCENIC_JAVA_HOME`（JDK 21 根目录）与 `SCENIC_MAVEN_CMD`（`mvn.cmd` 完整路径）；仓库不依赖开发机器的绝对安装路径。
迁移到新机器后可先运行 `.\start-app.cmd --check`，只检查 JDK 21 和 Maven，不启动图形界面。

Swing 前端采用统一浅色业务风格，包含登录入口、独立注册页、首页、个人档案、景点浏览、我的订单、后台管理、统计报表和系统审计。后台管理包含景点、分类、用户及票种/库存管理；门票核销底层能力保留，但不在当前后台界面展示入口。景点推荐入口已合并到景点浏览页，简介会在选中景点时后台预取并缓存，图片按区域缩放显示；无有效图片时隐藏图片区。评论使用独立卡片并把发表入口合并在游客评论页内；热门排行显示景点名称，系统审计支持用户、类型、级别、日期、关键词和条数组合查询，统计和审计结果使用中文表格展示。
普通用户登录后只显示用户侧页面；管理员登录后显示完整页面，并额外包含后台管理和系统审计。
管理员权限同时在服务层校验；退出会写入审计日志并隔离旧异步任务，账号切换后不会回写上一会话的数据。
后台分类支持修改父级并阻止循环；景点通过一个保存操作维护名称、分类、价格、状态、简介、拖拽导入图片及开放信息。用户侧的概览、简介和评论使用独立页签，历史字符串 `item_id` 详情/评论可兼容读取并在编辑时规范化。评论提交由服务层重查已支付未退款订单，同一用户对同一景点再次提交会修改原评论；列表显示脱敏用户、评分、正文和创建/更新时间，不保存或展示评论标签。
后台“票种与库存”支持多票种价格/优惠/状态及每日总库存维护；用户可按今天或未来日期范围查看可售票种、折后价和剩余数量。库存写入采用 MySQL 事务和行锁，管理员不能把总库存调到已预留与已售数量之下。
购票会创建保留 15 分钟库存的待支付订单，不会直接显示已支付；用户在“我的订单”主动确认模拟支付。待支付订单可取消，已支付且未到游玩日期、未完成、未核销的订单可申请模拟退款，退款记录、订单状态和库存恢复在同一 MySQL 事务完成。
当前后台界面不展示门票核销入口；底层仍保留历史核销记录以及“已核销订单不得退款”的业务校验。
景点门票原价和优惠减免比例由管理员维护，界面同时展示原价、优惠、折后单价和实付总额；普通用户购买时只填写票数并选择模拟付款方式。

如需额外生成一套演示数据，可在数据库服务启动且 `db.properties` 配置正确后运行：

```powershell
.\seed-demo-data.cmd --apply
```

该脚本默认补充 50 个普通用户、50 个上架景点、50 个已支付订单和 50 条对应评论；每个景点同时创建成人票及未来 31 天库存。脚本使用保留编号并执行冲突检查，可安全重复运行，不会覆盖管理员或游客之后修改的内容。演示账号为 `demo_user_001` 至 `demo_user_050`，统一密码为 `DemoPass123`。

初始化管理员账号：

```text
用户名：kongsc
密码：ksc123456
```

真实集成测试必须同时显式覆盖 MySQL 和 MongoDB 测试库；代码中的保护器会拒绝任何非 `scenic_ticket_test` 目标：

```powershell
mvn clean test -DintegrationTests=true `
  -Dscenic.ticket.mysql.url=jdbc:mysql://localhost:3306/scenic_ticket_test `
  -Dscenic.ticket.mongodb.database=scenic_ticket_test
```

Day 08 压力测试默认跳过；需要执行 10000 条日志 + 50 并发测试时，可显式开启：

```text
mvn test -DstressTests=true
```

2026-07-14 的 M10 验收结果：默认套件 `153 tests, 0 failures, 0 errors, 2 skipped`；真实 MySQL/MongoDB/跨库/并发套件 `167 tests, 0 failures, 0 errors, 1 skipped`。全新安装、Day08 升级和双角色完整流程见 [`docs/M10_REAL_DATABASE_ACCEPTANCE.md`](docs/M10_REAL_DATABASE_ACCEPTANCE.md)。

2026-07-15 当前代码合并后的默认回归结果为：`182 tests, 0 failures, 0 errors, 2 skipped`。本次覆盖评分推荐、评论标签移除、日期月历、拖拽图片、景点统一保存、月度订单明细钻取、报表与审计视图状态等更新；真实数据库完整套件最近一次结果仍为 `172 tests, 0 failures, 0 errors, 1 skipped`。

## 答辩与验收资料

- [用户使用手册](docs/用户使用手册.md)
- [M11 Swing 手工验收清单](docs/M11_SWING_MANUAL_ACCEPTANCE.md)
- [五分钟演示流程](docs/FIVE_MINUTE_DEMO.md)
- [常见答辩问题](docs/DEFENSE_QA.md)
- [稳定化最终报告](docs/FINAL_REPORT.md)

## 当前进度

- Day 01：项目结构、需求规格说明、MySQL E-R 图、MongoDB 集合设计。
- Day 02：MySQL/MongoDB 初始化脚本、DAO 基础结构、`UserDAO`。
- Day 03：用户注册登录、权限判断、用户档案维护、分类/景点/订单 DAO、订单事务、景点详情与日志 MongoDB DAO。
- Day 04：行为日志模块、评论管理、MongoDB 聚合管道、统计服务初版。
- Day 05：推荐功能、跨数据库联查服务、推荐结果 DTO 和跨库详情 DTO。
- Day 06：数据统计报表、MySQL 月度订单存储过程调用、MongoDB 系统操作审计聚合。
- Day 07：索引与 SQL 优化、批量日志写入、输入安全校验、Swing 系统集成入口、性能优化报告和安全检查清单。
- Day 08：补充 JUnit 单元测试、订单事务回滚测试、批量日志压力测试和测试报告。
- Day 09 / M1–M10：完成安全迁移、权限、票种库存、订单生命周期、退款核销、评论唯一性、推荐/审计修复、Swing 重构和真实数据库完整业务流验收。
- Day 11 / M11：需求、数据库设计、用户手册、演示流程、答辩问答和最终报告已校准，默认/真实数据库最终全量回归通过；真实视觉冒烟仍按验收清单收尾。
