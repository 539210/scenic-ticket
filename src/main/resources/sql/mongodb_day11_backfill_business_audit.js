const allowedDatabases = ["scenic_ticket", "scenic_ticket_test"];
if (!allowedDatabases.includes(db.getName())) {
  throw new Error(`Refusing business audit backfill for unexpected database: ${db.getName()}`);
}

const migrationId = "day11_backfill_business_audit_v1";
const mappings = {
  ORDER: { type: "ORDER_CREATE", operation: "历史购票", message: "历史购票行为（由行为日志回填）" },
  ORDER_CREATE: { type: "ORDER_CREATE", operation: "预定下单", message: "历史预定行为（由行为日志回填）" },
  ORDER_PAY: { type: "ORDER_PAY", operation: "支付购票", message: "历史支付行为（由行为日志回填）" },
  ORDER_CANCEL: { type: "ORDER_CANCEL", operation: "取消预定", message: "历史取消行为（由行为日志回填）" },
  ORDER_EXPIRE: { type: "ORDER_EXPIRE", operation: "预定过期", message: "历史过期行为（由行为日志回填）" },
  ORDER_REFUND: { type: "ORDER_REFUND", operation: "退款", message: "历史退款行为（由行为日志回填）" },
  COMMENT: { type: "COMMENT_CREATE", operation: "历史评论", message: "历史评论行为（由行为日志回填）" }
};

const sourceActions = db.action_logs.find({
  action_type: { $in: Object.keys(mappings) }
}).sort({ created_at: 1, _id: 1 }).toArray();

let inserted = 0;
let skipped = 0;
for (const source of sourceActions) {
  const sourceId = source._id.toString();
  const exists = db.system_logs.findOne({
    "action_detail.source_action_log_id": sourceId
  }, { _id: 1 });
  if (exists) {
    skipped += 1;
    continue;
  }

  const mapping = mappings[source.action_type];
  const userId = source.user_id ?? null;
  const itemId = source.item_id ?? null;
  const ip = source.client_info?.ip ?? "127.0.0.1";
  db.system_logs.insertOne({
    user_id: userId,
    log_type: mapping.type,
    log_level: "INFO",
    message: mapping.message,
    action_detail: {
      actor_user_id: userId,
      owner_user_id: userId,
      item_id: itemId,
      operation: mapping.operation,
      ip,
      business_key: itemId == null ? "-" : `item:${itemId}`,
      legacy_action_type: source.action_type,
      source_collection: "action_logs",
      source_action_log_id: sourceId,
      migration_id: migrationId
    },
    timestamp: source.created_at instanceof Date ? source.created_at : new Date()
  });
  inserted += 1;
}

const backfilledCount = db.system_logs.countDocuments({
  "action_detail.migration_id": migrationId
});
print(JSON.stringify({
  database: db.getName(),
  migration_id: migrationId,
  source_count: sourceActions.length,
  inserted,
  skipped,
  backfilled_count: backfilledCount
}));
