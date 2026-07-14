const allowedDatabases = ["scenic_ticket", "scenic_ticket_test"];
if (!allowedDatabases.includes(db.getName())) {
  throw new Error(`Refusing optimization for unexpected database: ${db.getName()}`);
}
print(`Optimizing MongoDB database: ${db.getName()}`);

db.action_logs.createIndex({ created_at: -1, action_type: 1, item_id: 1 });
db.comments.createIndex({ rating: -1, item_id: 1 });
db.system_logs.createIndex({ timestamp: -1, log_type: 1, log_level: 1 });
