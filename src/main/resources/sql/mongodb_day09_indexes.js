const allowedDatabases = ["scenic_ticket", "scenic_ticket_test"];
if (!allowedDatabases.includes(db.getName())) {
  throw new Error(`Refusing index migration for unexpected database: ${db.getName()}`);
}
print(`Updating MongoDB indexes for database: ${db.getName()}`);

db.action_logs.createIndex({ user_id: 1, action_type: 1, created_at: -1 });
db.action_logs.createIndex({ item_id: 1, created_at: -1 });
db.item_details.createIndex({ item_id: 1 }, { name: "uq_item_details_item_id", unique: true });
db.system_logs.createIndex(
  { user_id: 1, log_type: 1, log_level: 1, timestamp: -1 },
  { name: "idx_system_logs_audit" }
);
db.system_logs.createIndex(
  { message: "text" },
  { name: "idx_system_logs_message_text", default_language: "none" }
);
db.comments.createIndex({ item_id: 1, created_at: -1 }, { name: "idx_comments_item_time" });

const duplicateComments = db.comments.aggregate([
  { $group: { _id: { user_id: "$user_id", item_id: "$item_id" }, count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } },
  { $limit: 1 }
]).toArray();

if (duplicateComments.length === 0) {
  db.comments.createIndex(
    { user_id: 1, item_id: 1 },
    { name: "uq_comments_user_item", unique: true }
  );
  print("Created unique comment index for user_id + item_id");
} else {
  print("Skipped unique comment index because duplicate comment pairs exist; no documents were deleted");
}
