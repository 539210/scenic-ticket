# Day 07 系统集成与验证记录

## 系统集成内容

| 模块 | 集成入口 |
| --- | --- |
| 用户管理 | Swing 登录/注册页调用 `UserService` |
| 景点查询 | Swing 景点查询页调用 `BusinessService#searchItems` |
| 跨库详情 | Swing 详情按钮调用 `CrossDatabaseQueryService#getItemDetail` |
| 下单 | Swing 下单按钮调用 `BusinessService#createOrder` |
| 评论 | Swing 评论按钮调用 `BehaviorLogService#addComment` |
| 推荐 | Swing 推荐页调用 `RecommendService` |
| 统计报表 | Swing 统计页调用 `StatisticsService` |
| 系统审计 | Swing 审计页调用 `SystemLogService` |

入口文件：

```text
src/main/java/com/scenicticket/Main.java
src/main/java/com/scenicticket/ui/AppFrame.java
```

## 验证环境

| 项目 | 结果 |
| --- | --- |
| Maven | IntelliJ IDEA bundled Maven 3.9.9 |
| Java | Zulu JDK 21.0.8 |
| 命令 | `mvn test` |
| 结果 | `BUILD SUCCESS` |

## 验证命令

```powershell
$env:JAVA_HOME='D:\zulu21.44.17-ca-jdk21.0.8-win_x64\zulu21.44.17-ca-jdk21.0.8-win_x64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& "D:\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" test
```

## Maven 验证结果

```text
Compiling 41 source files with javac [debug release 21]
Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 说明

本次完成编译级系统集成验证。`UserDAOTest` 仍默认跳过数据库集成测试。完整端到端演示需要本地 MySQL 和 MongoDB 启动，并执行初始化脚本后，通过 Swing 界面按“登录/注册 -> 景点查询 -> 详情 -> 下单/评论 -> 推荐 -> 报表 -> 审计”路径验证。
