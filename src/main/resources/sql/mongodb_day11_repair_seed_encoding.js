const allowedDatabases = ["scenic_ticket", "scenic_ticket_test"];
if (!allowedDatabases.includes(db.getName())) {
  throw new Error(`Refusing encoding repair for unexpected database: ${db.getName()}`);
}

const migrationId = "day11_repair_seed_encoding_v1";
const corruptedText = /\?{2,}/;
const corruptedOnlyWithSequence = /^\?+\s+\d+\?+$/;
const corruptedOnly = /^\?+$/;

const damagedDetails = db.item_details.find({
  $or: [
    { description: corruptedText },
    { "metadata.address": corruptedText }
  ]
}).toArray();

const damagedComments = db.comments.find({
  $or: [
    { content: corruptedText },
    { tags: { $elemMatch: { $regex: corruptedText } } }
  ]
}).toArray();

const existingBackup = db.system_logs.findOne({
  log_type: "DATA_REPAIR_BACKUP",
  "action_detail.migration_id": migrationId
});
if (!existingBackup && (damagedDetails.length > 0 || damagedComments.length > 0)) {
  db.system_logs.insertOne({
    user_id: null,
    log_type: "DATA_REPAIR_BACKUP",
    log_level: "INFO",
    message: "Day11 历史 UTF-8 初始化样例修复前备份",
    action_detail: {
      migration_id: migrationId,
      item_details: damagedDetails,
      comments: damagedComments
    },
    timestamp: new Date()
  });
}

let repairedDetails = 0;
for (const detail of damagedDetails) {
  const itemId = Number(detail.item_id);
  if (!Number.isInteger(itemId) || itemId < 1 || itemId > 20) {
    continue;
  }
  const changes = {};
  if (corruptedText.test(String(detail.description ?? ""))) {
    changes.description = `景点 ${itemId} 的详细介绍，包含开放时间、游览路线、购票提示和入园须知。`;
  }
  const address = detail.metadata?.address;
  if (corruptedText.test(String(address ?? ""))) {
    changes["metadata.address"] = `示例景区地址 ${itemId} 号`;
  }
  if (Object.keys(changes).length > 0) {
    const result = db.item_details.updateOne({ _id: detail._id }, { $set: changes });
    repairedDetails += result.modifiedCount;
  }
}

const tagSets = [
  ["环境好", "适合亲子"],
  ["服务好", "交通方便"],
  ["景色美", "拍照推荐"],
  ["排队少", "体验好"]
];
let repairedComments = 0;
for (const comment of damagedComments) {
  const content = String(comment.content ?? "");
  const sequenceMatch = content.match(/\d+/);
  const sequence = sequenceMatch ? Number(sequenceMatch[0]) : NaN;
  const tagsAreCorrupted = Array.isArray(comment.tags)
    && comment.tags.length > 0
    && comment.tags.every(tag => corruptedOnly.test(String(tag)));
  const matchesLegacySeedFormula = Number.isInteger(sequence)
    && sequence >= 1
    && sequence <= 1000
    && corruptedOnlyWithSequence.test(content)
    && tagsAreCorrupted
    && Number(comment.user_id) === (sequence % 10) + 1
    && Number(comment.item_id) === (sequence % 20) + 1
    && Number(comment.rating) === (sequence % 5) + 1;
  if (!matchesLegacySeedFormula) {
    continue;
  }
  const result = db.comments.updateOne({ _id: comment._id }, {
    $set: {
      content: `第 ${sequence} 条评论：景区体验良好，购票流程顺畅。`,
      tags: tagSets[sequence % tagSets.length]
    }
  });
  repairedComments += result.modifiedCount;
}

const remainingDetails = db.item_details.countDocuments({
  $or: [
    { description: corruptedText },
    { "metadata.address": corruptedText }
  ]
});
const remainingComments = db.comments.countDocuments({
  $or: [
    { content: corruptedText },
    { tags: { $elemMatch: { $regex: corruptedText } } }
  ]
});

printjson({
  database: db.getName(),
  backup_created: !existingBackup && (damagedDetails.length > 0 || damagedComments.length > 0),
  repaired_details: repairedDetails,
  repaired_comments: repairedComments,
  remaining_corrupted_details: remainingDetails,
  remaining_corrupted_comments: remainingComments
});

if (remainingDetails > 0 || remainingComments > 0) {
  throw new Error(
    `Encoding repair incomplete: details=${remainingDetails}, comments=${remainingComments}. `
    + "Only records matching the trusted initialization templates are changed automatically."
  );
}
