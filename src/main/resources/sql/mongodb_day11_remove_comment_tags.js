const allowedDatabases = ["scenic_ticket", "scenic_ticket_test"];
if (!allowedDatabases.includes(db.getName())) {
  throw new Error(`Refusing comment tag removal for unexpected database: ${db.getName()}`);
}

const commentResult = db.comments.updateMany(
  { tags: { $exists: true } },
  { $unset: { tags: "" } }
);
const auditResult = db.system_logs.updateMany(
  { "action_detail.tags": { $exists: true } },
  { $unset: { "action_detail.tags": "" } }
);

printjson({
  database: db.getName(),
  comments_changed: commentResult.modifiedCount,
  audit_logs_changed: auditResult.modifiedCount,
  remaining_comment_tags: db.comments.countDocuments({ tags: { $exists: true } }),
  remaining_audit_tags: db.system_logs.countDocuments({ "action_detail.tags": { $exists: true } })
});
