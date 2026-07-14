const allowedDatabases = ["scenic_ticket", "scenic_ticket_test"];
if (!allowedDatabases.includes(db.getName())) {
  throw new Error(`Refusing initialization for unexpected database: ${db.getName()}`);
}
print(`Initializing MongoDB database: ${db.getName()}`);

const managedCollections = ["action_logs", "comments", "item_details", "system_logs"];
const existingManagedCollections = db.getCollectionNames().filter(name => managedCollections.includes(name));
if (existingManagedCollections.length > 0) {
  throw new Error(
    `Refusing destructive initialization because collections already exist: ${existingManagedCollections.join(", ")}. `
    + "Use the versioned mongodb_day09 migration scripts for an existing database."
  );
}

db.action_logs.drop();
db.comments.drop();
db.item_details.drop();
db.system_logs.drop();

db.createCollection("action_logs");
db.createCollection("comments");
db.createCollection("item_details");
db.createCollection("system_logs");

db.action_logs.createIndex({ user_id: 1, created_at: -1 }, { name: "idx_action_logs_user_time" });
db.action_logs.createIndex({ item_id: 1, action_type: 1 }, { name: "idx_action_logs_item_type" });
db.action_logs.createIndex({ created_at: -1 });
db.action_logs.createIndex({ created_at: -1, action_type: 1, item_id: 1 });

db.comments.createIndex({ item_id: 1, created_at: -1 }, { name: "idx_comments_item_time" });
db.comments.createIndex({ item_id: 1, rating: 1 });
db.comments.createIndex({ user_id: 1 });
db.comments.createIndex({ rating: -1, item_id: 1 });
db.comments.createIndex({ user_id: 1, item_id: 1 }, { name: "uq_comments_user_item", unique: true });

db.item_details.createIndex({ item_id: 1 }, { name: "uq_item_details_item_id", unique: true });
db.item_details.createIndex({ "metadata.language": 1 });

db.system_logs.createIndex({ user_id: 1, timestamp: -1 });
db.system_logs.createIndex({ log_type: 1, log_level: 1 });
db.system_logs.createIndex({ timestamp: -1 });
db.system_logs.createIndex({ timestamp: -1, log_type: 1, log_level: 1 });

const itemDetails = [];
for (let i = 1; i <= 20; i += 1) {
  itemDetails.push({
    item_id: i,
    description: `景点 ${i} 的详细介绍，包含开放时间、游览路线、购票提示和入园须知。`,
    images: [`https://example.com/scenic-${i}-1.jpg`, `https://example.com/scenic-${i}-2.jpg`],
    metadata: {
      language: "zh-CN",
      open_time: "08:00-18:00",
      address: `示例景区地址 ${i} 号`
    },
    updated_at: new Date()
  });
}
db.item_details.insertMany(itemDetails);

const comments = [];
const tags = [["环境好", "适合亲子"], ["服务好", "交通方便"], ["景色美", "拍照推荐"], ["排队少", "体验好"]];
for (let i = 1; i <= 30; i += 1) {
  comments.push({
    user_id: Math.floor((i - 1) / 20) + 1,
    item_id: ((i - 1) % 20) + 1,
    content: `第 ${i} 条评论：景区体验良好，购票流程顺畅。`,
    rating: (i % 5) + 1,
    tags: tags[i % tags.length],
    created_at: new Date(Date.now() - i * 3600 * 1000),
    updated_at: new Date(Date.now() - i * 3600 * 1000)
  });
}
db.comments.insertMany(comments);

const actionLogs = [];
const actionTypes = ["VIEW", "SEARCH", "ORDER", "COMMENT"];
for (let i = 1; i <= 120; i += 1) {
  actionLogs.push({
    user_id: (i % 10) + 1,
    item_id: (i % 20) + 1,
    action_type: actionTypes[i % actionTypes.length],
    duration_seconds: 30 + (i % 240),
    client_info: {
      client_type: "SWING",
      ip: `192.168.1.${(i % 200) + 1}`
    },
    created_at: new Date(Date.now() - i * 15 * 60 * 1000)
  });
}
db.action_logs.insertMany(actionLogs);

const systemLogs = [];
const logTypes = ["LOGIN", "LOGOUT", "ORDER_CREATE", "ITEM_UPDATE", "REPORT_VIEW"];
for (let i = 1; i <= 120; i += 1) {
  systemLogs.push({
    user_id: (i % 10) + 1,
    log_type: logTypes[i % logTypes.length],
    log_level: i % 17 === 0 ? "WARN" : "INFO",
    message: `系统操作日志 ${i}`,
    action_detail: {
      ip: `127.0.0.${(i % 200) + 1}`,
      operation: logTypes[i % logTypes.length]
    },
    timestamp: new Date(Date.now() - i * 10 * 60 * 1000)
  });
}
db.system_logs.insertMany(systemLogs);

db.action_logs.aggregate([
  { $group: { _id: "$item_id", view_count: { $sum: { $cond: [{ $eq: ["$action_type", "VIEW"] }, 1, 0] } }, total_actions: { $sum: 1 } } },
  { $sort: { total_actions: -1 } },
  { $limit: 10 }
]);
