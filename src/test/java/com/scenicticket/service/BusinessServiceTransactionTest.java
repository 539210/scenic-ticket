package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mysql.CategoryDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.model.Item;
import com.scenicticket.model.Order;
import org.junit.jupiter.api.Test;
import org.bson.Document;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessServiceTransactionTest {
    @Test
    void createOrderCommitsAndWritesActionLogOnSuccess() {
        TrackingConnection trackingConnection = TrackingConnection.create();
        SucceedingOrderDAO orderDAO = new SucceedingOrderDAO(77L);
        CapturingLogDAO logDAO = new CapturingLogDAO();
        BusinessService service = newService(orderDAO, logDAO, trackingConnection);

        long orderId = service.createOrder(1L, 2L, 2, "微信");

        assertEquals(77L, orderId);
        assertSame(trackingConnection.connection, orderDAO.connection);
        assertEquals(List.of(false, true), trackingConnection.autoCommitValues);
        assertTrue(trackingConnection.committed);
        assertFalse(trackingConnection.rolledBack);
        assertTrue(trackingConnection.closed);
        assertEquals("ORDER", logDAO.actionType);
    }

    @Test
    void createOrderRollsBackWhenOrderInsertFails() {
        TrackingConnection trackingConnection = TrackingConnection.create();
        SQLException cause = new SQLException("insert failed");
        FailingOrderDAO orderDAO = new FailingOrderDAO(cause);
        CapturingLogDAO logDAO = new CapturingLogDAO();
        BusinessService service = newService(orderDAO, logDAO, trackingConnection);

        DBException exception = assertThrows(DBException.class,
                () -> service.createOrder(1L, 2L, 2, "微信"));

        assertSame(cause, exception.getCause());
        assertSame(trackingConnection.connection, orderDAO.connection);
        assertEquals(List.of(false, true), trackingConnection.autoCommitValues);
        assertFalse(trackingConnection.committed);
        assertTrue(trackingConnection.rolledBack);
        assertTrue(trackingConnection.closed);
        assertEquals(0, logDAO.writeCount);
    }

    @Test
    void createOrderUsesLatestPriceAndDiscountFromScenicItem() {
        TrackingConnection trackingConnection = TrackingConnection.create();
        CapturingDiscountOrderDAO orderDAO = new CapturingDiscountOrderDAO();
        BusinessService service = new BusinessService(new CategoryDAO(), new DiscountedItemDAO(), orderDAO,
                new DetailDAO(), new CapturingLogDAO(), new CommentDAO(), () -> trackingConnection.connection,
                allowAllAuthorization());

        service.createOrder(1L, 2L, 1, "微信");

        assertEquals(new BigDecimal("8110.00"), orderDAO.order.getUnitPrice());
        assertEquals(new BigDecimal("1.00"), orderDAO.order.getDiscountRate());
        assertEquals(new BigDecimal("8028.90"), orderDAO.order.getAmount());
    }

    @Test
    void createOrderRejectsInvalidInputBeforeOpeningConnection() {
        TrackingConnection trackingConnection = TrackingConnection.create();
        BusinessService service = newService(new SucceedingOrderDAO(1L), new CapturingLogDAO(), trackingConnection);

        assertThrows(BusinessException.class,
                () -> service.createOrder(0L, 2L, 1, "微信"));
        assertThrows(BusinessException.class,
                () -> service.createOrder(1L, 2L, 0, "微信"));
        assertThrows(BusinessException.class,
                () -> service.createOrder(1L, 2L, 1, "现金"));
        assertFalse(trackingConnection.closed);
        assertEquals(List.of(), trackingConnection.autoCommitValues);
    }

    @Test
    void searchOrdersPassesAllFiltersToOrderDao() {
        CapturingSearchOrderDAO orderDAO = new CapturingSearchOrderDAO();
        BusinessService service = newService(orderDAO, new CapturingLogDAO(), TrackingConnection.create());

        List<Order> orders = service.searchOrders(1L, 1L, 23L, 1, 50, 0);

        assertEquals(1, orders.size());
        assertEquals(1L, orderDAO.userId);
        assertEquals(23L, orderDAO.orderId);
        assertEquals(1, orderDAO.status);
        assertEquals(50, orderDAO.limit);
        assertEquals(0, orderDAO.offset);
    }

    @Test
    void searchOrderViewsReturnsScenicNameAndPassesFilters() {
        CapturingOrderViewDAO orderDAO = new CapturingOrderViewDAO();
        BusinessService service = newService(orderDAO, new CapturingLogDAO(), TrackingConnection.create());

        List<OrderViewDTO> views = service.searchOrderViews(1L, 3L, 9L, 1, 50, 0);

        assertEquals(1, views.size());
        assertEquals("测试景点", views.get(0).getItemTitle());
        assertEquals(3L, orderDAO.userId);
        assertEquals(9L, orderDAO.orderId);
        assertEquals(1, orderDAO.status);
    }

    @Test
    void administratorItemSearchIncludesOfflineItems() {
        CapturingAdminItemDAO itemDAO = new CapturingAdminItemDAO();
        BusinessService service = new BusinessService(new CategoryDAO(), itemDAO, new OrderDAO(), new DetailDAO(),
                new CapturingLogDAO(), new CommentDAO(), () -> TrackingConnection.create().connection,
                allowAllAuthorization());

        service.searchAllItemsForAdmin(1L, "公园", 2L, 100, 0);

        assertEquals("公园", itemDAO.keyword);
        assertEquals(2L, itemDAO.categoryId);
        assertNull(itemDAO.status);
    }

    @Test
    void updateItemPricingNormalizesAndPassesValuesToDao() {
        CapturingAdminItemDAO itemDAO = new CapturingAdminItemDAO();
        BusinessService service = new BusinessService(new CategoryDAO(), itemDAO, new OrderDAO(), new DetailDAO(),
                new CapturingLogDAO(), new CommentDAO(), () -> TrackingConnection.create().connection,
                allowAllAuthorization());

        assertTrue(service.updateItemPricing(1L, 7L, new BigDecimal("99"), new BigDecimal("12.5")));

        assertEquals(7L, itemDAO.updatedItemId);
        assertEquals(new BigDecimal("99.00"), itemDAO.updatedPrice);
        assertEquals(new BigDecimal("12.50"), itemDAO.updatedDiscount);
    }

    @Test
    void administratorCanReplaceCorruptedScenicDescriptionWithoutLosingMetadata() {
        CapturingAdminItemDAO itemDAO = new CapturingAdminItemDAO();
        CapturingDetailDAO detailDAO = new CapturingDetailDAO();
        BusinessService service = new BusinessService(new CategoryDAO(), itemDAO, new OrderDAO(), detailDAO,
                new CapturingLogDAO(), new CommentDAO(), () -> TrackingConnection.create().connection,
                allowAllAuthorization());

        assertTrue(service.updateItemDescription(1L, 7L, "  森林公园景区简介  "));

        assertEquals("森林公园景区简介", detailDAO.savedDescription);
        assertEquals(List.of("cover.jpg"), detailDAO.savedImages);
        assertEquals("08:30-19:00", detailDAO.savedMetadata.getString("open_time"));
        assertEquals("旧简介", service.getItemDescription(7L));
    }

    private static BusinessService newService(OrderDAO orderDAO, LogDAO logDAO,
                                              TrackingConnection trackingConnection) {
        return new BusinessService(new CategoryDAO(), new PricingItemDAO(), orderDAO, new DetailDAO(), logDAO,
                new CommentDAO(), () -> trackingConnection.connection, allowAllAuthorization());
    }

    private static AuthorizationService allowAllAuthorization() {
        return new AuthorizationService(new UserDAO()) {
            @Override
            public com.scenicticket.model.User requireActiveUser(long actorUserId) {
                com.scenicticket.model.User user = new com.scenicticket.model.User();
                user.setUserId(actorUserId);
                user.setRole("ADMIN");
                user.setStatus(1);
                return user;
            }

            @Override
            public com.scenicticket.model.User requireAdmin(long actorUserId) {
                return requireActiveUser(actorUserId);
            }
        };
    }

    private static class PricingItemDAO extends ItemDAO {
        @Override
        public Optional<Item> findById(long itemId) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setTitle("测试景点");
            item.setCategoryId(1L);
            item.setPrice(new BigDecimal("88.00"));
            item.setDiscountRate(BigDecimal.ZERO);
            item.setStatus(1);
            return Optional.of(item);
        }
    }

    private static class DiscountedItemDAO extends ItemDAO {
        @Override
        public Optional<Item> findById(long itemId) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setTitle("优惠测试景点");
            item.setCategoryId(1L);
            item.setPrice(new BigDecimal("8110.00"));
            item.setDiscountRate(new BigDecimal("1.00"));
            item.setStatus(1);
            return Optional.of(item);
        }
    }

    private static class CapturingDiscountOrderDAO extends OrderDAO {
        private Order order;

        @Override
        public long create(Connection connection, Order order) {
            this.order = order;
            return 88L;
        }
    }

    private static class SucceedingOrderDAO extends OrderDAO {
        private final long orderId;
        private Connection connection;

        private SucceedingOrderDAO(long orderId) {
            this.orderId = orderId;
        }

        @Override
        public long create(Connection connection, Order order) {
            this.connection = connection;
            assertEquals(1L, order.getUserId());
            assertEquals(2L, order.getItemId());
            assertEquals(new BigDecimal("176.00"), order.getAmount());
            assertEquals(2, order.getQuantity());
            assertEquals(new BigDecimal("88.00"), order.getUnitPrice());
            assertEquals(new BigDecimal("0.00"), order.getDiscountRate());
            assertEquals("微信", order.getPaymentMethod());
            assertEquals(1, order.getStatus());
            return orderId;
        }
    }

    private static class FailingOrderDAO extends OrderDAO {
        private final SQLException exception;
        private Connection connection;

        private FailingOrderDAO(SQLException exception) {
            this.exception = exception;
        }

        @Override
        public long create(Connection connection, Order order) throws SQLException {
            this.connection = connection;
            throw exception;
        }
    }

    private static class CapturingSearchOrderDAO extends OrderDAO {
        private Long userId;
        private Long orderId;
        private Integer status;
        private int limit;
        private int offset;

        @Override
        public List<Order> search(Long userId, Long orderId, Integer status, int limit, int offset) {
            this.userId = userId;
            this.orderId = orderId;
            this.status = status;
            this.limit = limit;
            this.offset = offset;
            Order order = new Order();
            order.setOrderId(orderId);
            return List.of(order);
        }
    }

    private static class CapturingOrderViewDAO extends OrderDAO {
        private Long userId;
        private Long orderId;
        private Integer status;

        @Override
        public List<OrderViewDTO> searchViews(Long userId, Long orderId, Integer status, int limit, int offset) {
            this.userId = userId;
            this.orderId = orderId;
            this.status = status;
            Order order = new Order();
            order.setOrderId(orderId);
            OrderViewDTO view = new OrderViewDTO();
            view.setOrder(order);
            view.setItemTitle("测试景点");
            return List.of(view);
        }
    }

    private static class CapturingAdminItemDAO extends ItemDAO {
        private String keyword;
        private Long categoryId;
        private Integer status;
        private long updatedItemId;
        private BigDecimal updatedPrice;
        private BigDecimal updatedDiscount;

        @Override
        public List<Item> search(String keyword, Long categoryId, Integer status, int limit, int offset) {
            this.keyword = keyword;
            this.categoryId = categoryId;
            this.status = status;
            return List.of();
        }

        @Override
        public boolean updatePricing(long itemId, BigDecimal price, BigDecimal discountRate) {
            updatedItemId = itemId;
            updatedPrice = price;
            updatedDiscount = discountRate;
            return true;
        }

        @Override
        public Optional<Item> findById(long itemId) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setTitle("测试景点");
            return Optional.of(item);
        }
    }

    private static class CapturingDetailDAO extends DetailDAO {
        private String savedDescription;
        private List<String> savedImages;
        private Document savedMetadata;

        @Override
        public Document findByItemId(long itemId) {
            return new Document("item_id", itemId)
                    .append("description", "旧简介")
                    .append("images", List.of("cover.jpg"))
                    .append("metadata", new Document("open_time", "08:30-19:00"));
        }

        @Override
        public void upsertDetail(long itemId, String description, List<String> images, Document metadata) {
            savedDescription = description;
            savedImages = images;
            savedMetadata = metadata;
        }
    }

    private static class CapturingLogDAO extends LogDAO {
        private String actionType;
        private int writeCount;

        @Override
        public void recordAction(long userId, long itemId, String actionType, int durationSeconds,
                                 String clientType, String ip) {
            this.actionType = actionType;
            writeCount += 1;
        }
    }

    private static class TrackingConnection {
        private final List<Boolean> autoCommitValues = new ArrayList<>();
        private Connection connection;
        private boolean committed;
        private boolean rolledBack;
        private boolean closed;

        private static TrackingConnection create() {
            TrackingConnection tracking = new TrackingConnection();
            tracking.connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> tracking.invoke(proxy, method.getName(), args));
            return tracking;
        }

        private Object invoke(Object proxy, String methodName, Object[] args) {
            return switch (methodName) {
                case "setAutoCommit" -> {
                    autoCommitValues.add((Boolean) args[0]);
                    yield null;
                }
                case "commit" -> {
                    committed = true;
                    yield null;
                }
                case "rollback" -> {
                    rolledBack = true;
                    yield null;
                }
                case "close" -> {
                    closed = true;
                    yield null;
                }
                case "isClosed" -> closed;
                case "getAutoCommit" -> true;
                case "toString" -> "TrackingConnection";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Unexpected connection method: " + methodName);
            };
        }
    }
}
