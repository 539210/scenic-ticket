use("scenic_ticket");

db.action_logs.createIndex({ created_at: -1, action_type: 1, item_id: 1 });
db.comments.createIndex({ rating: -1, item_id: 1 });
db.system_logs.createIndex({ timestamp: -1, log_type: 1, log_level: 1 });
