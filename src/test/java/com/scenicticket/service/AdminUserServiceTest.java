package com.scenicticket.service;

import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.ProfileDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.dto.AdminChangeResult;
import com.scenicticket.dto.AdminUserDetailDTO;
import com.scenicticket.dto.UserOrderSummaryDTO;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Profile;
import com.scenicticket.model.User;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUserServiceTest {
    @Test
    void ordinaryUserCannotSearchAdministratorUsers() {
        Fixture fixture = fixture(users(admin(1L), user(2L)));

        assertThrows(BusinessException.class, () -> fixture.service.searchUsers(
                2L, new UserSearchCriteria(null, null, null, null, 50, 0)));
        assertEquals(0, fixture.userDAO.searchCount);
    }

    @Test
    void administratorCannotDisableOrDemoteSelf() {
        Fixture selfFixture = fixture(users(admin(1L), user(2L)));
        assertThrows(BusinessException.class, () -> selfFixture.service.changeUserStatus(1L, 1L, 0));
        assertTrue(selfFixture.connection.rolledBack);

        Fixture demotionFixture = fixture(users(admin(1L), user(2L)));
        assertThrows(BusinessException.class, () -> demotionFixture.service.changeUserRole(1L, 1L, "USER"));
    }

    @Test
    void statusAndRoleChangesCommitAndWriteAudit() {
        Fixture fixture = fixture(users(admin(1L), admin(2L), user(3L)));

        AdminChangeResult disabled = fixture.service.changeUserStatus(1L, 2L, 0);
        AdminChangeResult promoted = fixture.service.changeUserRole(1L, 3L, "ADMIN");

        assertTrue(disabled.updated());
        assertTrue(disabled.auditRecorded());
        assertEquals(0, fixture.userDAO.users.get(2L).getStatus());
        assertTrue(promoted.updated());
        assertEquals("ADMIN", fixture.userDAO.users.get(3L).getRole());
        assertEquals(List.of("USER_STATUS_UPDATE", "USER_ROLE_UPDATE"), fixture.systemLogDAO.types);
        assertTrue(fixture.connection.commits >= 2);
    }

    @Test
    void committedMysqlChangeReportsMongoAuditFailureWithoutRollback() {
        Fixture fixture = fixture(users(admin(1L), user(2L)));
        fixture.systemLogDAO.fail = true;

        AdminChangeResult result = fixture.service.changeUserStatus(1L, 2L, 0);

        assertTrue(result.updated());
        assertFalse(result.auditRecorded());
        assertTrue(result.message().contains("审计日志写入失败"));
        assertEquals(0, fixture.userDAO.users.get(2L).getStatus());
        assertEquals(1, fixture.connection.commits);
    }

    @Test
    void userDetailIncludesMysqlSummaryAndDegradesWhenMongoUnavailable() {
        Fixture fixture = fixture(users(admin(1L), user(2L)));
        fixture.logDAO.fail = true;

        AdminUserDetailDTO detail = fixture.service.getUserDetail(1L, 2L);

        assertEquals(2L, detail.getUser().getUserId());
        assertEquals("测试用户", detail.getProfile().getRealName());
        assertEquals(4, detail.getOrderSummary().getTotalOrders());
        assertEquals(new BigDecimal("188.00"), detail.getOrderSummary().getPaidAmount());
        assertFalse(detail.isBehaviorDataAvailable());
    }

    @Test
    void searchNormalizesAndPassesAllFilters() {
        Fixture fixture = fixture(users(admin(1L), user(2L)));

        fixture.service.searchUsers(1L, new UserSearchCriteria("  user  ", " example.com ", "user", 1, 999, -4));

        assertEquals("user", fixture.userDAO.username);
        assertEquals("example.com", fixture.userDAO.email);
        assertEquals("USER", fixture.userDAO.role);
        assertEquals(1, fixture.userDAO.status);
        assertEquals(200, fixture.userDAO.limit);
        assertEquals(0, fixture.userDAO.offset);
    }

    private static Fixture fixture(Map<Long, User> users) {
        FakeUserDAO userDAO = new FakeUserDAO(users);
        FakeProfileDAO profileDAO = new FakeProfileDAO();
        FakeOrderDAO orderDAO = new FakeOrderDAO();
        FakeLogDAO logDAO = new FakeLogDAO();
        FakeSystemLogDAO systemLogDAO = new FakeSystemLogDAO();
        TrackingConnection connection = TrackingConnection.create();
        AuthorizationService authorization = new AuthorizationService(userDAO);
        AdminUserService service = new AdminUserService(userDAO, profileDAO, orderDAO, logDAO, systemLogDAO,
                authorization, () -> connection.connection);
        return new Fixture(service, userDAO, logDAO, systemLogDAO, connection);
    }

    private static Map<Long, User> users(User... users) {
        Map<Long, User> map = new LinkedHashMap<>();
        for (User user : users) {
            map.put(user.getUserId(), user);
        }
        return map;
    }

    private static User admin(long id) { return user(id, "ADMIN", 1); }
    private static User user(long id) { return user(id, "USER", 1); }

    private static User user(long id, String role, int status) {
        User user = new User();
        user.setUserId(id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.com");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private record Fixture(AdminUserService service, FakeUserDAO userDAO, FakeLogDAO logDAO,
                           FakeSystemLogDAO systemLogDAO, TrackingConnection connection) {
    }

    private static final class FakeUserDAO extends UserDAO {
        private final Map<Long, User> users;
        private int searchCount;
        private String username;
        private String email;
        private String role;
        private Integer status;
        private int limit;
        private int offset;

        private FakeUserDAO(Map<Long, User> users) { this.users = users; }

        @Override
        public Optional<User> findById(long userId) { return Optional.ofNullable(users.get(userId)); }

        @Override
        public Optional<User> findByIdForUpdate(Connection connection, long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public List<Long> lockActiveAdminIds(Connection connection) {
            return users.values().stream()
                    .filter(user -> "ADMIN".equals(user.getRole()) && user.getStatus() != null && user.getStatus() == 1)
                    .map(User::getUserId)
                    .toList();
        }

        @Override
        public boolean updateStatus(Connection connection, long userId, int status) {
            users.get(userId).setStatus(status);
            return true;
        }

        @Override
        public boolean updateRole(Connection connection, long userId, String role) {
            users.get(userId).setRole(role);
            return true;
        }

        @Override
        public List<User> search(String username, String email, String role, Integer status, int limit, int offset) {
            searchCount += 1;
            this.username = username;
            this.email = email;
            this.role = role;
            this.status = status;
            this.limit = limit;
            this.offset = offset;
            return new ArrayList<>(users.values());
        }
    }

    private static final class FakeProfileDAO extends ProfileDAO {
        @Override
        public Optional<Profile> findByUserId(long userId) {
            Profile profile = new Profile();
            profile.setUserId(userId);
            profile.setRealName("测试用户");
            return Optional.of(profile);
        }
    }

    private static final class FakeOrderDAO extends OrderDAO {
        @Override
        public UserOrderSummaryDTO summarizeByUserId(long userId) {
            UserOrderSummaryDTO summary = new UserOrderSummaryDTO();
            summary.setTotalOrders(4);
            summary.setPaidOrders(2);
            summary.setPaidAmount(new BigDecimal("188.00"));
            return summary;
        }
    }

    private static final class FakeLogDAO extends LogDAO {
        private boolean fail;

        @Override
        public long countByUserId(long userId) {
            if (fail) {
                throw new IllegalStateException("Mongo unavailable");
            }
            return 7;
        }
    }

    private static final class FakeSystemLogDAO extends SystemLogDAO {
        private final List<String> types = new ArrayList<>();
        private boolean fail;

        @Override
        public void record(long userId, String logType, String logLevel, String message, Document actionDetail) {
            if (fail) {
                throw new IllegalStateException("Mongo unavailable");
            }
            types.add(logType);
        }
    }

    private static final class TrackingConnection {
        private Connection connection;
        private int commits;
        private boolean rolledBack;

        private static TrackingConnection create() {
            TrackingConnection tracking = new TrackingConnection();
            tracking.connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> tracking.invoke(proxy, method.getName(), args));
            return tracking;
        }

        private Object invoke(Object proxy, String method, Object[] args) {
            return switch (method) {
                case "setAutoCommit", "close" -> null;
                case "commit" -> { commits += 1; yield null; }
                case "rollback" -> { rolledBack = true; yield null; }
                case "isClosed" -> false;
                case "getAutoCommit" -> true;
                case "toString" -> "TrackingConnection";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Unexpected connection method: " + method);
            };
        }
    }
}
