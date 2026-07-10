use("scenic_ticket");

const allowedDatabases = ["scenic_ticket", "scenic_ticket_test"];
if (!allowedDatabases.includes(db.getName())) {
  throw new Error(`Refusing index migration for unexpected database: ${db.getName()}`);
}

db.action_logs.createIndex({ user_id: 1, action_type: 1, created_at: -1 });
db.action_logs.createIndex({ item_id: 1, created_at: -1 });
db.item_details.createIndex({ item_id: 1 }, { unique: true });
db.system_logs.createIndex({ user_id: 1, log_type: 1, log_level: 1, timestamp: -1 });
db.system_logs.createIndex({ message: "text" }, { default_language: "none" });
db.comments.createIndex({ item_id: 1, created_at: -1 });

const duplicateComments = db.comments.aggregate([
  { $group: { _id: { user_id: "$user_id", item_id: "$item_id" }, count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } },
  { $limit: 1 }
]).toArray();

if (duplicateComments.length === 0) {
  db.comments.createIndex({ user_id: 1, item_id: 1 }, { unique: true });
  print("Created unique comment index for user_id + item_id");
} else {
  print("Skipped unique comment index because duplicate comment pairs exist; no documents were deleted");
}
