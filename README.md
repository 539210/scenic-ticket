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

MySQL 建议按以下顺序执行：

```text
mysql -uroot -p < src/main/resources/sql/mysql_schema.sql
mysql -uroot -p < src/main/resources/sql/mysql_init_data.sql
mysql -uroot -p < src/main/resources/sql/mysql_views.sql
mysql -uroot -p < src/main/resources/sql/mysql_procedures.sql
mysql -uroot -p < src/main/resources/sql/mysql_triggers.sql
```

MongoDB 初始化：

```text
mongosh src/main/resources/sql/mongodb_init.js
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

当前已完成项目骨架、数据库脚本、DAO 基础类、用户模块、核心业务模块、订单事务和 MongoDB 日志/详情 DAO。安装并配置 Maven 后，可使用 Maven 编译和测试：

```text
mvn test
```

`UserDAOTest` 默认跳过数据库集成测试；初始化本地 MySQL 数据库后，可显式开启：

```text
mvn test -DintegrationTests=true
```

## 当前进度

- Day 01：项目结构、需求规格说明、MySQL E-R 图、MongoDB 集合设计。
- Day 02：MySQL/MongoDB 初始化脚本、DAO 基础结构、`UserDAO`。
- Day 03：用户注册登录、权限判断、用户档案维护、分类/景点/订单 DAO、订单事务、景点详情与日志 MongoDB DAO。
