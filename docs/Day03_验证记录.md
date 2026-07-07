# Day 03 验证记录

## 验证内容

| 检查项 | 结果 |
| --- | --- |
| Git 工作区基线 | 基于 Day 02 继续开发 |
| 用户模块代码 | 已完成 `UserService`、注册、登录、权限判断、档案 upsert |
| MySQL 核心 DAO | 已完成 `CategoryDAO`、`ItemDAO`、`OrderDAO`、`ProfileDAO` |
| 订单事务 | `BusinessService#createOrder` 使用 `setAutoCommit(false)`、`commit()`、异常 `rollback()` |
| MongoDB DAO | 已完成详情、评论、行为日志、系统日志基础方法 |
| DTO | 已完成 `LoginResult`、`ItemDetailDTO` |
| Maven 验证 | 使用 IntelliJ IDEA 自带 Maven 3.9.9 + Java 21 执行 `mvn test` 通过 |

## Maven 验证命令

```powershell
$env:JAVA_HOME='D:\zulu21.44.17-ca-jdk21.0.8-win_x64\zulu21.44.17-ca-jdk21.0.8-win_x64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& "D:\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" test
```

## Maven 验证结果

```text
BUILD SUCCESS
Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
```

说明：`UserDAOTest` 默认跳过数据库集成测试，避免普通 `mvn test` 强依赖本地 MySQL 数据状态。初始化数据库后，可执行：

```powershell
& "D:\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" test -DintegrationTests=true
```

## 修正记录

Maven 初次验证时，`org.mongodb:mongodb-driver-bom:5.2.0` 无法通过当前 Maven 镜像解析。已将 `pom.xml` 调整为直接给 `mongodb-driver-sync` 指定版本，验证通过。
