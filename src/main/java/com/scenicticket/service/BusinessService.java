package com.scenicticket.service;

import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mysql.CategoryDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dto.ItemDetailDTO;
import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import com.scenicticket.model.Order;
import com.scenicticket.util.ConnectionProvider;
import com.scenicticket.util.MySQLDBUtil;
import com.scenicticket.util.SecurityUtil;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BusinessService {
    private final CategoryDAO categoryDAO;
    private final ItemDAO itemDAO;
    private final OrderDAO orderDAO;
    private final DetailDAO detailDAO;
    private final LogDAO logDAO;
    private final CommentDAO commentDAO;
    private final ConnectionProvider connectionProvider;
    private final AuthorizationService authorizationService;

    public BusinessService() {
        this(new CategoryDAO(), new ItemDAO(), new OrderDAO(), new DetailDAO(), new LogDAO(), new CommentDAO());
    }

    public BusinessService(CategoryDAO categoryDAO, ItemDAO itemDAO, OrderDAO orderDAO, DetailDAO detailDAO,
                           LogDAO logDAO, CommentDAO commentDAO) {
        this(categoryDAO, itemDAO, orderDAO, detailDAO, logDAO, commentDAO, MySQLDBUtil::getConnection);
    }

    public BusinessService(CategoryDAO categoryDAO, ItemDAO itemDAO, OrderDAO orderDAO, DetailDAO detailDAO,
                           LogDAO logDAO, CommentDAO commentDAO, ConnectionProvider connectionProvider) {
        this(categoryDAO, itemDAO, orderDAO, detailDAO, logDAO, commentDAO, connectionProvider,
                new AuthorizationService());
    }

    public BusinessService(CategoryDAO categoryDAO, ItemDAO itemDAO, OrderDAO orderDAO, DetailDAO detailDAO,
                           LogDAO logDAO, CommentDAO commentDAO, ConnectionProvider connectionProvider,
                           AuthorizationService authorizationService) {
        this.categoryDAO = categoryDAO;
        this.itemDAO = itemDAO;
        this.orderDAO = orderDAO;
        this.detailDAO = detailDAO;
        this.logDAO = logDAO;
        this.commentDAO = commentDAO;
        this.connectionProvider = connectionProvider;
        this.authorizationService = authorizationService;
    }

    public long createCategory(long actorUserId, String name, Long parentId) {
        authorizationService.requireAdmin(actorUserId);
        String safeName = SecurityUtil.requireText(name, "分类名称", 50);
        validateCategoryParent(null, parentId);
        Category category = new Category();
        category.setName(safeName);
        category.setParentId(parentId);
        return categoryDAO.create(category);
    }

    public boolean updateCategory(long actorUserId, long categoryId, String name, Long parentId) {
        authorizationService.requireAdmin(actorUserId);
        if (categoryId <= 0) {
            throw new BusinessException("分类ID必须大于 0");
        }
        categoryDAO.findById(categoryId).orElseThrow(() -> new BusinessException("分类不存在"));
        validateCategoryParent(categoryId, parentId);
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setName(SecurityUtil.requireText(name, "分类名称", 50));
        category.setParentId(parentId);
        return categoryDAO.update(category);
    }

    public List<Category> listCategories() {
        return categoryDAO.findAll();
    }

    public long createItem(long actorUserId, String title, long categoryId, String description,
                           List<String> images, Document metadata) {
        return createItem(actorUserId, title, categoryId, description, images, metadata,
                new BigDecimal("80.00"), BigDecimal.ZERO);
    }

    public long createItem(long actorUserId, String title, long categoryId, String description,
                           List<String> images, Document metadata,
                           BigDecimal price, BigDecimal discountRate) {
        authorizationService.requireAdmin(actorUserId);
        String safeTitle = SecurityUtil.requireText(title, "景点标题", 200);
        requireCategory(categoryId);
        Item item = new Item();
        item.setTitle(safeTitle);
        item.setCategoryId(categoryId);
        item.setPrice(normalizePrice(price));
        item.setDiscountRate(normalizeDiscount(discountRate));
        item.setStatus(1);
        long itemId = itemDAO.create(item);
        try {
            detailDAO.upsertDetail(itemId, SecurityUtil.normalizeText(description, 2000),
                    normalizeImages(images), normalizeMetadata(metadata));
        } catch (RuntimeException detailFailure) {
            boolean takenOffline = false;
            try {
                takenOffline = itemDAO.updateStatus(itemId, 0);
            } catch (RuntimeException compensationFailure) {
                detailFailure.addSuppressed(compensationFailure);
            }
            String message = takenOffline
                    ? "景点基础数据已创建并自动下架，详情保存失败，请重试详情维护"
                    : "景点详情保存失败且自动下架未完成，请管理员立即检查该景点";
            throw new DBException(message, detailFailure);
        }
        return itemId;
    }

    public List<Item> searchItems(String keyword, Long categoryId, int limit, int offset) {
        return itemDAO.search(SecurityUtil.normalizeText(keyword, 100), categoryId, 1,
                SecurityUtil.normalizeLimit(limit, 20, 100), SecurityUtil.normalizeOffset(offset));
    }

    public List<Item> searchAllItemsForAdmin(long actorUserId, String keyword, Long categoryId, int limit, int offset) {
        authorizationService.requireAdmin(actorUserId);
        return itemDAO.search(SecurityUtil.normalizeText(keyword, 100), categoryId, null,
                SecurityUtil.normalizeLimit(limit, 20, 100), SecurityUtil.normalizeOffset(offset));
    }

    public boolean updateItemStatus(long actorUserId, long itemId, int status) {
        authorizationService.requireAdmin(actorUserId);
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("景点状态只能是上架或下架");
        }
        return itemDAO.updateStatus(itemId, status);
    }

    public boolean updateItemPricing(long actorUserId, long itemId, BigDecimal price, BigDecimal discountRate) {
        authorizationService.requireAdmin(actorUserId);
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        return itemDAO.updatePricing(itemId, normalizePrice(price), normalizeDiscount(discountRate));
    }

    public boolean updateItem(long actorUserId, long itemId, String title, long categoryId) {
        authorizationService.requireAdmin(actorUserId);
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        requireCategory(categoryId);
        Item item = itemDAO.findById(itemId).orElseThrow(() -> new BusinessException("景点不存在"));
        item.setTitle(SecurityUtil.requireText(title, "景点标题", 200));
        item.setCategoryId(categoryId);
        return itemDAO.update(item);
    }

    public String getItemDescription(long itemId) {
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        Document detail = detailDAO.findByItemId(itemId);
        return detail == null ? "" : SecurityUtil.normalizeText(detail.getString("description"), 2000);
    }

    public Document getItemDetailForAdmin(long actorUserId, long itemId) {
        authorizationService.requireAdmin(actorUserId);
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        itemDAO.findById(itemId).orElseThrow(() -> new BusinessException("景点不存在"));
        Document detail = detailDAO.findByItemId(itemId);
        return detail == null ? new Document() : new Document(detail);
    }

    public boolean updateItemDescription(long actorUserId, long itemId, String description) {
        authorizationService.requireAdmin(actorUserId);
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        itemDAO.findById(itemId).orElseThrow(() -> new BusinessException("景点不存在"));
        Document existing = detailDAO.findByItemId(itemId);
        List<String> images = existing == null ? null : existing.getList("images", String.class);
        Document metadata = existing == null ? null : existing.get("metadata", Document.class);
        detailDAO.upsertDetail(itemId, SecurityUtil.requireText(description, "景点简介", 2000),
                normalizeImages(images), normalizeMetadata(metadata));
        return true;
    }

    public boolean updateItemDetail(long actorUserId, long itemId, String description,
                                    List<String> images, Document metadata) {
        authorizationService.requireAdmin(actorUserId);
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        itemDAO.findById(itemId).orElseThrow(() -> new BusinessException("景点不存在"));
        detailDAO.upsertDetail(itemId, SecurityUtil.requireText(description, "景点简介", 2000),
                normalizeImages(images), normalizeMetadata(metadata));
        return true;
    }

    private void validateCategoryParent(Long categoryId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId <= 0) {
            throw new BusinessException("上级分类ID必须大于 0");
        }
        requireCategory(parentId);
        if (categoryId == null) {
            return;
        }
        if (categoryId.equals(parentId)) {
            throw new BusinessException("分类不能把自己设为上级分类");
        }
        Map<Long, Long> parents = new HashMap<>();
        for (Category category : categoryDAO.findAll()) {
            parents.put(category.getCategoryId(), category.getParentId());
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && visited.add(cursor)) {
            if (categoryId.equals(cursor)) {
                throw new BusinessException("分类层级不能形成循环");
            }
            cursor = parents.get(cursor);
        }
        if (cursor != null) {
            throw new BusinessException("现有分类层级包含循环，请先修复分类数据");
        }
    }

    private Category requireCategory(long categoryId) {
        if (categoryId <= 0) {
            throw new BusinessException("分类ID必须大于 0");
        }
        return categoryDAO.findById(categoryId).orElseThrow(() -> new BusinessException("分类不存在"));
    }

    private List<String> normalizeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String image : images) {
            String value = SecurityUtil.normalizeText(image, 500);
            if (value != null && !value.isBlank() && !normalized.contains(value)) {
                normalized.add(value);
            }
            if (normalized.size() > 20) {
                throw new BusinessException("景点图片最多保存 20 个地址");
            }
        }
        return List.copyOf(normalized);
    }

    private Document normalizeMetadata(Document metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new Document();
        }
        if (metadata.size() > 50) {
            throw new BusinessException("景点扩展属性最多保存 50 项");
        }
        Document normalized = new Document();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = SecurityUtil.requireText(entry.getKey(), "扩展属性名称", 100);
            normalized.put(key, entry.getValue());
        }
        return normalized;
    }

    public ItemDetailDTO getItemDetail(long userId, long itemId, String ip) {
        if (userId <= 0 || itemId <= 0) {
            throw new BusinessException("用户ID和景点ID必须大于 0");
        }
        authorizationService.requireActiveUser(userId);
        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new BusinessException("景点不存在"));
        logDAO.recordAction(userId, itemId, "VIEW", 0, "SWING", SecurityUtil.normalizeIp(ip));
        ItemDetailDTO dto = new ItemDetailDTO();
        dto.setItem(item);
        dto.setDetail(detailDAO.findByItemId(itemId));
        dto.setComments(commentDAO.findByItemId(itemId, 20));
        return dto;
    }

    public long createOrder(long userId, long itemId, int quantity, String paymentMethod) {
        if (userId <= 0 || itemId <= 0) {
            throw new BusinessException("用户ID和景点ID必须大于 0");
        }
        authorizationService.requireActiveUser(userId);
        if (quantity <= 0 || quantity > 99) {
            throw new BusinessException("购买票数必须是 1 到 99 之间的整数");
        }
        String safePaymentMethod = normalizePaymentMethod(paymentMethod);
        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new BusinessException("景点不存在"));
        if (item.getStatus() == null || item.getStatus() != 1) {
            throw new BusinessException("该景点未上架，暂不能购买");
        }
        BigDecimal unitPrice = normalizePrice(item.getPrice());
        BigDecimal discountRate = normalizeDiscount(item.getDiscountRate());
        BigDecimal amount = calculateOrderAmount(unitPrice, discountRate, quantity);
        try (Connection connection = connectionProvider.getConnection()) {
            try {
                connection.setAutoCommit(false);
                Order order = new Order();
                order.setUserId(userId);
                order.setItemId(itemId);
                order.setAmount(amount);
                order.setQuantity(quantity);
                order.setUnitPrice(unitPrice);
                order.setDiscountRate(discountRate);
                order.setPaymentMethod(safePaymentMethod);
                order.setStatus(1);
                long orderId = orderDAO.create(connection, order);
                connection.commit();
                logDAO.recordAction(userId, itemId, "ORDER", 0, "SWING", "127.0.0.1");
                return orderId;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new DBException("Failed to create order.", e);
            } catch (RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DBException("Failed to create order transaction.", e);
        }
    }

    public boolean canComment(long userId, long itemId) {
        if (userId <= 0 || itemId <= 0) {
            throw new BusinessException("用户ID和景点ID必须大于 0");
        }
        authorizationService.requireActiveUser(userId);
        return orderDAO.existsPaidOrder(userId, itemId);
    }

    public List<Order> listUserOrders(long userId, int limit, int offset) {
        if (userId <= 0) {
            throw new BusinessException("用户ID必须大于 0");
        }
        authorizationService.requireActiveUser(userId);
        return orderDAO.findByUserId(userId, SecurityUtil.normalizeLimit(limit, 20, 100),
                SecurityUtil.normalizeOffset(offset));
    }

    public List<Order> searchOrders(long actorUserId, Long userId, Long orderId, Integer status, int limit, int offset) {
        validateOrderReadAccess(actorUserId, userId);
        validateOrderFilters(userId, orderId, status);
        return orderDAO.search(userId, orderId, status, SecurityUtil.normalizeLimit(limit, 20, 100),
                SecurityUtil.normalizeOffset(offset));
    }

    public List<OrderViewDTO> searchOrderViews(long actorUserId, Long userId, Long orderId, Integer status,
                                               int limit, int offset) {
        validateOrderReadAccess(actorUserId, userId);
        validateOrderFilters(userId, orderId, status);
        return orderDAO.searchViews(userId, orderId, status, SecurityUtil.normalizeLimit(limit, 20, 100),
                SecurityUtil.normalizeOffset(offset));
    }

    private void validateOrderFilters(Long userId, Long orderId, Integer status) {
        if (userId != null && userId <= 0) {
            throw new BusinessException("用户ID必须大于 0");
        }
        if (orderId != null && orderId <= 0) {
            throw new BusinessException("订单ID必须大于 0");
        }
        if (status != null && (status < 0 || status > 3)) {
            throw new BusinessException("订单状态不正确");
        }
    }

    public boolean updateOrderStatus(long actorUserId, long orderId, int status) {
        authorizationService.requireAdmin(actorUserId);
        if (orderId <= 0) {
            throw new BusinessException("订单ID必须大于 0");
        }
        if (status < 0 || status > 3) {
            throw new BusinessException("订单状态不正确");
        }
        return orderDAO.updateStatus(orderId, status);
    }

    private void validateOrderReadAccess(long actorUserId, Long requestedUserId) {
        var actor = authorizationService.requireActiveUser(actorUserId);
        if (!"ADMIN".equals(actor.getRole())) {
            if (requestedUserId == null || requestedUserId != actorUserId) {
                throw new BusinessException("普通用户只能查询自己的订单");
            }
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            throw new DBException("Failed to rollback order transaction.", rollbackException);
        }
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("票价不能小于 0");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeDiscount(BigDecimal discountRate) {
        BigDecimal safeDiscount = discountRate == null ? BigDecimal.ZERO : discountRate;
        if (safeDiscount.compareTo(BigDecimal.ZERO) < 0 || safeDiscount.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("折扣必须在 0 到 100 之间，0 表示不打折");
        }
        return safeDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOrderAmount(BigDecimal unitPrice, BigDecimal discountRate, int quantity) {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(discountMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String value = SecurityUtil.requireText(paymentMethod, "付款方式", 20);
        if (!List.of("微信", "支付宝", "银行卡").contains(value)) {
            throw new BusinessException("付款方式只能选择微信、支付宝或银行卡");
        }
        return value;
    }
}
