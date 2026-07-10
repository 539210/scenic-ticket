# Swing 前端页面说明

## 完成范围

在 Day 01 到 Day 07 后端模块基础上，补齐可演示的 Java Swing 前端页面。前端入口仍为：

```text
src/main/java/com/scenicticket/Main.java
```

主窗口页面：

```text
src/main/java/com/scenicticket/ui/AppFrame.java
```

## 页面清单

| 页面 | 主要功能 | 后端服务 |
| --- | --- | --- |
| 登录页 | 系统入口，登录成功后进入主系统 | `UserService#login` |
| 注册页 | 从登录页跳转注册新用户，注册成功后返回登录页 | `UserService#register` |
| 首页 | 展示当前账号、账号类型和快捷入口 | 本地 UI 状态 |
| 个人档案 | 保存用户真实姓名、证件号、地址、个人简介 | `UserService#updateProfile` |
| 景点浏览 | 按预设关键词/数据库分类查询景点，在右侧查看详情和评论，通过“推荐”按钮选择为你推荐/热门/高分，在独立窗口完成购票和评论 | `BusinessService`、`CrossDatabaseQueryService`、`RecommendService`、`BehaviorLogService` |
| 我的订单 | 按订单号和状态查询订单，直接显示景点名称；管理员可按用户筛选并更新所选订单状态 | `BusinessService` |
| 后台管理 | 景点管理和分类管理独立页签；通过景点列表维护票价、折扣和上下架状态 | `BusinessService` |
| 统计报表 | 以表格展示月度订单、热门排行、用户报告和综合汇总 | `StatisticsService` |
| 系统审计 | 以中文表格展示日志明细、审计汇总、趋势和用户操作汇总 | `SystemLogService` |

普通用户登录后只显示用户侧页面：`首页`、`个人档案`、`景点浏览`、`我的订单`、`统计报表`。推荐入口已合并到 `景点浏览` 页。管理员登录后显示完整页面，额外包含 `后台管理` 和 `系统审计`。

注册页会校验用户名、密码、邮箱和手机号。手机号必须是 11 位大陆手机号；输入非法时界面会用中文提示具体字段。

景点票价和折扣由管理员维护。普通用户购买时只能填写票数并选择微信、支付宝或银行卡作为模拟付款方式，系统按景点固定票价和折扣自动计算实付金额，不能由用户手动定价。

## 新增/调整的后端接口

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `BusinessService` | `listCategories` | 前端后台管理页展示分类 |
| `BusinessService` | `updateItemStatus` | 前端后台管理页控制景点上下架 |
| `BusinessService` | `listUserOrders` | 前端订单页查询用户订单 |
| `BusinessService` | `updateOrderStatus` | 前端订单页更新订单状态 |
| `BusinessService` | `updateItemPricing` | 前端后台管理页维护景点票价和折扣 |
| `BusinessService` | `canComment` | 前端评论前校验用户是否购买过该景点 |
| `BusinessService` | `searchOrderViews` | 联表查询订单及景点名称，避免用户依赖景点 ID |
| `BusinessService` | `searchAllItemsForAdmin` | 管理员同时查看上架和下架景点 |

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
$env:JAVA_HOME='D:\zulu21.44.17-ca-jdk21.0.8-win_x64\zulu21.44.17-ca-jdk21.0.8-win_x64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& "D:\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java
```

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
Compiling 49 source files with javac [debug release 21]
Tests run: 43, Failures: 0, Errors: 0, Skipped: 2
BUILD SUCCESS
```

## 说明

本次验证为编译级验证。完整页面操作需要本地 MySQL 和 MongoDB 已启动，并执行初始化脚本后再运行 Swing 应用。
