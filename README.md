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

当前第一版只放置占位文件，后续 Day 02 完成具体建表、索引、视图、存储过程、触发器和初始化数据。

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

第一版只完成工程骨架，暂不包含完整业务功能。后续完成数据库脚本和 DAO 后，可使用 Maven 编译和测试：

```text
mvn test
```
