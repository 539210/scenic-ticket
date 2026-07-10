# 四个已知问题复现记录

日期：2026-07-10

## 复现方法

- 静态检查当前 Service、DAO、DTO、Swing 表格模型与格式化代码。
- 使用一次性只读 JUnit probe 连接本地配置的 `scenic_ticket`，只执行 count/find/aggregate 和 MySQL SELECT；没有写入、更新、删除或初始化数据。probe 执行后已从源码树删除。
- 当前 MongoDB 数据：`item_details=20`、`comments=32`、`system_logs=31`。
- ID 类型：20/20 个详情、32/32 个评论的 `item_id` 均为数值；字符串 `item_id` 为 0。

## KB-1 评论区内容显示错误或混乱

状态：已复现（数据内容损坏）；UI 完整字段仍不合格。

证据：

```text
item1.comments=1
comment.keys=[_id, user_id, item_id, content, rating, tags, created_at]
comment.content=?????? 40???????????????
```

根因结论：

- 当前实例不是 `item_id` 数字/字符串不匹配；查询能返回评论。
- MongoDB 中 `content` 已经被不可逆地写成问号，显示层无法还原原中文。
- `UiFormatters.readableText` 会把多个问号替换成“历史数据编码异常，暂无法显示”，这只是降级提示，不能算数据修复。
- 当前 Swing `formatComments` 只展示评分、时间、正文，遗漏用户信息和标签，并继续使用详情区同一个 JTextArea。

修复方向：测试库使用 UTF-8 重新导入可靠种子；增加编码/内容验证与迁移报告；评论使用独立模型/组件；无法恢复的用户内容明确标记但不得静默清空。

## KB-2 景点简介显示错误

状态：自动化修复与真实数据库回归已通过；最终 Swing 手工烟测待 M10/M11。

证据：

```text
item1.description=?? 1???????????????????????????????
```

根因结论：

- 当前实例的 `item_details.item_id` 全部为数值，且 `getItemDetail(1)` 能找到文档。
- `description` 在数据库中已损坏为问号；问题不是当前查询找不到文档。
- 当前 UI 会显示通用编码异常提示，并在简介/评论/购买结果之间复用同一文本区，容易让用户误解内容来源。

修复结果：测试库 UTF-8 中文往返通过；简介改为独立页签；切换景点会清空旧简介且过时异步回调被丢弃；历史字符串 `item_id` 可读取并在保存时原位规范为数值。管理员可同时维护简介、图片和扩展属性。历史已变成问号的文本不可逆，系统不伪造恢复内容，需依据原资料在后台重填。

## KB-3 景点浏览页面推荐分不显示

状态：当前 Service/DTO/静态 UI 路径未复现；Swing 手工验证待执行，不得关闭。

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

结论：最近提交可能已经修复旧的映射/列索引问题，但当前环境尚未完成可见 Swing 点击验收。需要增加表格模型回归测试，并由实际桌面操作验证三种推荐、刷新和切换查询后列值。

## KB-4 系统审计条件查询无法正常使用

状态：目标要求已复现为功能缺失；现有四种 Service 组合查询能返回数据。

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

修复方向：引入审计查询 DTO/可测试过滤器构建器，支持用户、类型、级别、起止时间、关键词、limit；UI 增加日期/关键词/limit/清空；补单、双、多条件和时区边界测试。

## 验收纪律

- KB-1、KB-2 已有真实数据复现证据，但修复后必须在 `scenic_ticket_test` 重新验证，不能直接修改生产名数据库。
- KB-3 在完成 Swing 手工操作前保持打开。
- KB-4 的现有条件可工作不等于目标完成；完整条件和组合测试通过后才关闭。
