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
import java.util.List;

public class BusinessService {
    private final CategoryDAO categoryDAO;
    private final ItemDAO itemDAO;
    private final OrderDAO orderDAO;
    private final DetailDAO detailDAO;
    private final LogDAO logDAO;
    private final CommentDAO commentDAO;
    private final ConnectionProvider connectionProvider;

    public BusinessService() {
        this(new CategoryDAO(), new ItemDAO(), new OrderDAO(), new DetailDAO(), new LogDAO(), new CommentDAO());
    }

    public BusinessService(CategoryDAO categoryDAO, ItemDAO itemDAO, OrderDAO orderDAO, DetailDAO detailDAO,
                           LogDAO logDAO, CommentDAO commentDAO) {
        this(categoryDAO, itemDAO, orderDAO, detailDAO, logDAO, commentDAO, MySQLDBUtil::getConnection);
    }

    public BusinessService(CategoryDAO categoryDAO, ItemDAO itemDAO, OrderDAO orderDAO, DetailDAO detailDAO,
                           LogDAO logDAO, CommentDAO commentDAO, ConnectionProvider connectionProvider) {
        this.categoryDAO = categoryDAO;
        this.itemDAO = itemDAO;
        this.orderDAO = orderDAO;
        this.detailDAO = detailDAO;
        this.logDAO = logDAO;
        this.commentDAO = commentDAO;
        this.connectionProvider = connectionProvider;
    }

    public long createCategory(String name, Long parentId) {
        String safeName = SecurityUtil.requireText(name, "分类名称", 50);
        Category category = new Category();
        category.setName(safeName);
        category.setParentId(parentId);
        return categoryDAO.create(category);
    }

    public List<Category> listCategories() {
        return categoryDAO.findAll();
    }

    public long createItem(String title, long categoryId, String description, List<String> images, Document metadata) {
        return createItem(title, categoryId, description, images, metadata, new BigDecimal("80.00"), BigDecimal.ZERO);
    }

    public long createItem(String title, long categoryId, String description, List<String> images, Document metadata,
                           BigDecimal price, BigDecimal discountRate) {
        String safeTitle = SecurityUtil.requireText(title, "景点标题", 200);
        Item item = new Item();
        item.setTitle(safeTitle);
        item.setCategoryId(categoryId);
        item.setPrice(normalizePrice(price));
        item.setDiscountRate(normalizeDiscount(discountRate));
        item.setStatus(1);
        long itemId = itemDAO.create(item);
        detailDAO.upsertDetail(itemId, SecurityUtil.normalizeText(description, 2000),
                images == null ? List.of() : images, metadata == null ? new Document() : metadata);
        return itemId;
    }

    public List<Item> searchItems(String keyword, Long categoryId, int limit, int offset) {
        return itemDAO.search(SecurityUtil.normalizeText(keyword, 100), categoryId, 1,
                SecurityUtil.normalizeLimit(limit, 20, 100), SecurityUtil.normalizeOffset(offset));
    }

    public List<Item> searchAllItemsForAdmin(String keyword, Long categoryId, int limit, int offset) {
        return itemDAO.search(SecurityUtil.normalizeText(keyword, 100), categoryId, null,
                SecurityUtil.normalizeLimit(limit, 20, 100), SecurityUtil.normalizeOffset(offset));
    }

    public boolean updateItemStatus(long itemId, int status) {
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("景点状态只能是上架或下架");
        }
        return itemDAO.updateStatus(itemId, status);
    }

    public boolean updateItemPricing(long itemId, BigDecimal price, BigDecimal discountRate) {
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        return itemDAO.updatePricing(itemId, normalizePrice(price), normalizeDiscount(discountRate));
    }

    public ItemDetailDTO getItemDetail(long userId, long itemId, String ip) {
        if (userId <= 0 || itemId <= 0) {
            throw new BusinessException("用户ID和景点ID必须大于 0");
        }
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
        return orderDAO.existsPaidOrder(userId, itemId);
    }

    public List<Order> listUserOrders(long userId, int limit, int offset) {
        if (userId <= 0) {
            throw new BusinessException("用户ID必须大于 0");
        }
        return orderDAO.findByUserId(userId, SecurityUtil.normalizeLimit(limit, 20, 100),
                SecurityUtil.normalizeOffset(offset));
    }

    public List<Order> searchOrders(Long userId, Long orderId, Integer status, int limit, int offset) {
        validateOrderFilters(userId, orderId, status);
        return orderDAO.search(userId, orderId, status, SecurityUtil.normalizeLimit(limit, 20, 100),
                SecurityUtil.normalizeOffset(offset));
    }

    public List<OrderViewDTO> searchOrderViews(Long userId, Long orderId, Integer status, int limit, int offset) {
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

    public boolean updateOrderStatus(long orderId, int status) {
        if (orderId <= 0) {
            throw new BusinessException("订单ID必须大于 0");
        }
        if (status < 0 || status > 3) {
            throw new BusinessException("订单状态不正确");
        }
        return orderDAO.updateStatus(orderId, status);
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
        if (!List.of("微信", "支付宝", "银行卡", "现金").contains(value)) {
            throw new BusinessException("付款方式只能选择微信、支付宝、银行卡或现金");
        }
        return value;
    }
}
