# Bug Backlog

更新时间：2026-07-14

状态：`OPEN` / `IN PROGRESS` / `BLOCKED` / `FIXED` / `VERIFIED` / `CLOSED`

## P0

| ID | 状态 | 问题 | 复现证据/根因 | 验收 |
| --- | --- | --- | --- | --- |
| BUG-P0-001 | CLOSED | 普通调用者可绕过管理员权限执行管理操作 | 管理写接口已强制接收 actor 并经 `AuthorizationService` 回查启用状态和 ADMIN 角色；统计、审计、私有数据查询同步收紧 | 直接服务调用越权测试及真实测试库管理员操作通过；M10 完整真实套件复核关闭 |
| BUG-P0-002 | CLOSED | 订单状态可被任意改写，允许非法转换 | 任意状态接口已 fail-closed；状态只可由支付、待支付取消、过期、退款和后续核销专用事务转换 | 已取消再支付、已支付直接取消、过期支付、完成/核销/过期退款均被拒绝；M10 完整真实套件复核关闭 |

## P1

| ID | 状态 | 问题 | 复现证据/根因 | 验收 |
| --- | --- | --- | --- | --- |
| BUG-P1-001 | CLOSED | 创建订单直接变为已支付 | 旧创建接口已停用；新流程创建状态 0、预留库存并设置 15 分钟过期，用户主动支付后才状态 1 | M10 双角色真实流程再次通过待支付→主动支付并复核关闭 |
| BUG-P1-002 | CLOSED | 无票种、游玩日期和每日库存，无法防超卖 | M4 行锁库存已接入 M5 待支付订单同一事务；票种/日期/数量与价格均由服务回查 | M10 真实并发、双角色流程及逐桶库存断言通过并复核关闭 |
| BUG-P1-003 | CLOSED | 无退款与库存恢复 | `refunds` DAO、退款资格、订单锁和已售→可售库存恢复在同一事务 | M10 双角色真实流程执行退款，边界套件拒绝过期、完成、核销和重复退款，复核关闭 |
| BUG-P1-004 | CLOSED | 无门票核销 | 已新增记录 DAO、管理员事务服务和 Swing 页；订单锁控制日期、状态和累计数量 | M10 双角色真实流程完成部分/全部核销、自动完成和持久化复核，关闭 |
| BUG-P1-005 | CLOSED | MySQL 提交成功后 Mongo 日志失败会向用户显示整体失败 | 生命周期先提交 MySQL，再安全写 Mongo；失败返回带审计警告的成功结果并记录 WARN | M10 完整真实套件及 Mongo 异常回归确认 MySQL 不回滚，复核关闭 |
| BUG-P1-006 | CLOSED | 真实集成测试没有隔离测试数据库 | 已增加配置覆盖和 `DatabaseTargetGuard`；完整集成套件显式使用两个 `scenic_ticket_test` | M10 三次安全重建、升级及 167 项真实套件均只指向隔离库，复核关闭 |
| BUG-P1-007 | CLOSED | Mongo 初始化脚本无保护地 drop 四个集合 | 全新安装脚本检测已有集合并拒绝；升级使用非破坏 Day09 脚本 | Java Driver 与 M11 官方 `mongosh` 空测试库初始化均通过，既有集合拒绝策略保留 |
| BUG-P1-008 | CLOSED | Mongo JS 脚本硬编码 `use("scenic_ticket")`，从测试 URI 执行仍会切到业务库 | 四个脚本删除内部数据库切换，改为校验连接 URI 当前数据库，只允许 `scenic_ticket`/`scenic_ticket_test`；README 命令显式指定 URI | 官方 `mongosh 2.9.2` 仅在空 `scenic_ticket_test` 顺序执行初始化、Day07 优化和两个 Day09 迁移；4 集合、290 条样例与 7/6/3/7 个索引验证通过 |
| BUG-P1-009 | CLOSED | Mongo 脚本默认索引名与 Java 初始化显式索引名冲突，脚本安装后评论真实集成失败 | 初始化与 Day09 脚本统一使用 `uq_comments_user_item`、`uq_item_details_item_id`、`idx_comments_item_time` 等稳定名称，重复建索引保持幂等 | 失败由 `CommentIntegrationTest` 真实复现为错误码 85；空库重跑四脚本后同一 172 项真实套件通过 |
| BUG-P1-010 | CLOSED | 业务库仍为 Day08 结构，普通用户查询订单时报数据库操作失败 | 只读诊断确认 `scenic_ticket.orders` 缺少 11 个 Day09 字段且 5 张配套表未部署；先完整备份，再顺序执行 6 个非破坏性迁移脚本；界面现在会把 MySQL 1054/1146 明确提示为“数据库结构未升级” | 迁移前后均为 28 笔订单、总金额 17519.90、13 个用户和 20 个景点；新版字段无缺失，`kscksc` 的应用完整订单查询返回 3 行 |

## P2

| ID | 状态 | 问题 | 复现证据/根因 | 验收 |
| --- | --- | --- | --- | --- |
| BUG-P2-001 | VERIFIED | 已知：评论区内容显示错误或混乱 | 根因确认是 20 条旧初始化样例的 `content/tags` 已损坏为问号；其序号、用户、景点和评分完整匹配 Git 中可信初始化公式，Day11 脚本备份后恢复原模板；其他未知来源乱码仍只降级、不伪造 | 业务库 20 条样例正文/标签恢复后剩余异常 0；UTF-8 新建/更新、唯一性、脱敏用户、标签、双时间戳、真实聚合及标签降级测试通过 |
| BUG-P2-002 | VERIFIED | 已知：景点简介显示错误 | 根因确认是 20 条旧初始化详情的 `description` 和地址已损坏为问号；Day11 脚本按保留的 1–20 ID 与 Git 初始化模板备份后恢复，仍保留图片和营业时间 | 业务库 20/20 条简介和地址恢复后剩余异常 0；`item_id=10` 返回完整中文简介与地址，UTF-8、图片/元数据及真实跨库 CRUD 测试通过 |
| BUG-P2-003 | VERIFIED | 已知：推荐分不显示 | 推荐服务返回非零分和理由；Swing 推荐表固定显示推荐分，选中景点概览显示推荐理由；乱码推荐理由已修复为 UTF-8 中文 | `RecommendServiceTest` 验证热门/高评分分数和理由；M8 默认与真实库套件通过，最终 Swing 手工冒烟保留到 M10/M11 |
| BUG-P2-004 | VERIFIED | 已知：系统审计条件查询不能正常使用 | 新增 `AuditLogQuery`；服务/DAO 支持用户、类型、级别、日期、关键词、limit 组合；独立 `AuditPanel` 维护四类结果与当前页刷新 | 服务/真实 MongoDB 组合查询及页面级全筛选、清空、日期全天边界、汇总刷新测试均通过；最终桌面视觉冒烟保留到 M10/M11 |
| BUG-P2-005 | VERIFIED | 评论规则允许同一用户对同一景点重复插入 | 改为兼容历史字符串 ID 的原位 upsert，保留 `created_at`、更新 `updated_at`；唯一索引并发冲突会重试为更新 | 单元测试及真实 MongoDB 测试证明再次评论更新原记录且兼容记录总数仍为 1 |
| BUG-P2-006 | VERIFIED | 评论展示缺用户信息、标签和更新时间 | 新增评论列表 DTO，将用户表脱敏用户名与 Mongo 评论字段组合；评论使用独立页签 | 自动测试验证脱敏用户名、评分、正文、标签、创建/更新时间完整，真实 UTF-8 往返通过 |
| BUG-P2-007 | VERIFIED | 管理员用户管理完全缺失 | 独立 `UserManagementPanel` 支持查询、详情、档案、订单/行为概况、启禁和角色管理；事务行锁保护自身与最后管理员规则 | 服务/真实库测试及页面级筛选、选择、本人保护、变更消息与 Mongo 降级测试通过 |
| BUG-P2-008 | VERIFIED | 退出没有写 LOGOUT 审计，异步任务可能跨会话回写 | 退出调用 `UserService.logout` 写 LOGOUT；`SessionTaskGuard` 在退出/登录切换时使旧异步回调失效 | Mongo 日志失败降级测试、会话代次回归测试和 M2 完整套件通过 |
| BUG-P2-009 | VERIFIED | 热门排行只显示景点 ID | `StatisticsService` 将 Mongo 热门聚合与 MySQL 主数据合并为 `HotItemRankingDTO`，独立 `ReportPanel` 固定名称/ID/状态/热度列 | 服务测试覆盖缺失主数据降级；页面测试覆盖真实名称、状态及刷新保持热门页签 |
| BUG-P2-010 | VERIFIED | 第二个存储过程和两个视图未在系统/测试中证明实际用途 | `ReportDAO` 现在实际调用 `sp_update_inactive_items`，并查询 `v_user_profile`、`v_item_order_summary` | `ReportDatabaseObjectsIntegrationTest` 在 `scenic_ticket_test` 事务内验证两个视图和第二存储过程，测试后回滚 |
| BUG-P2-011 | VERIFIED | 同一会话连续刷新可能由较慢旧请求覆盖较快新请求 | `SwingTaskRunner` 现按任务名获取 `LatestTaskGuard` 代次，成功/失败/中断回调均只接受最新代次 | `LatestTaskGuardTest` 验证同名新请求使旧请求失效，且不同任务名互不干扰；真实 Swing 连续刷新仍待 M10/M11 |
| BUG-P2-012 | VERIFIED | 订单表格选择任意订单都会同时启用支付、取消和退款按钮 | 新增 `OrderActionPolicy`，按当前 actor、订单所有者、状态和游玩日期计算按钮可用性；服务层资格校验继续保留 | `OrderActionPolicyTest` 覆盖本人待支付、本人可/不可退款、他人订单、已取消和已完成订单 |
| BUG-P2-013 | VERIFIED | 管理员订单查询留空用户 ID 时发生 `null` 隐式拆箱并抛出 NPE | 独立 `OrderPanel` 使用显式管理员/普通用户分支，管理员空用户 ID 保持为无筛选，普通用户始终绑定当前会话 ID | `OrderPanelTest` 覆盖管理员空筛选、普通用户强制本人及生命周期按钮联动 |
| BUG-P2-014 | VERIFIED | 点击订单生命周期操作后、后台任务执行前切换选择可能使操作读取到另一订单 ID | 支付、取消和退款均在点击事件中冻结订单 ID，后台任务只使用请求快照 | 延迟执行测试在点击支付后切换至另一行，确认服务仍收到点击时订单 ID |
| BUG-P2-015 | VERIFIED | 管理员在购票弹窗打开期间调价时，订单按服务端新价落库但成功页仍显示旧客户端估价 | `PendingOrderResult` 返回事务实际保存的金额及票种/日期/数量快照；弹窗明确标注“下单前估算”，成功页只展示服务端实际金额 | 服务测试固定返回快照等于订单快照；`PurchaseDialogPanelTest` 使用与估价不同的服务端金额验证成功页显示服务端值 |
| BUG-P2-016 | VERIFIED | 专用评论 DTO 渲染绕过历史乱码降级，与 BUG-P2-001 验收记录不一致 | `ItemDisplayFormatter` 对简介和评论正文统一使用 `UiFormatters.readableText`，不伪造已损坏内容，也不直接展示问号串 | 展示测试输入 `????????`，断言输出“历史数据编码异常，暂无法显示”且不包含原问号正文 |

## P3

| ID | 状态 | 问题 | 说明 |
| --- | --- | --- | --- |
| BUG-P3-001 | CLOSED | 启动脚本曾硬编码本机路径；删除后又无法发现未进 PATH 的 IntelliJ 内置 Maven | 支持标准 `JAVA_HOME`/`MAVEN_HOME`/PATH、Maven Wrapper和 `SCENIC_*` 覆盖，并通过独立 PowerShell 帮助器从 Windows 卸载注册表/标准安装目录动态发现 IntelliJ Maven；不写死版本或盘符。原环境直接执行 `start-app.cmd --check` 与 `start-system.cmd --check` 均通过，158 项默认回归通过 |
| BUG-P3-002 | VERIFIED | `AppFrame` 过大 | M9 已抽出完整后台、登录/注册、全部业务弹窗、九个业务页面组件和 `ItemDisplayFormatter`；`AppFrame` 从页面巨石降至 788 行，仅保留窗口、权限、服务装配和对话框编排 |
| BUG-P3-003 | VERIFIED | 文档数据库版本写成 MySQL 8.0.45 / MongoDB 8.3.2 | README 与需求规格已改为 MySQL 8.0+ / MongoDB 5.0+；快捷启动文档明确精确版本仅为本机验证记录，自动测试固定主要求文档 |
| BUG-P3-004 | VERIFIED | 错误提示过度归一为“检查数据库连接” | `UiFormatters.chineseError` 已区分数据库、权限和通用失败，并保留业务校验消息；`UiFormattersTest` 覆盖分类文案 |
| BUG-P3-005 | VERIFIED | 核销成功后自动刷新记录会覆盖服务返回的成功或审计降级消息 | `AdmissionPanel` 在刷新同订单记录后恢复本次业务结果文案，普通查询仍显示记录数/空结果 | 页面测试确认核销参数、同订单刷新及最终状态仍为“核销成功” |
| BUG-P3-006 | VERIFIED | 票种新增/更新和库存保存后的自动刷新覆盖业务结果消息 | `TicketInventoryPanel` 使用点击时参数快照刷新同一景点/票种日期范围，并在刷新后保留创建编号、更新或库存可售结果 | 页面测试覆盖票种 CRUD、库存保存、刷新参数及最终状态消息 |
| BUG-P3-007 | VERIFIED | 用户管理允许当前管理员点击自禁用/自降权，且变更结果会被列表刷新消息覆盖 | `UserManagementPanel` 对当前会话本人禁用两个变更按钮；其他目标冻结 ID，并在刷新后保留服务结果 | 页面测试覆盖本人保护、其他用户状态/角色目标及最终消息；最后管理员规则仍由服务事务校验 |
| BUG-P3-008 | VERIFIED | 分类编辑允许把当前分类直接选为自己的上级，只能等待服务报错 | `ManagementPanel` 更新前拒绝 `parentId == categoryId`，服务层仍负责跨多级循环检测 | 页面测试覆盖分类选择/父级 CRUD，服务测试覆盖直接与间接循环 |
| BUG-P3-009 | VERIFIED | 景点简介把图片 URL 当普通文字展示，用户无法直接查看图片 | 简介文本不再输出图片地址；`ScenicBrowsePanel` 在后台加载第一张有效 HTTP/HTTPS 图片，限制超时和 10 MB 响应并按 360×220 区域等比缩放；空地址、失败和非图片响应隐藏整个图片区 | `ScenicBrowsePanelTest` 覆盖直显和失败隐藏，`ScenicImageLoaderTest` 覆盖缩放、空地址及不支持协议，`ItemDisplayFormatterTest` 断言文本不含 URL |

## 已知四问题复现纪律

- 不把最近提交或静态代码看起来已修复当作验证完成。
- 每个问题必须记录：输入数据、操作步骤、失败现象、根因、失败测试、修复提交、相关测试、完整测试和需要的 Swing 手工步骤。
- 四个用户已知问题均已完成自动化验证；最终答辩前仍需按 M10/M11 手工冒烟清单复核 Swing 可见行为。
