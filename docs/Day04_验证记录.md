# Day 04 验证记录

## 验证环境

| 项目 | 结果 |
| --- | --- |
| Maven | IntelliJ IDEA bundled Maven 3.9.9 |
| Java | Zulu JDK 21.0.8 |
| 命令 | `mvn test` |
| 结果 | `BUILD SUCCESS` |

## 验证命令

```powershell
$env:JAVA_HOME='<JDK_21_HOME>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn test
```

## Maven 验证结果

```text
Compiling 31 source files with javac [debug release 21]
Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 待验证项

MongoDB 聚合实际数据验证需要本机配置 `mongosh` 或在 Java 集成测试中连接 MongoDB 后再执行。

## 说明

`UserDAOTest` 默认跳过数据库集成测试，所以普通 `mvn test` 不依赖 MySQL/MongoDB 服务状态。需要完整数据库集成验证时，执行：

```powershell
mvn test -DintegrationTests=true
```
