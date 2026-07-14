const allowedDatabases = ["scenic_ticket", "scenic_ticket_test"];
if (!allowedDatabases.includes(db.getName())) {
  throw new Error(`Refusing migration for unexpected database: ${db.getName()}`);
}
print(`Migrating MongoDB database: ${db.getName()}`);

function convertNumericString(collectionName, fieldName) {
  const collection = db.getCollection(collectionName);
  const result = collection.updateMany(
    {
      [fieldName]: { $type: "string" },
      $expr: { $regexMatch: { input: `$${fieldName}`, regex: /^[0-9]+$/ } }
    },
    [{ $set: { [fieldName]: { $toLong: `$${fieldName}` } } }]
  );
  print(`${collectionName}.${fieldName}: converted ${result.modifiedCount} numeric strings`);
}

convertNumericString("action_logs", "user_id");
convertNumericString("action_logs", "item_id");
convertNumericString("comments", "user_id");
convertNumericString("comments", "item_id");
convertNumericString("item_details", "item_id");
convertNumericString("system_logs", "user_id");

db.comments.updateMany(
  { updated_at: { $exists: false } },
  [{ $set: { updated_at: { $ifNull: ["$created_at", "$$NOW"] } } }]
);

const duplicateComments = db.comments.aggregate([
  { $group: { _id: { user_id: "$user_id", item_id: "$item_id" }, count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } },
  { $count: "duplicate_groups" }
]).toArray();

print(`comment duplicate groups: ${duplicateComments.length === 0 ? 0 : duplicateComments[0].duplicate_groups}`);
print("This compatibility migration does not delete duplicate comments. Resolve duplicates explicitly before adding the unique index.");
