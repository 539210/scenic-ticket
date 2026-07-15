# Swing 前端页面说明

## 完成范围

在 Day 01 到 Day 09 后端模块基础上，完成可演示并可测试的 Java Swing 前端页面。前端入口仍为：

```text
src/main/java/com/scenicticket/Main.java
```

主窗口页面：

```text
src/main/java/com/scenicticket/ui/AppFrame.java
```

`AppFrame` 仅保留窗口、权限、服务装配和业务对话框编排；登录/注册、首页、档案、景点、订单、后台、报表、审计及各业务弹窗均位于独立组件中。数据库按钮路径统一通过 `SwingTaskRunner` 后台执行，并使用会话代次、同名最新请求和忙碌遮罩防止旧回调或重复提交。

## 页面清单

| 页面 | 主要功能 | 后端服务 |
| --- | --- | --- |
| 登录页 | 系统入口，登录成功后进入主系统 | `UserService#login` |
| 注册页 | 从登录页跳转注册新用户，注册成功后返回登录页 | `UserService#register` |
| 首页 | 展示当前账号、账号类型和快捷入口 | 本地 UI 状态 |
| 个人档案 | 保存用户真实姓名、证件号、地址、个人简介 | `UserService#updateProfile` |
| 景点浏览 | 按预设关键词/数据库分类查询景点；概览、简介、评论使用独立页签，过时异步详情不会覆盖新选择；评论支持评分、正文和原位修改 | `BusinessService`、`CrossDatabaseQueryService`、`RecommendService`、`CommentService` |
| 我的订单 | 按订单号和状态查询订单，显示票种/日期/价格快照；专用按钮执行支付、待支付取消和退款 | `BusinessService`、`OrderLifecycleService` |
| 后台管理 | 景点、分类、用户和票种/库存独立页签；核销入口不在当前界面展示 | `BusinessService`、`AdminUserService`、`TicketInventoryService` |
| 统计报表 | 以表格展示月度订单、带景点名称的热门排行、用户报告和综合汇总 | `StatisticsService` |
| 系统审计 | 以中文表格展示日志明细、审计汇总、趋势和用户操作汇总，支持用户/类型/级别/日期/关键词/条数组合查询 | `SystemLogService` |

普通用户登录后只显示用户侧页面：`首页`、`个人档案`、`景点浏览`、`我的订单`、`统计报表`。推荐入口已合并到 `景点浏览` 页。管理员登录后显示完整页面，额外包含 `后台管理` 和 `系统审计`。

注册页会校验用户名、密码、邮箱和手机号。手机号必须是 11 位大陆手机号；输入非法时界面会用中文提示具体字段。

景点门票原价和优惠减免比例由管理员维护。`0` 表示无优惠，`20` 表示减免 20%（按原价的 80% 售票）。用户界面同时展示原价、优惠、折后单价和实付总额，避免把原价误认为折后价。

## 新增/调整的后端接口

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `BusinessService` | `listCategories` | 前端后台管理页展示分类 |
| `BusinessService` | `updateItemStatus` | 前端后台管理页控制景点上下架 |
| `BusinessService` | `listUserOrders` | 前端订单页查询用户订单 |
| `BusinessService` | `updateOrderStatus` | 旧任意状态接口已停用并 fail-closed |
| `BusinessService` | `updateItemPricing` | 前端后台管理页维护景点票价和折扣 |
| `BusinessService` | `updateCategory` / `updateItem` | 校验分类树并维护分类、景点名称和所属分类 |
| `BusinessService` | `getItemDetailForAdmin` / `updateItemDetail` | 管理员读取并更新简介、图片地址和 JSON 扩展属性 |
| `BusinessService` | `canComment` | 兼容旧只读资格提示；不是提交授权边界 |
| `CommentService` | `submit` / `listForItem` | 提交时重查支付资格，唯一新建/修改评论，并组合脱敏用户与完整展示字段 |
| `BusinessService` | `searchOrderViews` | 联表查询订单及景点名称，避免用户依赖景点 ID |
| `BusinessService` | `searchAllItemsForAdmin` | 管理员同时查看上架和下架景点 |
| `AdminUserService` | `searchUsers` / `getUserDetail` | 管理员条件查询用户并查看档案、订单和行为概况 |
| `AdminUserService` | `changeUserStatus` / `changeUserRole` | 事务化用户启禁和角色维护，保护自身及最后有效管理员 |
| `AuthorizationService` | `requireActiveUser` / `requireAdmin` | 在服务层回查 actor 状态和角色，不依赖 UI 隐藏按钮 |
| `TicketInventoryService` | `createTicketType` / `updateTicketType` | 管理多票种价格、优惠和上下架状态 |
| `TicketInventoryService` | `setTotalStock` / `listAvailable` | 行锁维护每日库存并向用户展示未来可售票种和日期 |
| `OrderLifecycleService` | `createPendingOrder` / `pay` | 创建待支付订单并预留库存，主动支付后把预留转为已售 |
| `OrderLifecycleService` | `cancelPending` / `refund` / `expireDueOrders` | 合法取消、退款、过期释放以及库存恢复 |
| `AdmissionService` | `admit` / `listByOrder` | 管理员部分/全部核销、完成状态和核销记录查询 |

## 运行方式

推荐在项目根目录直接运行：

```powershell
.\start-system.cmd
```

如果数据库已经启动，也可以只启动 Swing 前端：

```powershell
.\start-app.cmd
```

手动 Maven 启动方式：

```powershell
$env:JAVA_HOME='<JDK_21_HOME>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn exec:java
```

若 Maven 未加入 PATH，可设置 `MAVEN_HOME`，或用 `SCENIC_MAVEN_CMD` 指向本机的 `mvn.cmd` 后运行 `start-app.cmd`。

也可以在 IntelliJ IDEA 中直接运行 `com.scenicticket.Main`。

## 初始账号

初始化脚本内置管理员账号：

```text
用户名：kongsc
密码：ksc123456
```

应用启动后先显示登录页，登录成功才进入系统主界面。注册页通过登录页下方的“注册新账号”按钮进入，注册成功后会清空输入并返回登录页。

## 验证记录

```text
Compiling 102 source files with javac [debug release 21]
Compiling 58 test source files with javac [debug release 21]
Default: Tests run: 158, Failures: 0, Errors: 0, Skipped: 2
Real DB: Tests run: 172, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 说明

M11 已在隔离 MySQL/MongoDB 中复跑普通用户和管理员完整服务/数据库流程，并对所有独立 Swing 组件及文档一致性执行自动测试。真实窗口布局、滚动、快速连点、断库恢复和完整鼠标点击流程仍按 `docs/M11_SWING_MANUAL_ACCEPTANCE.md` 手工冒烟，不把自动测试伪报为视觉通过。
