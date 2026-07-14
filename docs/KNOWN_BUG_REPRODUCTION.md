# 四个已知问题复现记录

日期：2026-07-10

## 复现方法

- 静态检查当前 Service、DAO、DTO、Swing 表格模型与格式化代码。
- 使用一次性只读 JUnit probe 连接本地配置的 `scenic_ticket`，只执行 count/find/aggregate 和 MySQL SELECT；没有写入、更新、删除或初始化数据。probe 执行后已从源码树删除。
- 当前 MongoDB 数据：`item_details=20`、`comments=32`、`system_logs=31`。
- ID 类型：20/20 个详情、32/32 个评论的 `item_id` 均为数值；字符串 `item_id` 为 0。

## KB-1 评论区内容显示错误或混乱

状态：已复现并完成自动化修复验证；最终 Swing 手工冒烟测试留待 M10/M11。

证据：

```text
item1.comments=1
comment.keys=[_id, user_id, item_id, content, rating, tags, created_at]
comment.content=?????? 40???????????????
```

根因结论：

- 当前实例不是 `item_id` 数字/字符串不匹配；查询能返回评论。
- MongoDB 中 `content` 已经被写成问号，显示层自身无法还原；但本批 20 条记录仍保留 21–40 序号，且用户、景点、评分全部匹配 Git 历史中的初始化公式，可以从可信模板恢复。
- `UiFormatters.readableText` 会把多个问号替换成“历史数据编码异常，暂无法显示”，这只是降级提示，不能算数据修复。
- 原 Swing `formatComments` 只展示评分、时间、正文，遗漏用户信息和标签，并继续使用详情区同一个 JTextArea。

修复结果：评论改由 `CommentService` 和专用 DTO 读取，兼容数值/字符串 `user_id`、`item_id`；独立评论页签显示脱敏用户名、评分、正文、标签、创建时间和更新时间。Day11 修复脚本把原文档完整写入 `DATA_REPAIR_BACKUP` 审计后，仅恢复同时匹配可信初始化公式的 20 条样例正文和标签，业务库剩余问号异常为 0。未知来源乱码仍由 `ItemDisplayFormatter` 明确降级，正文与标签都不直接展示问号。

## KB-2 景点简介显示错误

状态：自动化修复与真实数据库回归已通过；最终 Swing 手工烟测待 M10/M11。

证据：

```text
item1.description=?? 1???????????????????????????????
```

根因结论：

- 当前实例的 `item_details.item_id` 全部为数值，且 `getItemDetail(1)` 能找到文档。
- `description` 在数据库中已损坏为问号；问题不是当前查询找不到文档。本批 20 条详情仍保留 1–20 ID、图片和元数据结构，并与 Git 初始化模板一一对应。
- 当前 UI 会显示通用编码异常提示，并在简介/评论/购买结果之间复用同一文本区，容易让用户误解内容来源。

修复结果：测试库 UTF-8 中文往返通过；简介改为独立页签；切换景点会清空旧简介且过时异步回调被丢弃；历史字符串 `item_id` 可读取并在保存时原位规范为数值。Day11 修复脚本在审计备份后恢复 20/20 条可信初始化简介和地址，保留图片与营业时间；业务库 `item_id=10` 已返回完整中文。无法匹配可信模板的真实数据仍不会自动猜测，需管理员依据原资料重填。

## KB-3 景点浏览页面推荐分不显示

状态：M8 已完成自动化验证；最终 Swing 手工冒烟待 M10/M11 执行。

只读证据：

```text
recommendations.count=10
recommendation=10,score=85.0,reason=根据你的浏览、评论或下单偏好推荐同类景点
recommendation=20,score=85.0,reason=根据你的浏览、评论或下单偏好推荐同类景点
```

当前代码：

- `RecommendationDTO` 保存 `score` 和 `reason`。
- `RecommendService` 产生非零分数。
- `AppFrame.fillRecommendations` 把分数写入第 6 列；普通查询路径写入 `-`。

M8 修复结果：推荐理由文本修复为 UTF-8 中文；`RecommendServiceTest` 验证热门推荐和高评分推荐均返回统一分值和中文理由；Swing 推荐表固定显示推荐分，选中概览显示推荐理由。实际桌面点击仍按最终手工冒烟清单复核。

## KB-4 系统审计条件查询无法正常使用

状态：M8 已完成自动化验证；最终 Swing 手工冒烟待 M10/M11 执行。

只读证据：

```text
audit.empty=31
audit.login=28
audit.info=25
audit.login_info=22
```

根因结论：

- `SystemLogDAO` 对 user/type/level/date 的 Document 组合本身在当前数据上可工作。
- Swing 只提供用户 ID、类型、级别；日期范围始终传 `null`。
- Service/DAO 没有关键词条件；UI 没有 limit 输入和“清空条件”动作。
- 当前测试只验证 limit 归一化，没有验证构建出的 MongoDB 多条件过滤器。

M8 修复结果：已引入 `AuditLogQuery`；服务/DAO 支持用户、类型、级别、起止时间、关键词和 limit；UI 增加日期、关键词、limit 与清空条件。`SystemLogServiceTest`、`StatisticsServiceTest` 和真实 MongoDB 组合查询已验证。

## 验收纪律

- KB-1、KB-2 已有真实数据复现证据，但修复后必须在 `scenic_ticket_test` 重新验证，不能直接修改生产名数据库。
- KB-3、KB-4 已完成自动化验证；最终仍需按 M10/M11 手工冒烟清单复核 Swing 可见行为。
- KB-4 的现有条件可工作不等于目标完成；完整条件和组合测试通过后才关闭。
