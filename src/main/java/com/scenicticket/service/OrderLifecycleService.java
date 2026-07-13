package com.scenicticket.service;

import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.RefundDAO;
import com.scenicticket.dao.mysql.TicketInventoryDAO;
import com.scenicticket.dao.mysql.TicketTypeDAO;
import com.scenicticket.dto.OrderActionResult;
import com.scenicticket.dto.PendingOrderResult;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Item;
import com.scenicticket.model.Order;
import com.scenicticket.model.Refund;
import com.scenicticket.model.TicketType;
import com.scenicticket.model.User;
import com.scenicticket.util.ConnectionProvider;
import com.scenicticket.util.MySQLDBUtil;
import com.scenicticket.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderLifecycleService {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_CANCELLED = 2;
    public static final int STATUS_COMPLETED = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderLifecycleService.class);

    private final OrderDAO orderDAO;
    private final TicketTypeDAO ticketTypeDAO;
    private final TicketInventoryDAO inventoryDAO;
    private final ItemDAO itemDAO;
    private final RefundDAO refundDAO;
    private final LogDAO logDAO;
    private final AuthorizationService authorizationService;
    private final ConnectionProvider connectionProvider;
    private final Clock clock;

    public OrderLifecycleService() {
        this(new OrderDAO(), new TicketTypeDAO(), new TicketInventoryDAO(), new ItemDAO(), new RefundDAO(),
                new LogDAO(), new AuthorizationService(), MySQLDBUtil::getConnection, Clock.systemDefaultZone());
    }

    public OrderLifecycleService(OrderDAO orderDAO, TicketTypeDAO ticketTypeDAO, TicketInventoryDAO inventoryDAO,
                                 ItemDAO itemDAO, RefundDAO refundDAO, LogDAO logDAO,
                                 AuthorizationService authorizationService, ConnectionProvider connectionProvider,
                                 Clock clock) {
        this.orderDAO = orderDAO;
        this.ticketTypeDAO = ticketTypeDAO;
        this.inventoryDAO = inventoryDAO;
        this.itemDAO = itemDAO;
        this.refundDAO = refundDAO;
        this.logDAO = logDAO;
        this.authorizationService = authorizationService;
        this.connectionProvider = connectionProvider;
        this.clock = clock;
    }

    public PendingOrderResult createPendingOrder(long userId, long ticketTypeId, LocalDate visitDate,
                                                 int quantity, String paymentMethod, String ip) {
        authorizationService.requireActiveUser(userId);
        validateVisitDate(visitDate);
        validateQuantity(quantity);
        String safePaymentMethod = normalizePaymentMethod(paymentMethod);
        long orderId;
        long itemId;
        BigDecimal totalAmount;
        String ticketTypeName;
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                TicketType ticketType = ticketTypeDAO.findById(connection, ticketTypeId)
                        .orElseThrow(() -> new BusinessException("票种不存在"));
                if (ticketType.getStatus() == null || ticketType.getStatus() != 1) {
                    throw new BusinessException("票种当前未上架");
                }
                Item item = itemDAO.findById(connection, ticketType.getItemId())
                        .orElseThrow(() -> new BusinessException("景点不存在"));
                if (item.getStatus() == null || item.getStatus() != 1) {
                    throw new BusinessException("景点当前未上架");
                }
                inventoryDAO.reserve(connection, ticketTypeId, visitDate, quantity);
                BigDecimal discountedPrice = discountedPrice(ticketType);
                Order order = new Order();
                order.setUserId(userId);
                order.setItemId(ticketType.getItemId());
                order.setTicketTypeId(ticketTypeId);
                order.setTicketTypeNameSnapshot(ticketType.getName());
                order.setOriginalUnitPrice(ticketType.getOriginalPrice());
                order.setDiscountRate(ticketType.getDiscountRate());
                order.setDiscountedUnitPrice(discountedPrice);
                order.setUnitPrice(discountedPrice);
                order.setQuantity(quantity);
                order.setAmount(discountedPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP));
                order.setPaymentMethod(safePaymentMethod);
                order.setVisitDate(visitDate);
                order.setExpiresAt(LocalDateTime.now(clock).plusMinutes(15));
                order.setStatusVersion(0);
                order.setStatus(STATUS_PENDING);
                orderId = orderDAO.create(connection, order);
                itemId = ticketType.getItemId();
                totalAmount = order.getAmount();
                ticketTypeName = order.getTicketTypeNameSnapshot();
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new DBException("创建待支付订单失败", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DBException("创建待支付订单事务失败", exception);
        }
        boolean audited = safeAudit(userId, itemId, "ORDER_CREATE", ip);
        String message = "待支付订单已创建，请在 15 分钟内确认支付";
        return new PendingOrderResult(orderId, totalAmount, ticketTypeName, visitDate, quantity, audited,
                audited ? message : message + "；但审计日志写入失败，请联系管理员");
    }

    public OrderActionResult pay(long actorUserId, long orderId, String ip) {
        User actor = authorizationService.requireActiveUser(actorUserId);
        Order order;
        boolean expired = false;
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                order = requireLockedOrder(connection, orderId);
                requireOwnerOrAdmin(actor, order.getUserId());
                requireStatus(order, STATUS_PENDING, "只有待支付订单可以支付");
                if (!order.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
                    boolean released = inventoryDAO.releaseReservationIfPresent(
                            connection, order.getTicketTypeId(), order.getVisitDate(), order.getQuantity());
                    if (!released) {
                        LOGGER.warn("Expired legacy order {} had no matching reserved stock; cancelling without stock inflation",
                                orderId);
                    }
                    orderDAO.markCancelled(connection, orderId, STATUS_PENDING, false);
                    expired = true;
                } else {
                    inventoryDAO.confirmSale(connection, order.getTicketTypeId(), order.getVisitDate(), order.getQuantity());
                    if (!orderDAO.markPaid(connection, orderId)) {
                        throw new BusinessException("订单状态已变化，请刷新后重试");
                    }
                }
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new DBException("确认支付失败", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DBException("确认支付事务失败", exception);
        }
        String action = expired ? "ORDER_EXPIRE" : "ORDER_PAY";
        boolean audited = safeAudit(actorUserId, order.getItemId(), action, ip);
        return result(orderId, true, audited, expired ? "订单已过期并释放库存，不能继续支付" : "模拟支付成功");
    }

    public OrderActionResult cancelPending(long actorUserId, long orderId, String ip) {
        User actor = authorizationService.requireActiveUser(actorUserId);
        Order order;
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                order = requireLockedOrder(connection, orderId);
                requireOwnerOrAdmin(actor, order.getUserId());
                requireStatus(order, STATUS_PENDING, "只有待支付订单可以直接取消；已支付订单请申请退款");
                inventoryDAO.releaseReservation(connection, order.getTicketTypeId(), order.getVisitDate(), order.getQuantity());
                if (!orderDAO.markCancelled(connection, orderId, STATUS_PENDING, false)) {
                    throw new BusinessException("订单状态已变化，请刷新后重试");
                }
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new DBException("取消订单失败", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DBException("取消订单事务失败", exception);
        }
        boolean audited = safeAudit(actorUserId, order.getItemId(), "ORDER_CANCEL", ip);
        return result(orderId, true, audited, "订单已取消，预留库存已释放");
    }

    public OrderActionResult refund(long actorUserId, long orderId, String reason, String ip) {
        User actor = authorizationService.requireActiveUser(actorUserId);
        String safeReason = SecurityUtil.requireText(reason, "退款原因", 500);
        Order order;
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                order = requireLockedOrder(connection, orderId);
                requireOwnerOrAdmin(actor, order.getUserId());
                requireStatus(order, STATUS_PAID, "只有已支付且未完成的订单可以退款");
                if (!LocalDate.now(clock).isBefore(order.getVisitDate())) {
                    throw new BusinessException("已到或超过游玩日期，不能退款");
                }
                if (orderDAO.hasAdmissions(connection, orderId)) {
                    throw new BusinessException("门票已经核销，不能退款");
                }
                if (refundDAO.findByOrderId(connection, orderId).isPresent()) {
                    throw new BusinessException("订单已经退款，不能重复退款");
                }
                inventoryDAO.restoreSold(connection, order.getTicketTypeId(), order.getVisitDate(), order.getQuantity());
                Refund refund = new Refund();
                refund.setOrderId(orderId);
                refund.setRefundAmount(order.getAmount());
                refund.setReason(safeReason);
                refund.setRefundStatus("SUCCESS");
                refund.setOperatorUserId(actorUserId);
                refundDAO.create(connection, refund);
                if (!orderDAO.markCancelled(connection, orderId, STATUS_PAID, true)) {
                    throw new BusinessException("订单状态已变化，请刷新后重试");
                }
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new DBException("模拟退款失败", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DBException("模拟退款事务失败", exception);
        }
        boolean audited = safeAudit(actorUserId, order.getItemId(), "ORDER_REFUND", ip);
        return result(orderId, true, audited, "模拟退款成功，库存已恢复");
    }

    public int expireDueOrders(int limit) {
        int safeLimit = SecurityUtil.normalizeLimit(limit, 100, 500);
        int expired = 0;
        for (Long orderId : orderDAO.findExpiredPendingIds(safeLimit)) {
            if (expireOne(orderId)) {
                expired += 1;
            }
        }
        return expired;
    }

    private boolean expireOne(long orderId) {
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Order order = requireLockedOrder(connection, orderId);
                if (order.getStatus() != STATUS_PENDING || order.getExpiresAt() == null
                        || order.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
                    connection.rollback();
                    return false;
                }
                boolean released = inventoryDAO.releaseReservationIfPresent(
                        connection, order.getTicketTypeId(), order.getVisitDate(), order.getQuantity());
                if (!released) {
                    LOGGER.warn("Expired legacy order {} had no matching reserved stock; cancelling without stock inflation",
                            orderId);
                }
                orderDAO.markCancelled(connection, orderId, STATUS_PENDING, false);
                connection.commit();
                safeAudit(order.getUserId(), order.getItemId(), "ORDER_EXPIRE", "127.0.0.1");
                return true;
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                LOGGER.warn("Failed to expire pending order {}", orderId, exception);
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            LOGGER.warn("Failed to open expiry transaction for order {}", orderId, exception);
            return false;
        }
    }

    private Order requireLockedOrder(Connection connection, long orderId) throws SQLException {
        if (orderId <= 0) throw new BusinessException("订单ID必须大于 0");
        return orderDAO.findByIdForUpdate(connection, orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
    }

    private void requireStatus(Order order, int expected, String message) {
        if (order.getStatus() == null || order.getStatus() != expected) {
            throw new BusinessException(message);
        }
    }

    private void requireOwnerOrAdmin(User actor, long ownerUserId) {
        if (!java.util.Objects.equals(actor.getUserId(), ownerUserId) && !"ADMIN".equals(actor.getRole())) {
            throw new BusinessException("不能操作其他用户的订单");
        }
    }

    private void validateVisitDate(LocalDate visitDate) {
        if (visitDate == null || visitDate.isBefore(LocalDate.now(clock))) {
            throw new BusinessException("游玩日期必须是今天或未来日期");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0 || quantity > 99) throw new BusinessException("购买票数必须是 1 到 99 之间的整数");
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String value = SecurityUtil.requireText(paymentMethod, "付款方式", 20);
        if (!java.util.Set.of("微信", "支付宝", "银行卡").contains(value)) {
            throw new BusinessException("付款方式仅支持微信、支付宝或银行卡");
        }
        return value;
    }

    private BigDecimal discountedPrice(TicketType ticketType) {
        return ticketType.getOriginalPrice().multiply(BigDecimal.valueOf(100).subtract(ticketType.getDiscountRate()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private boolean safeAudit(long userId, long itemId, String action, String ip) {
        try {
            logDAO.recordAction(userId, itemId, action, 0, "SWING", SecurityUtil.normalizeIp(ip));
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("MySQL order operation committed but MongoDB audit failed: order action {}", action, exception);
            return false;
        }
    }

    private OrderActionResult result(long orderId, boolean updated, boolean audited, String message) {
        return new OrderActionResult(orderId, updated, audited,
                audited ? message : message + "；但审计日志写入失败，请联系管理员");
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
