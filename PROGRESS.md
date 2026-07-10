# 景点售票系统进度记录

更新时间：2026-07-10 22:20（Asia/Shanghai）

## 当前状态

- 当前里程碑：M1 数据库安装与迁移（实现与验收完成，等待 Git 检查点）
- 当前分支：`codex/scenic-ticket-stabilization`
- 基线提交：`90f254ca42fa47a95cfe3f5b936ae7270bba10f2`
- 工作树基线：干净；未覆盖或撤销用户修改
- Java：Zulu OpenJDK `21.0.8`
- Maven：Apache Maven `3.9.9`（IntelliJ bundled Maven）
- 注意：直接执行该 Maven 时默认拾取 Java 17；必须显式设置仓库文档中的 Java 21 `JAVA_HOME`。

## 已完成工作

1. 完整读取目标文件。
2. 读取课程要求文件 `项目092_景点售票系统.docx`。目标中写的是 `docs/课程项目要求书.docx`，仓库没有该路径；根目录文件内容与课程要求相符，作为权威课程源。
3. 完整读取指定 Markdown 文档、`pom.xml`、全部 SQL/JS 数据库脚本、49 个主代码文件、13 个测试文件和启动脚本。
4. 记录 Git 最近 15 条提交和环境版本。
5. 检查数据库服务：`MySQL80`、`MongoDB` 均为 Running；本机 3306、27017 端口可达。
6. 发现本地忽略配置指向 `scenic_ticket`，因此没有执行会写数据的现有集成测试。
7. 建立本文件及计划、审计、Bug、验收、业务规则、迁移、UI 操作矩阵文件。
8. 运行只读数据库 probe：确认评论/简介中文已经在 MongoDB 中损坏为问号；确认当前 ID 类型全为数值；确认推荐 Service 返回非零分数；确认现有审计 user/type/level 组合可返回结果。
9. 完成 M0 初始审计、Bug 分级、验收清单、迁移计划与 UI 操作矩阵；不可自动验证的 Swing 点击项明确保留为手工步骤。

## 测试基线

执行命令（Java 21）：

```powershell
$env:JAVA_HOME='<JDK_21_HOME>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& '<MAVEN_HOME>\bin\mvn.cmd' clean test
```

真实结果：

```text
Compiling 49 source files with javac [debug release 21]
Compiling 13 test source files with javac [debug release 21]
Tests run: 45, Failures: 0, Errors: 0, Skipped: 2
BUILD SUCCESS
```

跳过项：

- `UserDAOTest`：仅在 `-DintegrationTests=true` 时执行，并写入配置所指向的 MySQL 数据库。
- `BatchLogServiceStressTest`：仅在 `-DstressTests=true` 时执行，使用 Fake DAO，不是真实 MongoDB 压测。

## 环境与验证限制

- `mvn` 不在 PATH，但项目文档列出的 IntelliJ Maven 路径存在。
- Maven 首次在沙箱内运行因缺少插件且网络受限失败；允许依赖下载后 Java 21 基线通过。
- LibreOffice/soffice 不可用，因此课程 DOCX 已完成结构化全文读取，但无法完成页面 PNG 视觉 QA；未虚构视觉检查结果。
- `mongosh` 不在 PATH；MongoDB 服务和端口可达。后续可通过 Java Driver 运行测试，不依赖 `mongosh`。
- 本地配置当前为 MySQL/MongoDB `scenic_ticket`，真实集成测试必须先创建隔离的 `scenic_ticket_test` 配置和数据库。

## 下一步

1. 为已知四个 Bug 建立失败测试或稳定复现夹具。
2. 完成 M0 初始缺陷分级与 UI 操作检查。
3. 进入 M1，先提供不会触碰 `scenic_ticket` 的测试库初始化/迁移机制。

详细证据见 `docs/KNOWN_BUG_REPRODUCTION.md`。

## M1 数据库安装与迁移进展（2026-07-10）

- 增加配置系统属性/环境变量覆盖，不复制或提交本机凭据。
- 增加 `DatabaseTargetGuard`，集成测试必须同时指向 MySQL/MongoDB `scenic_ticket_test`。
- 全新 schema 增加迁移记录、票种、每日库存、订单快照/生命周期、退款、核销表，课程原表/字段保留。
- 增加三份 MySQL Day09 正向迁移和两份 MongoDB Day09 兼容/索引迁移。
- 全新 MySQL 测试库验证：10 表、12 外键、2 视图、2 过程、2 触发器、60 票种、420 库存，库存违规 0。
- Day08 旧结构夹具升级验证：原用户、档案、订单保留并正确回填票种/价格快照。
- MongoDB 真实测试：四集合、索引、中文往返、数值 ID、热门/行为/评分/审计聚合通过。
- 完整 Java 21 集成套件：51 tests，0 failures，0 errors，1 skipped（Fake DAO 压力测试）。
- 默认 Java 21 套件：48 tests，0 failures，0 errors，2 skipped；真实数据库默认不参与普通单元测试。
- M1 实现与验收已完成；计划检查点为 `[Day 09] 完善数据库迁移与测试隔离`。
- 当前阻塞：工作区 `.git` 受保护，Git 暂存需要提升权限；Codex 提升操作额度已用尽并提示 2026-07-11 03:12 后重试。为遵守里程碑顺序，未在检查点前进入 M2，也未尝试绕过权限。
