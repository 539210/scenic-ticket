package com.scenicticket.integration;

import com.mongodb.client.model.Filters;
import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Order;
import com.scenicticket.model.Profile;
import com.scenicticket.service.AdminUserService;
import com.scenicticket.service.AdmissionService;
import com.scenicticket.service.BusinessService;
import com.scenicticket.service.CommentService;
import com.scenicticket.service.OrderLifecycleService;
import com.scenicticket.service.StatisticsService;
import com.scenicticket.service.SystemLogService;
import com.scenicticket.service.TicketInventoryService;
import com.scenicticket.service.UserService;
import com.scenicticket.util.MongoDBUtil;
import com.scenicticket.util.MySQLDBUtil;
import org.bson.Document;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleBusinessFlowIntegrationTest {
    private static final long ADMIN_ID = 1L;
    private static final String IP = "127.0.0.1";

    @BeforeAll
    static void verifyTargets() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true with explicit scenic_ticket_test overrides.");
        DatabaseTargetGuard.requireIsolatedTestTargets(DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));
    }

    @Test
    void ordinaryUserAndAdministratorFlowsPersistAcrossRelogin() throws Exception {
        UserService userService = new UserService();
        BusinessService businessService = new BusinessService();
        TicketInventoryService inventoryService = new TicketInventoryService();
        OrderLifecycleService lifecycleService = new OrderLifecycleService();
        CommentService commentService = new CommentService();
        AdminUserService adminUserService = new AdminUserService();
        AdmissionService admissionService = new AdmissionService();
        StatisticsService statisticsService = new StatisticsService();
        SystemLogService systemLogService = new SystemLogService();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String username = "flow_" + suffix;
        String password = "FlowPass123";
        String email = username + "@example.com";
        String phone = String.format("139%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(30);
        Date testStartedAt = new Date(System.currentTimeMillis() - 1_000L);

        long userId = 0L;
        long categoryId = 0L;
        long itemId = 0L;
        long futureTicketTypeId = 0L;
        long todayTicketTypeId = 0L;
        long futureOrderId = 0L;
        long todayOrderId = 0L;
        try {
            var adminLogin = userService.login("kongsc", "ksc123456", IP);
            assertTrue(adminLogin.isSuccess());
            assertTrue(userService.isAdmin(adminLogin.getUser()));

            categoryId = businessService.createCategory(ADMIN_ID, "全流程分类-" + suffix, null);
            itemId = businessService.createItem(ADMIN_ID, "全流程景点-" + suffix, categoryId,
                    "用于 M10 完整业务流程验收的中文简介", List.of("flow-cover.jpg"),
                    new Document("open_time", "08:00-18:00").append("level", "AAAAA"),
                    new BigDecimal("120.00"), new BigDecimal("10.00"));
            futureTicketTypeId = inventoryService.createTicketType(ADMIN_ID, itemId, "提前购票-" + suffix,
                    new BigDecimal("100.00"), new BigDecimal("20.00"));
            todayTicketTypeId = inventoryService.createTicketType(ADMIN_ID, itemId, "当日核销票-" + suffix,
                    new BigDecimal("80.00"), BigDecimal.ZERO);
            inventoryService.setTotalStock(ADMIN_ID, futureTicketTypeId, futureDate, 10);
            inventoryService.setTotalStock(ADMIN_ID, todayTicketTypeId, today, 5);

            userId = userService.register(username, password, email, phone);
            long flowUserId = userId;
            var firstLogin = userService.login(username, password, IP);
            assertTrue(firstLogin.isSuccess());
            assertEquals(flowUserId, firstLogin.getUser().getUserId());
            assertFalse(userService.isAdmin(firstLogin.getUser()));

            Profile profile = new Profile();
            profile.setUserId(flowUserId);
            profile.setRealName("全流程游客");
            profile.setIdCard("M10-FLOW-" + suffix);
            profile.setAddress("测试地址 10 号");
            profile.setNotes("退出重登后仍应存在");
            assertTrue(userService.updateProfile(flowUserId, profile));

            long createdItemId = itemId;
            assertTrue(businessService.searchItems(suffix, null, 20, 0).stream()
                    .anyMatch(item -> item.getItemId() == createdItemId));
            var detail = businessService.getItemDetail(flowUserId, itemId, IP);
            assertEquals("用于 M10 完整业务流程验收的中文简介",
                    detail.getDetail().getString("description"));
            assertEquals(2, inventoryService.listAvailable(flowUserId, itemId, today, futureDate).size());
            assertThrows(BusinessException.class,
                    () -> businessService.searchAllItemsForAdmin(flowUserId, null, null, 20, 0));

            futureOrderId = lifecycleService.createPendingOrder(flowUserId, futureTicketTypeId, futureDate, 2,
                    "微信", IP).orderId();
            lifecycleService.pay(flowUserId, futureOrderId, IP);
            long paidFutureOrderId = futureOrderId;
            assertTrue(businessService.listUserOrders(flowUserId, 20, 0).stream()
                    .anyMatch(order -> order.getOrderId() == paidFutureOrderId && order.getStatus() == 1));

            var comment = commentService.submit(flowUserId, itemId, "完整流程中文评论", 5, IP);
            assertFalse(comment.updated());
            assertTrue(commentService.listForItem(flowUserId, itemId, 50).comments().stream()
                    .anyMatch(view -> "完整流程中文评论".equals(view.content())));

            assertTrue(userService.logout(firstLogin.getUser(), IP));
            var secondLogin = userService.login(username, password, IP);
            assertTrue(secondLogin.isSuccess());
            assertEquals("退出重登后仍应存在",
                    userService.getProfile(flowUserId, flowUserId).orElseThrow().getNotes());
            assertTrue(businessService.listUserOrders(flowUserId, 20, 0).stream()
                    .anyMatch(order -> order.getOrderId() == paidFutureOrderId && order.getStatus() == 1));

            lifecycleService.refund(flowUserId, futureOrderId, "M10 完整流程退款", IP);
            assertEquals(2,
                    findOrder(businessService.listUserOrders(flowUserId, 20, 0), futureOrderId).getStatus());

            todayOrderId = lifecycleService.createPendingOrder(flowUserId, todayTicketTypeId, today, 2,
                    "支付宝", IP).orderId();
            lifecycleService.pay(flowUserId, todayOrderId, IP);
            assertTrue(userService.logout(secondLogin.getUser(), IP));

            assertTrue(adminUserService.searchUsers(ADMIN_ID,
                    new UserSearchCriteria(username, email, "USER", 1, 20, 0)).stream()
                    .anyMatch(user -> user.getUserId() == flowUserId));
            assertEquals(flowUserId,
                    adminUserService.getUserDetail(ADMIN_ID, flowUserId).getUser().getUserId());
            assertEquals(2,
                    businessService.searchOrderViews(ADMIN_ID, flowUserId, null, null, 20, 0).size());

            assertTrue(adminUserService.changeUserStatus(ADMIN_ID, flowUserId, 0).updated());
            assertFalse(userService.login(username, password, IP).isSuccess());
            assertTrue(adminUserService.changeUserStatus(ADMIN_ID, flowUserId, 1).updated());

            var partialAdmission = admissionService.admit(ADMIN_ID, todayOrderId, 1, "第一位游客入园");
            assertFalse(partialAdmission.completed());
            var completedAdmission = admissionService.admit(ADMIN_ID, todayOrderId, 1, "第二位游客入园");
            assertTrue(completedAdmission.completed());
            assertEquals(3,
                    findOrder(businessService.listUserOrders(flowUserId, 20, 0), todayOrderId).getStatus());
            assertEquals(2, admissionService.listByOrder(ADMIN_ID, todayOrderId).size());
            assertEquals(2, inventoryService.listInventory(ADMIN_ID, todayTicketTypeId, today, today)
                    .get(0).getSoldStock());

            assertTrue(statisticsService.getHotItemRanking(null, null, 100).stream()
                    .anyMatch(row -> row.getItemId() == createdItemId && row.isItemFound()));
            assertNotNull(statisticsService.buildDashboardReport(ADMIN_ID, null, null));
            assertFalse(statisticsService.getMonthlyOrderReport(today.getYear(), today.getMonthValue()).isEmpty());
            assertTrue(systemLogService.queryAuditLogs(ADMIN_ID, flowUserId, "LOGIN", "INFO",
                    testStartedAt, null, "Login success", 100).size() >= 2);

            assertTrue(userService.logout(adminLogin.getUser(), IP));
            var finalLogin = userService.login(username, password, IP);
            assertTrue(finalLogin.isSuccess());
            List<Order> persistedOrders = businessService.listUserOrders(flowUserId, 20, 0);
            assertEquals(2, findOrder(persistedOrders, futureOrderId).getStatus());
            assertEquals(3, findOrder(persistedOrders, todayOrderId).getStatus());
            assertEquals("完整流程中文评论",
                    commentService.listForItem(flowUserId, itemId, 50).comments().stream()
                            .filter(view -> view.userId() == flowUserId).findFirst().orElseThrow().content());
            assertTrue(userService.logout(finalLogin.getUser(), IP));
        } finally {
            try {
                cleanupMongo(userId, itemId, testStartedAt);
            } finally {
                cleanupMySql(userId, futureOrderId, todayOrderId, futureTicketTypeId, todayTicketTypeId,
                        itemId, categoryId);
            }
        }
    }

    private static Order findOrder(List<Order> orders, long orderId) {
        return orders.stream().filter(order -> order.getOrderId() == orderId).findFirst().orElseThrow();
    }

    private static void cleanupMongo(long userId, long itemId, Date testStartedAt) {
        if (itemId > 0) {
            MongoDBUtil.getDatabase().getCollection("item_details")
                    .deleteMany(Filters.in("item_id", itemId, String.valueOf(itemId)));
            MongoDBUtil.getDatabase().getCollection("comments")
                    .deleteMany(Filters.in("item_id", itemId, String.valueOf(itemId)));
            MongoDBUtil.getDatabase().getCollection("action_logs")
                    .deleteMany(Filters.in("item_id", itemId, String.valueOf(itemId)));
        }
        if (userId > 0) {
            MongoDBUtil.getDatabase().getCollection("system_logs")
                    .deleteMany(Filters.in("user_id", userId, String.valueOf(userId)));
        }
        MongoDBUtil.getDatabase().getCollection("system_logs").deleteMany(Filters.and(
                Filters.in("user_id", ADMIN_ID, String.valueOf(ADMIN_ID)),
                Filters.gte("timestamp", testStartedAt)));
    }

    private static void cleanupMySql(long userId, long futureOrderId, long todayOrderId,
                                     long futureTicketTypeId, long todayTicketTypeId,
                                     long itemId, long categoryId) throws Exception {
        try (Connection connection = MySQLDBUtil.getConnection()) {
            for (long orderId : new long[]{futureOrderId, todayOrderId}) {
                if (orderId <= 0) continue;
                for (String table : new String[]{"admissions", "refunds", "orders"}) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM " + table + " WHERE order_id = ?")) {
                        statement.setLong(1, orderId);
                        statement.executeUpdate();
                    }
                }
            }
            for (long ticketTypeId : new long[]{futureTicketTypeId, todayTicketTypeId}) {
                if (ticketTypeId <= 0) continue;
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM ticket_inventory WHERE ticket_type_id = ?")) {
                    statement.setLong(1, ticketTypeId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM ticket_types WHERE ticket_type_id = ?")) {
                    statement.setLong(1, ticketTypeId);
                    statement.executeUpdate();
                }
            }
            if (itemId > 0) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM items WHERE item_id = ?")) {
                    statement.setLong(1, itemId);
                    statement.executeUpdate();
                }
            }
            if (categoryId > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM categories WHERE category_id = ?")) {
                    statement.setLong(1, categoryId);
                    statement.executeUpdate();
                }
            }
            if (userId > 0) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM profiles WHERE user_id = ?")) {
                    statement.setLong(1, userId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                    statement.setLong(1, userId);
                    statement.executeUpdate();
                }
            }
        }
    }
}
