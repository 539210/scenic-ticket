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
| 首页 | 展示登录状态、系统模块和快捷入口 | 本地 UI 状态 |
| 登录注册 | 用户登录、用户注册 | `UserService` |
| 个人档案 | 保存用户真实姓名、证件号、地址、备注 | `UserService#updateProfile` |
| 景点浏览 | 查询景点、查看跨库详情、创建订单、发表评论 | `BusinessService`、`CrossDatabaseQueryService`、`BehaviorLogService` |
| 我的订单 | 查询用户订单、更新订单状态 | `BusinessService` |
| 后台管理 | 分类列表、新增分类、新增景点、景点上下架 | `BusinessService` |
| 推荐 | 个性化推荐、热门推荐、高评分推荐 | `RecommendService` |
| 统计报表 | 月度订单、热门排行、用户报告、仪表盘汇总 | `StatisticsService` |
| 系统审计 | 系统日志查询、审计汇总、审计趋势、用户操作汇总 | `SystemLogService` |

## 新增/调整的后端接口

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `BusinessService` | `listCategories` | 前端后台管理页展示分类 |
| `BusinessService` | `updateItemStatus` | 前端后台管理页控制景点上下架 |
| `BusinessService` | `listUserOrders` | 前端订单页查询用户订单 |
| `BusinessService` | `updateOrderStatus` | 前端订单页更新订单状态 |

## 运行方式

```powershell
$env:JAVA_HOME='D:\zulu21.44.17-ca-jdk21.0.8-win_x64\zulu21.44.17-ca-jdk21.0.8-win_x64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& "D:\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java
```

也可以在 IntelliJ IDEA 中直接运行 `com.scenicticket.Main`。

## 验证记录

```text
Compiling 41 source files with javac [debug release 21]
Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 说明

本次验证为编译级验证。完整页面操作需要本地 MySQL 和 MongoDB 已启动，并执行初始化脚本后再运行 Swing 应用。
