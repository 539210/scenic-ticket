package com.scenicticket.service;

import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.AdmissionDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Admission;
import com.scenicticket.model.Order;
import com.scenicticket.model.User;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-11T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void partialAdmissionRecordsQuantityWithoutCompletingOrder() {
        Fixture fixture = new Fixture(order(3, 1, LocalDate.of(2026, 7, 11)));

        var result = fixture.service.admit(1L, 44L, 1, "东门入园");

        assertEquals(2, result.admittedQuantity());
        assertEquals(1, result.remainingQuantity());
        assertFalse(result.completed());
        assertEquals(1, fixture.admissions.created.getQuantity());
        assertFalse(fixture.orders.completed);
        assertTrue(fixture.connection.committed);
    }

    @Test
    void finalAdmissionCompletesOrder() {
        Fixture fixture = new Fixture(order(3, 1, LocalDate.of(2026, 7, 11)));

        var result = fixture.service.admit(1L, 44L, 2, "全部入园");

        assertTrue(result.completed());
        assertEquals(3, result.admittedQuantity());
        assertEquals(0, result.remainingQuantity());
        assertTrue(fixture.orders.completed);
    }

    @Test
    void rejectsWrongStatusDateAndExcessQuantity() {
        Fixture unpaid = new Fixture(order(3, 0, LocalDate.of(2026, 7, 11)));
        assertThrows(BusinessException.class, () -> unpaid.service.admit(1L, 44L, 1, "未支付"));

        Fixture wrongDate = new Fixture(order(3, 1, LocalDate.of(2026, 7, 12)));
        assertThrows(BusinessException.class, () -> wrongDate.service.admit(1L, 44L, 1, "日期不符"));

        Fixture excess = new Fixture(order(3, 1, LocalDate.of(2026, 7, 11)));
        excess.admissions.admitted = 2;
        assertThrows(BusinessException.class, () -> excess.service.admit(1L, 44L, 2, "超过剩余"));
        assertTrue(excess.connection.rolledBack);
    }

    @Test
    void auditFailureDoesNotUndoCompletedAdmission() {
        Fixture fixture = new Fixture(order(1, 1, LocalDate.of(2026, 7, 11)));
        fixture.admissions.admitted = 0;
        fixture.logs.fail = true;

        var result = fixture.service.admit(1L, 44L, 1, "审计故障测试");

        assertTrue(result.completed());
        assertFalse(result.auditRecorded());
        assertTrue(result.message().contains("审计日志写入失败"));
        assertTrue(fixture.connection.committed);
    }

    private static Order order(int quantity, int status, LocalDate visitDate) {
        Order order = new Order();
        order.setOrderId(44L);
        order.setQuantity(quantity);
        order.setStatus(status);
        order.setVisitDate(visitDate);
        return order;
    }

    private static class Fixture {
        private final FakeAdmissionDAO admissions = new FakeAdmissionDAO();
        private final FakeOrderDAO orders;
        private final FakeSystemLogDAO logs = new FakeSystemLogDAO();
        private final TrackingConnection connection = TrackingConnection.create();
        private final AdmissionService service;

        private Fixture(Order order) {
            orders = new FakeOrderDAO(order);
            service = new AdmissionService(admissions, orders, logs, authorization(),
                    () -> connection.connection, CLOCK);
        }
    }

    private static AuthorizationService authorization() {
        return new AuthorizationService() {
            @Override
            public User requireAdmin(long actorUserId) {
                User user = new User();
                user.setUserId(actorUserId);
                user.setRole("ADMIN");
                user.setStatus(1);
                return user;
            }
        };
    }

    private static class FakeAdmissionDAO extends AdmissionDAO {
        private int admitted = 1;
        private Admission created;
        @Override
        public int sumQuantity(Connection connection, long orderId) { return admitted; }
        @Override
        public long create(Connection connection, Admission admission) { created = admission; return 7L; }
    }

    private static class FakeOrderDAO extends OrderDAO {
        private final Order order;
        private boolean completed;
        private FakeOrderDAO(Order order) { this.order = order; }
        @Override
        public Optional<Order> findByIdForUpdate(Connection connection, long orderId) { return Optional.of(order); }
        @Override
        public boolean markCompleted(Connection connection, long orderId) { completed = true; return true; }
    }

    private static class FakeSystemLogDAO extends SystemLogDAO {
        private boolean fail;
        @Override
        public void record(long userId, String logType, String logLevel, String message, Document actionDetail) {
            if (fail) throw new IllegalStateException("Mongo unavailable");
        }
    }

    private static class TrackingConnection {
        private final List<Boolean> autoCommitValues = new ArrayList<>();
        private Connection connection;
        private boolean committed;
        private boolean rolledBack;
        private static TrackingConnection create() {
            TrackingConnection tracking = new TrackingConnection();
            tracking.connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit" -> { tracking.autoCommitValues.add((Boolean) args[0]); yield null; }
                        case "commit" -> { tracking.committed = true; yield null; }
                        case "rollback" -> { tracking.rolledBack = true; yield null; }
                        case "close" -> null;
                        case "isClosed" -> false;
                        case "unwrap" -> null;
                        case "isWrapperFor" -> false;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            return tracking;
        }
    }
}
