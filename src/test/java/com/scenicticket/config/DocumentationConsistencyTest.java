package com.scenicticket.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationConsistencyTest {
    @Test
    void readmeUsesExistingLaunchersAndIsolatedIntegrationTargets() throws IOException {
        String readme = read("README.md");

        assertTrue(Files.isRegularFile(Path.of("start-app.cmd")));
        assertTrue(Files.isRegularFile(Path.of("start-system.cmd")));
        assertTrue(Files.isRegularFile(Path.of("scripts/verify-mysql-test-database.ps1")));
        assertTrue(Files.isRegularFile(Path.of("scripts/verify-mysql-upgrade.ps1")));
        assertTrue(readme.contains(".\\start-app.cmd --check"));
        assertTrue(readme.contains("jdbc:mysql://localhost:3306/scenic_ticket_test"));
        assertTrue(readme.contains("-Dscenic.ticket.mongodb.database=scenic_ticket_test"));
        assertTrue(readme.contains("mongosh \"mongodb://localhost:27017/scenic_ticket\" --file"));
        assertFalse(readme.contains("mvn test -DintegrationTests=true\n```"));
        for (String document : List.of("docs/用户使用手册.md", "docs/M11_SWING_MANUAL_ACCEPTANCE.md",
                "docs/FIVE_MINUTE_DEMO.md", "docs/DEFENSE_QA.md", "docs/FINAL_REPORT.md")) {
            assertTrue(Files.isRegularFile(Path.of(document)));
            assertTrue(readme.contains("(" + document + ")"));
        }
        try (var paths = Files.walk(Path.of("docs"))) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".md")).toList()) {
                String content = Files.readString(path);
                assertFalse(content.contains("C:\\Users\\asus"), path + " contains a developer machine path");
                assertFalse(content.contains("D:\\zulu"), path + " contains a developer JDK path");
                assertFalse(content.contains("D:\\idea"), path + " contains a developer Maven path");
            }
        }
    }

    @Test
    void userManualMatchesCurrentTabsAndOrderActionPolicy() throws IOException {
        String manual = read("docs/用户使用手册.md");
        String appFrame = read("src/main/java/com/scenicticket/ui/AppFrame.java");
        String browsePanel = read("src/main/java/com/scenicticket/ui/ScenicBrowsePanel.java");
        String orderPanel = read("src/main/java/com/scenicticket/ui/OrderPanel.java");

        for (String tab : List.of("首页", "个人档案", "景点浏览", "我的订单", "统计报表", "后台管理", "系统审计")) {
            assertTrue(appFrame.contains("addTab(\"" + tab + "\""));
            assertTrue(manual.contains(tab));
        }
        for (String action : List.of("景点简介", "游客评论", "可售票种与日期", "购买门票", "发表评论")) {
            assertTrue(browsePanel.contains("(\"" + action + "\")"));
            assertTrue(manual.contains("“" + action + "”"));
        }
        for (String action : List.of("确认支付", "取消待支付订单", "申请模拟退款")) {
            assertTrue(orderPanel.contains("(\"" + action + "\")"));
            assertTrue(manual.contains("“" + action + "”"));
        }
        assertTrue(manual.contains("查询和查看其他用户订单，但不会启用支付、取消或退款按钮"));
        assertTrue(manual.contains("下单前估算"));
    }

    @Test
    void mysqlDesignListsCurrentTablesLifecycleFieldsAndObjects() throws IOException {
        String schema = read("src/main/resources/sql/mysql_schema.sql");
        String design = read("docs/MySQL_ER图.md");

        for (String table : List.of("schema_migrations", "users", "categories", "items", "orders", "profiles",
                "ticket_types", "ticket_inventory", "refunds", "admissions")) {
            assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS " + table));
            assertTrue(design.contains(table));
        }
        for (String field : List.of("price", "discount_rate", "quantity", "payment_method", "ticket_type_id",
                "ticket_type_name_snapshot", "original_unit_price", "discounted_unit_price", "visit_date",
                "expires_at", "paid_at", "cancelled_at", "completed_at", "refunded_at", "status_version",
                "available_stock", "reserved_stock", "sold_stock", "reason", "operator_user_id", "note")) {
            assertTrue(schema.contains(field));
            assertTrue(design.contains(field));
        }
        for (String object : List.of("v_user_profile", "v_item_order_summary", "sp_monthly_order_report",
                "sp_update_inactive_items", "trg_orders_before_insert", "trg_items_before_update")) {
            assertTrue(design.contains(object));
        }
    }

    @Test
    void mongoDesignListsManagedCollectionsCompatibilityAndDay09Indexes() throws IOException {
        String init = read("src/main/resources/sql/mongodb_init.js");
        String optimization = read("src/main/resources/sql/mongodb_day07_optimization.js");
        String compatibility = read("src/main/resources/sql/mongodb_day09_id_compatibility.js");
        String indexes = read("src/main/resources/sql/mongodb_day09_indexes.js");
        String design = read("docs/MongoDB集合设计文档.md");

        for (String collection : List.of("action_logs", "comments", "item_details", "system_logs")) {
            assertTrue(init.contains("createCollection(\"" + collection + "\")"));
            assertTrue(design.contains(collection));
        }
        assertTrue(indexes.contains("message: \"text\""));
        assertTrue(design.contains("message | 文本索引"));
        assertTrue(design.contains("user_id, log_type, log_level, timestamp"));
        assertTrue(design.contains("user_id, action_type, created_at"));
        assertTrue(design.contains("数值 ID"));
        assertTrue(design.contains("Java Driver"));
        for (String script : List.of(init, optimization, compatibility, indexes)) {
            assertFalse(script.contains("use(\"scenic_ticket\")"));
            assertTrue(script.contains("db.getName()"));
            assertTrue(script.contains("scenic_ticket_test"));
        }
        for (String indexName : List.of("idx_action_logs_user_time", "idx_action_logs_item_type",
                "idx_comments_item_time", "uq_comments_user_item", "uq_item_details_item_id")) {
            assertTrue(init.contains(indexName));
        }
        for (String indexName : List.of("idx_comments_item_time", "uq_comments_user_item",
                "uq_item_details_item_id", "idx_system_logs_audit", "idx_system_logs_message_text")) {
            assertTrue(indexes.contains(indexName));
        }
    }

    @Test
    void requirementsAndMigrationPlanDescribeImplementedBusinessAndActualScripts() throws IOException {
        String requirements = read("docs/需求规格说明书.md");
        String rules = read("docs/BUSINESS_RULES.md");
        String migrations = read("docs/DATABASE_MIGRATIONS.md");

        for (String requirement : List.of("票种管理", "每日库存", "待支付订单", "模拟退款", "入园核销",
                "服务授权", "MongoDB `item_details`", "系统审计")) {
            assertTrue(requirements.contains(requirement));
        }
        assertTrue(requirements.contains("今天或未来"));
        assertTrue(rules.contains("今天或未来的有效游玩日期"));
        assertTrue(rules.contains("outbox/补偿记录属于后续正式产品化扩展"));

        for (String script : List.of("mysql_day09_migration_baseline.sql",
                "mysql_day09_ticket_types_inventory.sql",
                "mysql_day09_order_lifecycle_refunds_admissions.sql",
                "mongodb_day09_id_compatibility.js", "mongodb_day09_indexes.js")) {
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/sql", script)));
            assertTrue(migrations.contains(script));
        }
        assertFalse(migrations.contains("mysql_day09_ticket_types.sql"));
        assertFalse(migrations.contains("mysql_day09_inventory.sql"));
        assertFalse(migrations.contains("mysql_day09_refunds_admissions.sql"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
