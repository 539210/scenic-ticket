package com.scenicticket.service;

import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mysql.CategoryDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dto.ItemDetailDTO;
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
        String safeName = SecurityUtil.requireText(name, "Category name", 50);
        Category category = new Category();
        category.setName(safeName);
        category.setParentId(parentId);
        return categoryDAO.create(category);
    }

    public List<Category> listCategories() {
        return categoryDAO.findAll();
    }

    public long createItem(String title, long categoryId, String description, List<String> images, Document metadata) {
        String safeTitle = SecurityUtil.requireText(title, "Item title", 200);
        Item item = new Item();
        item.setTitle(safeTitle);
        item.setCategoryId(categoryId);
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

    public boolean updateItemStatus(long itemId, int status) {
        if (itemId <= 0) {
            throw new BusinessException("Item id must be positive.");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("Item status must be 0 or 1.");
        }
        return itemDAO.updateStatus(itemId, status);
    }

    public ItemDetailDTO getItemDetail(long userId, long itemId, String ip) {
        if (userId <= 0 || itemId <= 0) {
            throw new BusinessException("User id and item id must be positive.");
        }
        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new BusinessException("Item not found."));
        logDAO.recordAction(userId, itemId, "VIEW", 0, "SWING", SecurityUtil.normalizeIp(ip));
        ItemDetailDTO dto = new ItemDetailDTO();
        dto.setItem(item);
        dto.setDetail(detailDAO.findByItemId(itemId));
        dto.setComments(commentDAO.findByItemId(itemId, 20));
        return dto;
    }

    public long createOrder(long userId, long itemId, BigDecimal amount) {
        if (userId <= 0 || itemId <= 0) {
            throw new BusinessException("User id and item id must be positive.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Order amount must be non-negative.");
        }
        try (Connection connection = connectionProvider.getConnection()) {
            try {
                connection.setAutoCommit(false);
                Order order = new Order();
                order.setUserId(userId);
                order.setItemId(itemId);
                order.setAmount(amount);
                order.setStatus(0);
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

    public List<Order> listUserOrders(long userId, int limit, int offset) {
        if (userId <= 0) {
            throw new BusinessException("User id must be positive.");
        }
        return orderDAO.findByUserId(userId, SecurityUtil.normalizeLimit(limit, 20, 100),
                SecurityUtil.normalizeOffset(offset));
    }

    public boolean updateOrderStatus(long orderId, int status) {
        if (orderId <= 0) {
            throw new BusinessException("Order id must be positive.");
        }
        if (status < 0 || status > 3) {
            throw new BusinessException("Order status must be between 0 and 3.");
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
}
