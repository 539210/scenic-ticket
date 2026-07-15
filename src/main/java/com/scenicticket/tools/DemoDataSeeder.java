package com.scenicticket.tools;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.scenicticket.config.DBConfig;
import com.scenicticket.util.MongoDBUtil;
import com.scenicticket.util.MySQLDBUtil;
import com.scenicticket.util.PasswordUtil;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Idempotent CLI seeder for an additional set of realistic demonstration data. */
public final class DemoDataSeeder {
    static final int MAX_COUNT = 50;
    static final long USER_ID_BASE = 20_000L;
    static final long ITEM_ID_BASE = 20_000L;
    static final long ORDER_ID_BASE = 40_000L;
    private static final String SEED_SOURCE = "DEMO50_SEED_V1";
    private static final String DEMO_PASSWORD = "DemoPass123";
    private static final Set<String> ALLOWED_DATABASES = Set.of("scenic_ticket", "scenic_ticket_test");
    private static final String[] REGIONS = {
            "青岚", "星河", "云栖", "松涛", "月湾", "锦溪", "丹霞", "碧湖", "长风", "古韵"
    };
    private static final ScenicKind[] KINDS = {
            new ScenicKind("山岳公园", 4L, "层叠山势、观景步道和自然植被构成主要景观", List.of("登高观景", "自然徒步")),
            new ScenicKind("湿地公园", 1L, "湖泊、浅滩和生态栈道适合观鸟与休闲散步", List.of("湿地生态", "观鸟休闲")),
            new ScenicKind("历史街区", 6L, "传统街巷、历史建筑和地方文化展示串联完整游线", List.of("历史文化", "古街漫游")),
            new ScenicKind("亲子乐园", 8L, "互动游乐、亲子协作和儿童休息设施覆盖多个年龄段", List.of("亲子互动", "家庭出游")),
            new ScenicKind("文化展馆", 7L, "主题展览、公共教育和互动体验共同呈现地方文化", List.of("文化展览", "互动体验"))
    };
    private static final String[] COMMENT_TEXTS = {
            "景区路线清楚，工作人员服务耐心，整体体验比预期更好。",
            "环境维护得很干净，游览节奏轻松，适合周末慢慢参观。",
            "现场指引完善，购票和入园都很顺畅，下次还会再来。",
            "景观层次丰富，休息区域也充足，带家人游玩很方便。",
            "讲解内容清晰，配套设施齐全，是一次很满意的出行。"
    };

    private DemoDataSeeder() {
    }

    public static void main(String[] args) throws Exception {
        SeedArguments arguments = SeedArguments.parse(args);
        if (!arguments.apply()) {
            System.out.println("安全检查：未提供 --apply，数据库没有改变。");
            System.out.println("确认执行：seed-demo-data.cmd --apply");
            return;
        }

        List<DemoUser> users = buildUsers(arguments.count());
        List<DemoScenic> scenics = buildScenics(arguments.count());
        try {
            MySqlSeedResult mysql = seedMySql(users, scenics);
            MongoSeedResult mongo = seedMongo(users, scenics);
            System.out.printf(Locale.ROOT,
                    "演示数据完成：用户 %d（新增 %d），景点 %d（新增 %d），评论 %d（新增 %d），订单新增 %d。%n",
                    users.size(), mysql.insertedUsers(), scenics.size(), mysql.insertedItems(),
                    scenics.size(), mongo.insertedComments(), mysql.insertedOrders());
            System.out.println("演示账号：demo_user_001 至 demo_user_"
                    + String.format(Locale.ROOT, "%03d", arguments.count()));
            System.out.println("统一演示密码：" + DEMO_PASSWORD);
        } finally {
            MongoDBUtil.closeClient();
            MySQLDBUtil.closeDataSource();
        }
    }

    static List<DemoUser> buildUsers(int count) {
        validateCount(count);
        List<DemoUser> users = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            long id = USER_ID_BASE + index;
            users.add(new DemoUser(id,
                    String.format(Locale.ROOT, "demo_user_%03d", index),
                    String.format(Locale.ROOT, "demo.user.%03d@example.invalid", index),
                    String.format(Locale.ROOT, "1398%07d", index),
                    String.format(Locale.ROOT, "演示游客%03d", index)));
        }
        return List.copyOf(users);
    }

    static List<DemoScenic> buildScenics(int count) {
        validateCount(count);
        List<DemoScenic> scenics = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            String region = REGIONS[(index - 1) % REGIONS.length];
            ScenicKind kind = KINDS[(index - 1) / REGIONS.length % KINDS.length];
            long itemId = ITEM_ID_BASE + index;
            BigDecimal price = BigDecimal.valueOf(38L + ((index * 13L) % 130L));
            BigDecimal discount = BigDecimal.valueOf((index % 5) * 5L);
            String title = "扩展示范·" + region + kind.suffix();
            String description = title + "以" + kind.feature()
                    + "。景区设置清晰导览、休息点和游客服务设施，适合半日游览；出行前请关注天气和开放公告。";
            String address = region + "市文旅大道 " + (100 + index) + " 号";
            scenics.add(new DemoScenic(itemId, title, kind.categoryId(), price, discount,
                    description, address, kind.tags(), "08:30-18:00"));
        }
        return List.copyOf(scenics);
    }

    private static MySqlSeedResult seedMySql(List<DemoUser> users, List<DemoScenic> scenics) throws SQLException {
        try (Connection connection = MySQLDBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                requireCategories(connection, scenics);
                validateReservedRows(connection, users, scenics);
                String passwordHash = PasswordUtil.hashPassword(DEMO_PASSWORD);
                int insertedUsers = insertUsers(connection, users, passwordHash);
                insertProfiles(connection, users);
                int insertedItems = insertItems(connection, scenics);
                int insertedOrders = 0;
                for (int index = 0; index < scenics.size(); index++) {
                    DemoScenic scenic = scenics.get(index);
                    DemoUser user = users.get(index % users.size());
                    long ticketTypeId = ensureAdultTicket(connection, scenic);
                    ensureInventory(connection, ticketTypeId);
                    insertedOrders += ensurePaidOrder(connection, index + 1, user, scenic, ticketTypeId);
                }
                connection.commit();
                return new MySqlSeedResult(insertedUsers, insertedItems, insertedOrders);
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException sqlException) throw sqlException;
                throw new SQLException("生成 MySQL 演示数据失败", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static void requireCategories(Connection connection, List<DemoScenic> scenics) throws SQLException {
        Set<Long> required = new HashSet<>();
        scenics.forEach(scenic -> required.add(scenic.categoryId()));
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT category_id FROM categories WHERE category_id IN (1, 4, 6, 7, 8)")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) required.remove(resultSet.getLong(1));
            }
        }
        if (!required.isEmpty()) {
            throw new SQLException("缺少初始化景点分类 " + required + "，请先运行项目数据库初始化");
        }
    }

    private static void validateReservedRows(Connection connection, List<DemoUser> users,
                                             List<DemoScenic> scenics) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT user_id, username, email FROM users WHERE user_id = ? OR username = ? OR email = ?")) {
            for (DemoUser user : users) {
                statement.setLong(1, user.userId());
                statement.setString(2, user.username());
                statement.setString(3, user.email());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        if (rows.getLong("user_id") != user.userId()
                                || !user.username().equals(rows.getString("username"))
                                || !user.email().equals(rows.getString("email"))) {
                            throw new SQLException("演示用户保留编号或账号发生冲突：" + user.username());
                        }
                    }
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT item_id, title FROM items WHERE item_id = ? OR title = ?")) {
            for (DemoScenic scenic : scenics) {
                statement.setLong(1, scenic.itemId());
                statement.setString(2, scenic.title());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        if (rows.getLong("item_id") != scenic.itemId()
                                || !scenic.title().equals(rows.getString("title"))) {
                            throw new SQLException("演示景点保留编号或名称发生冲突：" + scenic.title());
                        }
                    }
                }
            }
        }
    }

    private static int insertUsers(Connection connection, List<DemoUser> users, String passwordHash)
            throws SQLException {
        int inserted = 0;
        String sql = "INSERT IGNORE INTO users "
                + "(user_id, username, password_hash, email, phone, role, status) VALUES (?, ?, ?, ?, ?, 'USER', 1)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (DemoUser user : users) {
                statement.setLong(1, user.userId());
                statement.setString(2, user.username());
                statement.setString(3, passwordHash);
                statement.setString(4, user.email());
                statement.setString(5, user.phone());
                inserted += statement.executeUpdate();
            }
        }
        return inserted;
    }

    private static void insertProfiles(Connection connection, List<DemoUser> users) throws SQLException {
        String sql = "INSERT IGNORE INTO profiles (user_id, real_name, id_card, address, notes) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (DemoUser user : users) {
                statement.setLong(1, user.userId());
                statement.setString(2, user.realName());
                statement.setString(3, String.format(Locale.ROOT, "33010119900101%04d", index));
                statement.setString(4, "扩展示范城区游客路 " + index + " 号");
                statement.setString(5, "由 50 条演示数据脚本生成，可用于用户管理和统计展示");
                statement.addBatch();
                index++;
            }
            statement.executeBatch();
        }
    }

    private static int insertItems(Connection connection, List<DemoScenic> scenics) throws SQLException {
        int inserted = 0;
        String sql = "INSERT IGNORE INTO items "
                + "(item_id, title, category_id, price, discount_rate, status) VALUES (?, ?, ?, ?, ?, 1)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (DemoScenic scenic : scenics) {
                statement.setLong(1, scenic.itemId());
                statement.setString(2, scenic.title());
                statement.setLong(3, scenic.categoryId());
                statement.setBigDecimal(4, scenic.price());
                statement.setBigDecimal(5, scenic.discount());
                inserted += statement.executeUpdate();
            }
        }
        return inserted;
    }

    private static long ensureAdultTicket(Connection connection, DemoScenic scenic) throws SQLException {
        String insert = "INSERT INTO ticket_types (item_id, name, original_price, discount_rate, status) "
                + "VALUES (?, '成人票', ?, ?, 1) ON DUPLICATE KEY UPDATE ticket_type_id = ticket_type_id";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setLong(1, scenic.itemId());
            statement.setBigDecimal(2, scenic.price());
            statement.setBigDecimal(3, scenic.discount());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ticket_type_id FROM ticket_types WHERE item_id = ? AND name = '成人票'")) {
            statement.setLong(1, scenic.itemId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getLong(1);
            }
        }
        throw new SQLException("成人票创建失败：" + scenic.title());
    }

    private static void ensureInventory(Connection connection, long ticketTypeId) throws SQLException {
        String sql = "INSERT IGNORE INTO ticket_inventory "
                + "(ticket_type_id, visit_date, total_stock, available_stock, reserved_stock, sold_stock, version) "
                + "VALUES (?, ?, 100, 100, 0, 0, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int day = 1; day <= 31; day++) {
                statement.setLong(1, ticketTypeId);
                statement.setDate(2, Date.valueOf(LocalDate.now().plusDays(day)));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static int ensurePaidOrder(Connection connection, int index, DemoUser user,
                                       DemoScenic scenic, long ticketTypeId) throws SQLException {
        long orderId = ORDER_ID_BASE + index;
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT user_id, item_id FROM orders WHERE order_id = ?")) {
            check.setLong(1, orderId);
            try (ResultSet rows = check.executeQuery()) {
                if (rows.next()) {
                    if (rows.getLong("user_id") != user.userId() || rows.getLong("item_id") != scenic.itemId()) {
                        throw new SQLException("演示订单保留编号发生冲突：" + orderId);
                    }
                    return 0;
                }
            }
        }
        BigDecimal unitPrice = scenic.price().multiply(BigDecimal.ONE.subtract(
                scenic.discount().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate visitDate = LocalDate.now().plusDays(7L + (index % 10));
        LocalDateTime createdAt = LocalDateTime.now().minusDays(index % 28L).withNano(0);
        String sql = "INSERT INTO orders (order_id, user_id, item_id, amount, quantity, unit_price, "
                + "discount_rate, payment_method, ticket_type_id, ticket_type_name_snapshot, "
                + "original_unit_price, discounted_unit_price, visit_date, expires_at, paid_at, status, created_at) "
                + "VALUES (?, ?, ?, ?, 1, ?, ?, '微信', ?, '成人票', ?, ?, ?, ?, ?, 1, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, user.userId());
            statement.setLong(3, scenic.itemId());
            statement.setBigDecimal(4, unitPrice);
            statement.setBigDecimal(5, unitPrice);
            statement.setBigDecimal(6, scenic.discount());
            statement.setLong(7, ticketTypeId);
            statement.setBigDecimal(8, scenic.price());
            statement.setBigDecimal(9, unitPrice);
            statement.setDate(10, Date.valueOf(visitDate));
            statement.setTimestamp(11, Timestamp.valueOf(createdAt.plusMinutes(15)));
            statement.setTimestamp(12, Timestamp.valueOf(createdAt.plusMinutes(2)));
            statement.setTimestamp(13, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE ticket_inventory SET available_stock = available_stock - 1, sold_stock = sold_stock + 1, "
                        + "version = version + 1 WHERE ticket_type_id = ? AND visit_date = ? AND available_stock > 0")) {
            statement.setLong(1, ticketTypeId);
            statement.setDate(2, Date.valueOf(visitDate));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("演示订单库存扣减失败：" + scenic.title());
            }
        }
        return 1;
    }

    private static MongoSeedResult seedMongo(List<DemoUser> users, List<DemoScenic> scenics) {
        String databaseName = DBConfig.get("mongodb.database");
        if (!ALLOWED_DATABASES.contains(databaseName)) {
            throw new IllegalStateException("拒绝向非预期 MongoDB 数据库写入演示数据：" + databaseName);
        }
        MongoDatabase database = MongoDBUtil.getDatabase();
        Set<String> collections = new HashSet<>();
        database.listCollectionNames().into(new ArrayList<>()).forEach(collections::add);
        for (String required : List.of("item_details", "comments", "system_logs")) {
            if (!collections.contains(required)) {
                throw new IllegalStateException("MongoDB 缺少集合 " + required + "，请先运行项目数据库初始化");
            }
        }
        MongoCollection<Document> details = database.getCollection("item_details");
        MongoCollection<Document> comments = database.getCollection("comments");
        int insertedDetails = 0;
        int insertedComments = 0;
        java.util.Date now = new java.util.Date();
        for (int index = 0; index < scenics.size(); index++) {
            DemoScenic scenic = scenics.get(index);
            DemoUser user = users.get(index % users.size());
            Document existingDetail = details.find(Filters.eq("item_id", scenic.itemId())).first();
            if (existingDetail != null && !SEED_SOURCE.equals(existingDetail.getString("seed_source"))) {
                throw new IllegalStateException("MongoDB 景点保留编号发生冲突：" + scenic.itemId());
            }
            var detailResult = details.updateOne(Filters.eq("item_id", scenic.itemId()), Updates.combine(
                    Updates.setOnInsert("item_id", scenic.itemId()),
                    Updates.setOnInsert("description", scenic.description()),
                    Updates.setOnInsert("images", List.of()),
                    Updates.setOnInsert("metadata", new Document("language", "zh-CN")
                            .append("open_time", scenic.openTime())
                            .append("address", scenic.address())
                            .append("highlights", scenic.tags())
                            .append("recommended_duration", "2-4 小时")),
                    Updates.setOnInsert("seed_source", SEED_SOURCE),
                    Updates.setOnInsert("updated_at", now)), new UpdateOptions().upsert(true));
            if (detailResult.getUpsertedId() != null) insertedDetails++;

            int rating = 4 + (index % 3 == 0 ? 1 : 0);
            var commentFilter = Filters.and(
                    Filters.eq("user_id", user.userId()), Filters.eq("item_id", scenic.itemId()));
            Document existingComment = comments.find(commentFilter).first();
            if (existingComment != null && !SEED_SOURCE.equals(existingComment.getString("source"))) {
                throw new IllegalStateException("MongoDB 评论保留用户/景点组合发生冲突："
                        + user.userId() + "/" + scenic.itemId());
            }
            var commentResult = comments.updateOne(commentFilter,
                    Updates.combine(
                            Updates.setOnInsert("user_id", user.userId()),
                            Updates.setOnInsert("item_id", scenic.itemId()),
                            Updates.setOnInsert("content", COMMENT_TEXTS[index % COMMENT_TEXTS.length]),
                            Updates.setOnInsert("rating", rating),
                            Updates.setOnInsert("tags", scenic.tags()),
                            Updates.setOnInsert("source", SEED_SOURCE),
                            Updates.setOnInsert("created_at", new java.util.Date(now.getTime() - index * 3_600_000L)),
                            Updates.setOnInsert("updated_at", new java.util.Date(now.getTime() - index * 3_600_000L))),
                    new UpdateOptions().upsert(true));
            if (commentResult.getUpsertedId() != null) insertedComments++;
        }
        database.getCollection("system_logs").updateOne(
                Filters.and(Filters.eq("log_type", "DEMO_DATA_SEED"),
                        Filters.eq("action_detail.seed_source", SEED_SOURCE)),
                Updates.combine(
                        Updates.setOnInsert("user_id", null),
                        Updates.setOnInsert("log_type", "DEMO_DATA_SEED"),
                        Updates.setOnInsert("log_level", "INFO"),
                        Updates.setOnInsert("message", "生成扩展演示用户、景点、订单与评论"),
                        Updates.setOnInsert("action_detail", new Document("seed_source", SEED_SOURCE)
                                .append("count", scenics.size())),
                        Updates.setOnInsert("timestamp", now)),
                new UpdateOptions().upsert(true));
        return new MongoSeedResult(insertedDetails, insertedComments);
    }

    private static void validateCount(int count) {
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("生成数量必须在 1 到 " + MAX_COUNT + " 之间");
        }
    }

    record DemoUser(long userId, String username, String email, String phone, String realName) {
    }

    record DemoScenic(long itemId, String title, long categoryId, BigDecimal price, BigDecimal discount,
                      String description, String address, List<String> tags, String openTime) {
        DemoScenic {
            tags = List.copyOf(tags);
        }
    }

    private record ScenicKind(String suffix, long categoryId, String feature, List<String> tags) {
    }

    private record MySqlSeedResult(int insertedUsers, int insertedItems, int insertedOrders) {
    }

    private record MongoSeedResult(int insertedDetails, int insertedComments) {
    }

    private record SeedArguments(boolean apply, int count) {
        private static SeedArguments parse(String[] args) {
            boolean apply = false;
            int count = MAX_COUNT;
            for (String argument : args == null ? new String[0] : args) {
                if ("--apply".equalsIgnoreCase(argument)) {
                    apply = true;
                } else if (argument.startsWith("--count=")) {
                    count = Integer.parseInt(argument.substring("--count=".length()));
                } else {
                    throw new IllegalArgumentException("未知参数：" + argument);
                }
            }
            validateCount(count);
            return new SeedArguments(apply, count);
        }
    }
}
