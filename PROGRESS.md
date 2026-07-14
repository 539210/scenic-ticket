# 景点售票系统进度记录

更新时间：2026-07-14（Asia/Shanghai）

## 当前状态

- 当前里程碑：M10 真实数据库集成验证已完成；下一里程碑为 M11 答辩稳定版
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
- M1 检查点已完成：`8d72476 [Day 09] 完善数据库迁移与测试隔离`。

## M2 用户、会话和权限进展（2026-07-11）

- 新增数据库回查型 `AuthorizationService`；管理员写操作、系统审计、系统级统计以及用户私有数据查询均在服务层校验 actor、启用状态和角色，不再依赖 Swing 隐藏入口。
- 新增管理员用户管理服务和 Swing 页签，支持按用户名、邮箱、角色、状态查询，查看档案、订单概况和行为概况，以及启禁账号和修改角色。
- 用户状态/角色修改使用 MySQL 事务与行锁；禁止管理员禁用自己、修改自己的角色或禁用/降级最后一个有效管理员。
- 登录、注册和退出审计采用明确的跨库降级策略：MySQL 核心结果不因 MongoDB 日志失败而伪装失败；界面显示审计警告。
- 退出和账号切换递增会话代次，旧 `SwingWorker` 回调不能回写新会话；退出写入 `LOGOUT` 审计。
- M2 真实集成套件显式使用 MySQL/MongoDB `scenic_ticket_test`：66 tests，0 failures，0 errors，1 skipped（仅 opt-in 压力测试）。
- M2 默认 Java 21 `mvn clean test`：61 tests，0 failures，0 errors，2 skipped（真实数据库测试和 opt-in 压力测试）；`BUILD SUCCESS`。
- M2 检查点已完成：`0847777 [Day 09] 加固权限与用户管理`。

## M3 分类、景点和详情起始审计（2026-07-11）

- 分类 DAO 已有新增、查询、子级查询和更新，但服务/UI 仅暴露新增与平铺列表，尚缺修改、树形展示、父级存在性与循环保护。
- 景点 DAO 已有新增、查询、定价和状态更新，尚需核对名称/分类修改、关联有效性和 MongoDB 详情写入失败的一致性策略。
- `item_details` 当前只按数值 `item_id` 查询；需补充历史字符串 ID 兼容读取/迁移验证，并确保简介、图片和扩展属性独立展示。
- 已知简介问题的历史数据根因是 MongoDB 中文内容已损坏为问号；M3 将以 UTF-8 测试数据、兼容查询和切换清空回归验证修复路径，不伪造原始文本恢复。

## M3 分类、景点和详情验收（2026-07-11）

- 分类新增和修改均校验父级存在性，拒绝自身父级、后代父级和已有损坏循环；后台列表展示完整层级路径。
- 景点后台可修改名称、分类、票价、优惠、上下架、简介、图片地址和 JSON 扩展属性；更新名称/分类会保留原价格、优惠和状态。
- 用户侧将景点概览、景点简介和游客评论拆为独立页签；切换景点会清空旧内容，异步详情回调仅能更新发起时仍被选中的景点。
- `DetailDAO` 优先读取数值 `item_id` 并兼容历史字符串 ID；再次保存时原位规范为数值 ID，避免产生第二份详情。
- 新建景点的 MongoDB 详情写入失败时，服务会尝试立即把 MySQL 景点下架并返回明确恢复提示，避免无详情景点继续售卖。
- 默认 Java 21 `mvn clean test`：67 tests，0 failures，0 errors，2 skipped；真实 MySQL/MongoDB `scenic_ticket_test` 完整套件：74 tests，0 failures，0 errors，1 skipped。
- M3 检查点已完成：`8e93e69 [Day 09] 完善分类景点与详情管理`。

## M4 票种、日期和库存起始审计（2026-07-11）

- `ticket_types`、`ticket_inventory`、订单 `visit_date` 和价格快照字段已由 M1 迁移创建，但 Java 主代码尚无对应 Model、DAO、Service 或 Swing 管理页。
- 每日库存约束已防止负数和账面总量超界，但应用层仍需事务与 `SELECT ... FOR UPDATE`/等效条件更新，证明并发预留不会超卖。
- M4 将先完成票种与库存维护和未来日期可售查询；订单创建、支付、过期释放和退款恢复在 M5 统一接入同一库存事务 API。

## M4 票种、日期和库存验收（2026-07-11）

- 新增 `TicketType`、`TicketInventory`、对应 JDBC DAO、`TicketInventoryService` 和可售 DTO；票价、优惠、状态、日期范围和库存边界均在服务层校验。
- 管理员后台新增“票种与库存”页，可按景点维护票种名称/价格/优惠/上下架，并按票种和日期查询、创建或调整每日总库存。
- 普通用户可在景点页查询未来日期范围内仍有库存的上架票种，查看原价、优惠、折后价和可售数量。
- 库存总量调整使用 MySQL 事务和 `SELECT ... FOR UPDATE`；不得小于已预留加已售数量，可售量由服务重新计算，不接受 UI 直接提交。
- 并发测试首次发现“事务持有连接后另借连接校验票种”会耗尽 HikariCP；已改为在同一事务连接中完成票种校验和库存行锁。
- 真实 20 线程争抢 10 张库存：成功 10、失败 10、最终可售 0、预留 10、无负库存且总量平衡。
- 默认 Java 21 `mvn clean test`：71 tests，0 failures，0 errors，2 skipped；真实 MySQL/MongoDB `scenic_ticket_test` 完整套件：79 tests，0 failures，0 errors，1 skipped。
- M4 检查点已完成：`4fae212 [Day 09] 实现票种与并发库存管理`。

## M5 订单、支付和退款起始审计（2026-07-11）

- 现有 `BusinessService.createOrder` 仍按景点单价创建并直接写状态 1，未保存票种、游玩日期、过期时间和完整价格快照，是 BUG-P1-001 的直接根因。
- `OrderDAO.updateStatus` 可任意写 0..3，未锁订单、未校验合法转换，也未同步库存，是 BUG-P0-002 的直接根因。
- M1 已提供订单生命周期字段与 `refunds` 表；M4 已提供同连接库存行锁原语。M5 将建立独立订单生命周期服务，使创建/支付/取消/过期/退款与库存变化处于同一 MySQL 事务。

## M5 订单、支付和退款实现与主要验收（2026-07-11）

- 新增 `OrderLifecycleService`：创建订单只进入待支付并预留库存，保存票种名称、原价、优惠、折后单价、数量、总额、游玩日期、付款方式和 15 分钟过期时间快照。
- 用户必须在“我的订单”主动确认支付；支付把同一日期库存从已预留转为已售。待支付取消/过期把预留恢复为可售，支付后取消必须走模拟退款。
- 模拟退款仅允许游玩日期之前、已支付、未完成、未退款、未核销订单；同一事务写 `refunds`、订单取消/退款时间并把已售库存恢复为可售。
- 旧 `BusinessService.createOrder` 和任意 `updateOrderStatus` 已 fail-closed；Swing 只暴露创建待支付、确认支付、取消待支付、申请退款四类业务动作。
- 真实测试库通过待支付→支付→退款、待支付→取消、待支付→过期全流程；逐步断言订单快照、状态时间、退款记录以及 available/reserved/sold 三个库存桶。
- 过期扫描发现迁移前的待支付订单没有对应预留；兼容策略是锁定并取消订单但不增加库存，避免凭空造库存。新订单的取消/支付仍严格要求预留存在。
- MySQL 提交后 Mongo 审计失败返回“业务成功、审计失败”警告，不回滚或诱导重复支付/退款。
- 默认 Java 21 `mvn clean test`：76 tests，0 failures，0 errors，2 skipped。
- 完整真实 MySQL/MongoDB `scenic_ticket_test` 套件最终重跑：85 tests，0 failures，0 errors，1 skipped；包含退款边界、真实生命周期、库存并发及跨库审计降级。
- M5 实现与验收已完成，计划检查点为 `[Day 09] 完成订单支付退款状态机`。
- M5 检查点已完成：`ff81022 [Day 09] 完成订单支付退款状态机`。

## M6 入园核销起始审计（2026-07-11）

- `admissions` 表和外键/数量约束已由 M1 创建，但 Java 主代码尚无 Admission Model、DAO、Service 或 Swing 管理页。
- M5 退款已通过查询 `admissions` 阻止已核销订单退款；M6 需补齐仅管理员、仅已支付、仅有效游玩日期、部分/全部数量核销以及全部核销后订单完成的原子事务。

## M6 入园核销验收（2026-07-11）

- 新增 `Admission`、`AdmissionDAO`、`AdmissionService` 和管理员“门票核销”页；核销记录保存订单、数量、操作员、时间和备注。
- 服务先锁定订单，要求管理员、已支付未完成、未退款且游玩日期等于当天；累计核销数量不得超过订单数量。
- 支持多次部分核销；最后一张核销后同一事务把订单从已支付变为已完成并写 `completed_at`。
- MySQL 核销提交后写 MongoDB `system_logs` 审计；审计失败不回滚核销，返回明确警告。
- 真实流程：3 张票支付后先核销 1 张，退款被拒；再核销 2 张后订单完成，退款继续被拒；两条核销记录均可查询。
- 默认 Java 21 `mvn clean test`：80 tests，0 failures，0 errors，2 skipped；完整真实测试库套件：90 tests，0 failures，0 errors，1 skipped。
- M6 检查点已完成：`2f15d07 [Day 09] 实现门票核销与完成流程`。

## M7 评论和景点互动起始审计（2026-07-11）

- `CommentDAO.addComment` 永远插入，未使用已存在的唯一索引执行更新，也未保存 `updated_at`。
- `BehaviorLogService.addComment` 只校验 ID/内容/评分，不在服务层重新校验有效已支付订单，直接调用可绕过 UI 的 `canComment`。
- 用户侧评论仍以纯文本显示，缺脱敏用户名、标签和更新时间；历史中文问号内容不可逆，M7 只保证新写/更新 UTF-8 往返并明确旧数据限制。

## M7 评论和景点互动验收（2026-07-11）

- 新增 `CommentService` 与评论 DTO；提交时回查当前 actor 为有效用户、景点存在，并在服务层重新确认该用户有该景点已支付/完成且未退款订单，旧可绕过发布接口已 fail-closed。
- 每用户每景点使用兼容数值/字符串 ID 的原位 upsert；首次保存写创建/更新时间，再次提交保留 `created_at` 并更新正文、评分、标签和 `updated_at`，唯一索引并发冲突重试为更新。
- 用户侧评论保留独立页签；列表组合 MySQL 用户名并脱敏，完整显示评分、正文、标签、创建时间和更新时间。评论弹窗支持中英文逗号分隔标签。
- MongoDB 评论成功但行为日志失败时，保存结果不回滚，界面收到明确审计警告，避免用户重复提交。
- 真实跨库测试以测试订单完成待支付→支付，随后验证首次中文评论、再次原位修改、兼容记录唯一、UTF-8 往返、标签/双时间戳、脱敏展示和评分聚合，并清理临时数据。
- 默认 Java 21 `mvn clean test`：83 tests，0 failures，0 errors，2 skipped；完整真实 MySQL/MongoDB `scenic_ticket_test` 套件：94 tests，0 failures，0 errors，1 skipped。
- 已知评论显示 BUG-P2-001、重复插入 BUG-P2-005、字段缺失 BUG-P2-006 均完成自动化验证；历史已损坏为问号的正文不可逆，保留明确降级提示，不伪造恢复。
- Swing 审计警告对话框、状态保留及混合数字/字符串 `item_id` 统一评分摘要均已纳入最终复跑。2026-07-12 默认套件再次通过：83 tests，0 failures，0 errors，2 skipped；完整真实 `scenic_ticket_test` 套件再次通过：94 tests，0 failures，0 errors，1 skipped。
- M7 实现与验收已完成，检查点为 `[Day 09] 完善评论资格与展示`；提交并同步远端后进入 M8。

## M8 推荐、统计和审计起始审计（2026-07-12）

- 推荐服务已有统一 0-100 分值，但缺少推荐理由回归断言；景点浏览表格只显示推荐分，理由依赖选中概览展示。
- 统计热门排行直接展示 MongoDB `_id`，没有与 MySQL `items` 主数据合并，导致答辩演示只能看到景点 ID。
- 系统审计查询服务和 UI 缺关键词、日期、limit 和清空条件；汇总、趋势、用户操作不复用日期范围。
- `sp_update_inactive_items`、`v_user_profile`、`v_item_order_summary` 存在于脚本中，但此前没有系统或测试层实际调用证据。

## M8 推荐、统计和审计验收（2026-07-12）

- 新增 `AuditLogQuery`，`SystemLogDAO`/`SystemLogService` 支持用户、类型、级别、起止日期、关键词、limit 组合查询；反向日期范围在服务层拒绝。
- 系统审计 Swing 页新增开始日期、结束日期、关键词、条数和清空条件；明细、汇总、趋势、用户操作刷新均使用当前日期范围。
- 新增 `HotItemRankingDTO`，`StatisticsService.getHotItemRanking` 将 MongoDB 热门聚合与 MySQL `items` 主数据合并；UI 显示景点名称、ID、状态和热度指标，缺失主数据时明确显示“景点不存在或已删除”。
- 推荐理由文本修复为 UTF-8 中文，并用单元测试验证热门推荐和高评分推荐的分数与理由。
- `ReportDAO` 实际调用 `sp_update_inactive_items`，并提供两个视图 `v_user_profile`、`v_item_order_summary` 的查询入口；真实集成测试在事务内验证第二存储过程会下架无订单景点并回滚测试改动。
- 默认 Java 21 `mvn -q test` 通过：87 tests，0 failures，0 errors，2 skipped；完整真实 MySQL/MongoDB `scenic_ticket_test` 套件通过：100 tests，0 failures，0 errors，1 skipped。
- BUG-P2-003、BUG-P2-004、BUG-P2-009、BUG-P2-010 的自动化验证已完成；最终仍需在 M10/M11 进行 Swing 手工冒烟确认。

## M9 Swing 重构与巡检切片（2026-07-12）

- 抽出 `SwingTaskRunner` 统一执行 Swing 后台任务，保留会话代次校验，避免退出/切换账号后的旧异步回调继续更新界面。
- `AppFrame` 不再内联 `SwingWorker`、忙碌计数和错误分支；后续仍需按页面继续拆分，`AppFrame` 过大的结构问题尚未完全关闭。
- 新增忙碌 glass pane，后台任务运行期间消费鼠标、滚轮和键盘事件，降低重复点击导致的重复提交风险；真实桌面手工点击仍留到 M10/M11。
- `UiFormatters.chineseError` 从单一“检查数据库连接”调整为数据库、权限和通用失败三类中文提示，同时继续保留业务校验消息。
- 默认 Java 21 `mvn clean test` 已通过：88 tests，0 failures，0 errors，2 skipped。
- 继续抽出 `OrderTableModels`，把“我的订单”表格列定义、列宽和行填充从 `AppFrame` 移出；自动测试固定普通用户/管理员表格均显示票种和游玩日期，防止答辩演示关键字段回退。默认 Java 21 `mvn -q test` 通过：90 tests，0 failures，0 errors，2 skipped。
- 新增 `LatestTaskGuard` 并接入 `SwingTaskRunner`：同一会话中同名查询/刷新连续发起时，仅最新请求可以更新成功、失败或中断状态，防止旧查询后完成覆盖新结果；不同任务名互不影响。默认 Java 21 `mvn -q test` 通过：92 tests，0 failures，0 errors，2 skipped。
- 新增 `OrderActionPolicy`：订单表格选中后仅对订单本人启用合法操作，待支付订单启用支付/取消，已支付且游玩日期未到的订单启用退款，已取消、已完成、已到游玩日或他人订单均不启用误导性按钮；服务层仍执行最终资格校验。默认 Java 21 `mvn -q test` 通过：95 tests，0 failures，0 errors，2 skipped。
- 新增 `UiInputParsers`，集中 ID、整数、金额、ISO 日期、审计起止日期和可选文本解析；`AppFrame` 仅保留薄委托。测试发现并明确 `java.util.Date`/Mongo 日期的毫秒精度，结束日期按当天 `23:59:59.999` 包含。默认 Java 21 `mvn -q test` 通过：99 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `HomePanel`：首页账户/角色摘要、普通用户快捷入口、管理员后台/审计入口和刷新状态均由独立组件维护；`AppFrame` 删除首页专用字段、刷新方法及遗留卡片/导航辅助方法。默认 Java 21 `mvn -q test` 通过：101 tests，0 failures，0 errors，2 skipped；`AppFrame` 当前仍有 2777 行，继续保留为 M9 进行中项。
- 完整抽出 `ProfilePanel`，通过可注入 `UiTaskExecutor` 复用统一异步执行；加载、表单绑定、空档案清理、保存和消息状态均移出 `AppFrame`。删除未展示的冗余 `userIdField`，保存对象始终绑定当前会话用户 ID。默认 Java 21 `mvn -q test` 通过：103 tests，0 failures，0 errors，2 skipped；`AppFrame` 从 2777 行降至 2726 行。
- 完整抽出 `ScenicBrowsePanel`，将关键词/类型筛选、推荐列表、选中概览、简介/评论异步回写保护及购票按钮状态集中到独立组件；新增自动测试固定类型与折后价展示、上下架购买权限、推荐分 0–100 边界及推荐理由。分类缓存更新明确回到 EDT 成功回调，避免后台线程修改 Swing 共享状态；`AppFrame` 从 2726 行降至 2578 行。默认 Java 21 `mvn -q test` 通过：105 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `OrderPanel`，将普通用户/管理员查询、表格选择、支付/取消/退款按钮状态和生命周期刷新集中到独立组件；页面测试覆盖普通用户查询强制绑定本人、管理员不可操作他人订单、待支付操作组合、退款原因传递及点击后切换选择的订单号快照。测试同时发现管理员留空用户 ID 时条件表达式会把 `null` 隐式拆箱并抛出 NPE，现改为显式分支并固定空筛选回归；输入解析仍在统一后台任务内执行，以保留中文错误处理。`AppFrame` 从 2578 行降至 2483 行。默认 Java 21 `mvn -q test` 通过：109 tests，0 failures，0 errors，2 skipped。
- 修复启动可移植性：`start-app.cmd` 删除本机 Zulu/IntelliJ 绝对路径，按 `SCENIC_JAVA_HOME`、标准 `JAVA_HOME`、PATH 中 `javac` 发现 JDK 21，并按 `SCENIC_MAVEN_CMD`、Maven Wrapper、`MAVEN_HOME`、PATH 发现 Maven；新增 `--check` 环境诊断模式。已分别用标准变量和显式覆盖实跑通过，自动测试禁止启动脚本重新引入机器绝对路径。README/需求规格统一声明 MySQL 8.0+、MongoDB 5.0+，本机精确版本只作为历史验证环境。默认 Java 21 `mvn -q test` 通过：111 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `AuditPanel`，将用户/类型/级别/日期/关键词/条数组合筛选、清空重查、明细/汇总/趋势/用户操作四个结果页及“刷新当前结果”路由移出 `AppFrame`。页面测试固定结束日期包含到 `23:59:59.999`、日志类型/级别中文展示、清空后的无筛选语义，以及汇总页刷新不误切回明细；已知 BUG-P2-004 从服务层验证补强为 UI 组合查询自动化验证。`AppFrame` 从 2483 行降至 2308 行。默认 Java 21 `mvn -q test` 通过：114 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `ReportPanel`，将月报、热门排行、本人/管理员用户报告、综合汇总和当前页刷新移出 `AppFrame`；页面测试固定热门景点名称/ID/状态展示、刷新不切页、普通用户报告强制本人、管理员目标用户及空月报/空用户报告明确状态。删除表格化后无调用的旧报表、审计和通用文档文本渲染器，避免双套展示逻辑漂移；`AppFrame` 从 2308 行降至 1809 行。默认 Java 21 `mvn -q test` 通过：117 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `AdmissionPanel`，将订单 ID/核销数量/备注快照、记录查询和成功后同订单刷新移出 `AppFrame`；页面测试固定数量、操作员、时间、空备注展示，核销参数传递及空记录明确状态。修复刷新记录覆盖“核销成功/审计警告”等业务结果消息的问题，刷新完成后保留服务返回文案；`AppFrame` 从 1809 行降至 1787 行。默认 Java 21 `mvn -q test` 通过：120 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `TicketInventoryPanel`，将票种查询/选择回填/新增/更新、日期范围库存查询和总库存维护移出 `AppFrame`；页面测试固定票种更新按钮状态、原价/优惠解析、票种 ID 联动、库存查询日期快照及 total/available/reserved/sold/version 行展示。新增/更新/保存后的刷新保留业务结果消息，不再被“已加载”覆盖；`AppFrame` 从 1787 行降至 1679 行。默认 Java 21 `mvn -q test` 通过：123 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `UserManagementPanel`，将用户名/邮箱/角色/状态筛选、表格选择、详情加载、启禁和角色变更移出 `AppFrame`；页面测试固定完整 `UserSearchCriteria`、当前管理员本人按钮禁用、其他用户目标 ID/结果消息，以及 Mongo 行为数据不可用时继续展示 MySQL 用户详情。服务层仍以事务行锁执行最后管理员和并发资格校验；`AppFrame` 从 1679 行降至 1532 行。默认 Java 21 `mvn -q test` 通过：126 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `ManagementPanel`，组合景点、分类、用户、票种库存和核销五个后台页；景点/分类页面测试固定分类树和缓存回调、景点详情异步回写保护、选择后按钮状态、空查询提示、名称分类/价格优惠参数，以及分类父级 CRUD 与结果消息。新增分类自选父级的 UI 快速拒绝，服务层继续验证完整循环；删除旧后台实现和专用死代码，`AppFrame` 从 1532 行降至 1281 行。默认 Java 21 `mvn -q test` 通过：130 tests，0 failures，0 errors，2 skipped。
- 完整抽出 `LoginPanel` 与 `RegisterPanel`，将认证字段、行内结果、失败状态和页面导航从 `AppFrame` 移出，并扩展 `UiTaskExecutor` 的可选错误回调以继续复用统一后台执行器。密码提交后立即清空 Swing 字段并擦除临时字符数组；页面测试覆盖成功登录、禁用账号、服务异常、注册成功/失败和返回登录。删除认证专用死代码后 `AppFrame` 从 1281 行降至 1167 行。默认 Java 21 `mvn clean test` 通过：136 tests，0 failures，0 errors，2 skipped。
- 抽出 `PurchaseDialogPanel` 与 `CommentDialogPanel`：购票选项切换会按当前可售库存动态限制数量，确认时冻结票种/日期/数量/支付方式；评论确认时冻结正文、评分和中英文逗号标签。巡检发现调价窗口期成功页仍显示加载时的客户端估价，订单数据库金额虽正确但 UI 可能不一致；新增 `PendingOrderResult` 返回服务端实际总额、票种名、日期和数量快照，成功页只以该结果为准，并明确将弹窗金额标为“下单前估算”。`AppFrame` 从 1167 行降至 1117 行。默认 Java 21 `mvn clean test` 通过：142 tests，0 failures，0 errors，2 skipped。
- 抽出 `CreateItemDialogPanel` 与 `TicketAvailabilityDialogPanel`：新增景点表单冻结完整分类、简介、价格、优惠、图片和 JSON 快照；可售查询默认未来 1–14 天，在发起后台查询前拒绝过期、反向或非法日期，结果表固定七个只读业务列并提供明确空状态。`UiInputParsers` 新增图片行去空/去重和 metadata JSON 解析，新增与编辑入口共用；表单解析异常在 EDT 转为可见中文错误。同步删除主窗体旧表单/表格辅助方法、旧评论渲染器和未使用的 `BehaviorLogService`，`AppFrame` 从 1117 行降至 885 行。默认 Java 21 `mvn clean test` 通过：149 tests，0 failures，0 errors，2 skipped。
- 抽出 `ItemDisplayFormatter`，独立固定景点类型/原价/优惠/折后价/状态/简介/图片/metadata，以及评论脱敏用户、评分、正文、标签、创建/更新时间和评分摘要。退出审计发现专用评论 DTO 渲染曾绕过 `readableText`，历史 `????????` 会直接显示，与 BUG-P2-001 文档不一致；现改为明确的“历史数据编码异常，暂无法显示”，并对缺详情、旧 Map metadata、空评论和完整中文字段补回归。`AppFrame` 从 885 行降至 788 行，仅保留窗口、权限、服务装配和对话框编排。默认 Java 21 `mvn clean test` 通过：153 tests，0 failures，0 errors，2 skipped。

## M9 退出结论（2026-07-13）

- 所有主要页面、后台子页、认证页、业务弹窗、输入解析、表格模型、按钮策略和展示格式化均已形成可测试组件；`AppFrame` 不再承载页面业务状态。
- UI 层只有 `AppFrame` 装配服务，所有数据库查询/写入按钮路径均进入统一 `SwingTaskRunner`；`SwingWorker` 只存在于该执行器。
- 会话切换、同名请求竞态、重复点击遮罩、订单操作资格、输入边界、空结果、Mongo 审计失败和已知四个显示问题均有自动/静态证据。
- `UI_ACTION_MATRIX` 中无法在当前环境真实点击验证的快速连点、断库恢复、滚动视觉和重登全流程仍明确标为 M10/M11 手工项，没有伪造“手工通过”。
- M9 退出条件满足；下一步严格进入 M10，重建隔离测试库并执行 MySQL、MongoDB、跨库及普通用户/管理员完整流程。

## M10 真实数据库集成验收（2026-07-14）

- 安全脚本三次重建且只允许 `scenic_ticket_test`；本地业务库 `scenic_ticket` 未被操作，凭据未打印或提交。
- MySQL 全新安装通过：10 表、12 外键、2 视图、2 存储过程、2 触发器、4 条迁移、60 个票种、420 条库存，非法库存为 0。
- Day08 旧结构升级通过：历史用户、档案、订单和课程原字段保留；升级后 10 表、3 票种、21 条库存及全部数据库对象完整。
- 新增 `RoleBusinessFlowIntegrationTest`，在同一真实测试库串联管理员建分类/景点/票种/库存、普通用户注册/登录/档案/浏览/下单/支付/评论/退款/重登，以及管理员用户管理/订单查询/部分与全部核销/统计/月报/审计/退出。
- 端到端流程最后再次登录并重新查询档案、退款订单、已完成订单和评论，证明服务与数据库层持久化一致；普通用户直接调用管理员接口被拒绝。
- Java 21 默认套件：153 tests，0 failures，0 errors，2 skipped；完整真实 MySQL/MongoDB/跨库/并发套件：167 tests，0 failures，0 errors，1 skipped；两轮均为 `BUILD SUCCESS`。
- P0/P1 在最终真实套件复核后由 VERIFIED 转为 CLOSED；普通用户和管理员完整流程停止条件已具备真实自动化证据。
- `mongosh` 不在当前环境，Mongo JS 脚本仍只声明 Java Driver 空库等效验证；真实 Swing 快速连点、断库恢复、滚动布局和完整点击重登继续保留到 M11，未伪报手工通过。
- M10 退出条件满足；详细证据见 `docs/M10_REAL_DATABASE_ACCEPTANCE.md`。下一步进入 M11 文档一致性、手工桌面冒烟、五分钟演示、答辩问答和最终报告。

## M11 答辩稳定版收尾（2026-07-14）

- README、需求规格、业务规则、数据库迁移、MySQL E-R、MongoDB 集合、Swing 页面和用户手册均按当前实现校准；新增 `DocumentationConsistencyTest` 固定启动入口、隔离测试库命令、页面按钮策略、实际表/字段/对象、四集合/索引以及真实迁移脚本名。
- 新增 `M11_SWING_MANUAL_ACCEPTANCE.md`、`FIVE_MINUTE_DEMO.md`、`DEFENSE_QA.md` 与 `FINAL_REPORT.md`；未把自动化证据伪报为真实桌面点击结果。
- Java 21 最终默认 `mvn clean test`：158 tests，0 failures，0 errors，2 skipped，`BUILD SUCCESS`。
- 隔离 MySQL 空库安装再次通过：10 表、12 外键、2 视图、2 存储过程、2 触发器、60 票种、420 库存、4 迁移记录、非法库存 0。
- Day08 旧结构升级再次通过：历史用户、档案、订单保留，升级后 10 表、3 票种、21 库存及全部数据库对象完整；随后再次重建空测试库作为最终回归基线。
- Java 21 最终真实 MySQL/MongoDB/跨库/并发与双角色完整套件：172 tests，0 failures，0 errors，1 skipped，`BUILD SUCCESS`；唯一跳过项为需显式开启的 Fake DAO 压力测试。
- 审计发现四个 Mongo JS 脚本硬编码 `use("scenic_ticket")`，从测试 URI 执行仍可能切到业务库；已删除内部切库，统一校验连接 URI 当前数据库并登记关闭 `BUG-P1-008`。
- 官方 `mongosh 2.9.2` 已在空 `scenic_ticket_test` 顺序实跑初始化、Day07 优化及两个 Day09 脚本：4 集合、290 条样例、索引数 7/6/3/7，评论重复组 0，唯一索引成功。
- 当前只剩由可见桌面执行者完成 Swing 布局、滚动、快速连点、断库和完整重登手工清单；在补证前 M11 与 Goal 保持进行中。
