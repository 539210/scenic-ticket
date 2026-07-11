package com.scenicticket.service;

import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.AdmissionDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dto.AdmissionResult;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Admission;
import com.scenicticket.model.Order;
import com.scenicticket.util.ConnectionProvider;
import com.scenicticket.util.MySQLDBUtil;
import com.scenicticket.util.SecurityUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class AdmissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdmissionService.class);
    private final AdmissionDAO admissionDAO;
    private final OrderDAO orderDAO;
    private final SystemLogDAO systemLogDAO;
    private final AuthorizationService authorizationService;
    private final ConnectionProvider connectionProvider;
    private final Clock clock;

    public AdmissionService() {
        this(new AdmissionDAO(), new OrderDAO(), new SystemLogDAO(), new AuthorizationService(),
                MySQLDBUtil::getConnection, Clock.systemDefaultZone());
    }

    public AdmissionService(AdmissionDAO admissionDAO, OrderDAO orderDAO, SystemLogDAO systemLogDAO,
                            AuthorizationService authorizationService, ConnectionProvider connectionProvider,
                            Clock clock) {
        this.admissionDAO = admissionDAO;
        this.orderDAO = orderDAO;
        this.systemLogDAO = systemLogDAO;
        this.authorizationService = authorizationService;
        this.connectionProvider = connectionProvider;
        this.clock = clock;
    }

    public AdmissionResult admit(long actorUserId, long orderId, int quantity, String note) {
        authorizationService.requireAdmin(actorUserId);
        if (orderId <= 0) throw new BusinessException("订单ID必须大于 0");
        if (quantity <= 0 || quantity > 99) throw new BusinessException("核销数量必须是 1 到 99 之间的整数");
        String safeNote = SecurityUtil.normalizeText(note, 500);
        int admitted;
        int remaining;
        boolean completed;
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Order order = orderDAO.findByIdForUpdate(connection, orderId)
                        .orElseThrow(() -> new BusinessException("订单不存在"));
                if (order.getStatus() == null || order.getStatus() != 1) {
                    throw new BusinessException("只有已支付且未完成的订单可以核销");
                }
                if (order.getRefundedAt() != null) throw new BusinessException("订单已经退款，不能核销");
                if (!LocalDate.now(clock).equals(order.getVisitDate())) {
                    throw new BusinessException("只能在订单游玩日期当天核销");
                }
                int alreadyAdmitted = admissionDAO.sumQuantity(connection, orderId);
                remaining = order.getQuantity() - alreadyAdmitted;
                if (remaining <= 0) throw new BusinessException("订单门票已经全部核销");
                if (quantity > remaining) throw new BusinessException("核销数量超过剩余可核销数量：" + remaining);
                Admission admission = new Admission();
                admission.setOrderId(orderId);
                admission.setQuantity(quantity);
                admission.setOperatorUserId(actorUserId);
                admission.setNote(safeNote);
                admissionDAO.create(connection, admission);
                admitted = alreadyAdmitted + quantity;
                remaining = order.getQuantity() - admitted;
                completed = remaining == 0;
                if (completed && !orderDAO.markCompleted(connection, orderId)) {
                    throw new BusinessException("订单状态已变化，请刷新后重试");
                }
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new DBException("门票核销失败", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DBException("门票核销事务失败", exception);
        }
        boolean audited = safeAudit(actorUserId, orderId, quantity, admitted, completed);
        String message = completed ? "门票已全部核销，订单已完成" : "本次核销成功，剩余 " + remaining + " 张";
        return new AdmissionResult(orderId, admitted, remaining, completed, audited,
                audited ? message : message + "；但审计日志写入失败");
    }

    public List<Admission> listByOrder(long actorUserId, long orderId) {
        authorizationService.requireAdmin(actorUserId);
        if (orderId <= 0) throw new BusinessException("订单ID必须大于 0");
        return admissionDAO.findByOrderId(orderId);
    }

    private boolean safeAudit(long actorUserId, long orderId, int quantity, int admitted, boolean completed) {
        try {
            systemLogDAO.record(actorUserId, "ADMISSION", "INFO", "门票核销成功",
                    new Document("order_id", orderId).append("quantity", quantity)
                            .append("admitted_quantity", admitted).append("completed", completed));
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("Admission committed but audit write failed for order {}", orderId, exception);
            return false;
        }
    }

    private void rollback(Connection connection, Exception original) {
        try { connection.rollback(); } catch (SQLException rollbackFailure) { original.addSuppressed(rollbackFailure); }
    }
}
