package com.scenicticket.service;

import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.ProfileDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.dto.LoginResult;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Profile;
import com.scenicticket.model.User;
import com.scenicticket.util.PasswordUtil;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {
    private final InMemoryUserDAO userDAO = new InMemoryUserDAO();
    private final CapturingProfileDAO profileDAO = new CapturingProfileDAO();
    private final CapturingSystemLogDAO systemLogDAO = new CapturingSystemLogDAO();
    private final UserService service = new UserService(userDAO, profileDAO, systemLogDAO);

    @Test
    void registerCreatesUserWithHashedPasswordAndAuditLog() {
        long userId = service.register("  alice  ", "password123", " alice@example.com ", "13900000000");

        User savedUser = userDAO.usersByName.get("alice");
        assertEquals(userId, savedUser.getUserId());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertTrue(PasswordUtil.verifyPassword("password123", savedUser.getPasswordHash()));
        assertEquals("REGISTER", systemLogDAO.lastLogType);
        assertEquals("INFO", systemLogDAO.lastLogLevel);
    }

    @Test
    void registerRejectsDuplicateUsernameAndInvalidEmail() {
        service.register("alice", "password123", "alice@example.com", "13900000000");

        assertThrows(BusinessException.class,
                () -> service.register("alice", "password123", "alice2@example.com", "13900000001"));
        assertThrows(BusinessException.class,
                () -> service.register("bob", "password123", "bad-email", "13900000002"));
        assertThrows(BusinessException.class,
                () -> service.register("bob", "password123", "bob@example.com", "12345"));
    }

    @Test
    void loginRecordsWarningOnWrongPasswordAndInfoOnSuccess() {
        long userId = service.register("alice", "password123", "alice@example.com", "13900000000");

        LoginResult failed = service.login("alice", "wrong", "invalid ip");
        assertFalse(failed.isSuccess());
        assertEquals(userId, systemLogDAO.lastUserId);
        assertEquals("WARN", systemLogDAO.lastLogLevel);
        assertEquals("127.0.0.1", systemLogDAO.lastActionDetail.getString("ip"));

        LoginResult success = service.login("alice", "password123", "10.0.0.8");
        assertTrue(success.isSuccess());
        assertNotNull(success.getUser());
        assertEquals("INFO", systemLogDAO.lastLogLevel);
        assertEquals("10.0.0.8", systemLogDAO.lastActionDetail.getString("ip"));
    }

    @Test
    void loginRejectsDisabledUsersWithoutAuditWrite() {
        service.register("alice", "password123", "alice@example.com", "13900000000");
        userDAO.usersByName.get("alice").setStatus(0);
        systemLogDAO.clear();

        LoginResult result = service.login("alice", "password123", "10.0.0.8");

        assertFalse(result.isSuccess());
        assertEquals("账号已被禁用", result.getMessage());
        assertEquals(0, systemLogDAO.writeCount);
    }

    @Test
    void updateProfileRequiresUserIdAndNormalizesFields() {
        userDAO.addActiveUser(8L);
        Profile profile = new Profile();
        profile.setUserId(8L);
        profile.setRealName("  Alice  ");
        profile.setAddress("  Hangzhou  ");

        assertTrue(service.updateProfile(8L, profile));
        assertEquals("Alice", profileDAO.profile.getRealName());
        assertEquals("Hangzhou", profileDAO.profile.getAddress());

        Profile invalid = new Profile();
        assertThrows(BusinessException.class, () -> service.updateProfile(8L, invalid));
    }

    @Test
    void getProfileRequiresValidUserIdAndReadsProfile() {
        userDAO.addActiveUser(8L);
        Profile profile = new Profile();
        profile.setUserId(8L);
        profile.setRealName("Alice");
        profileDAO.profile = profile;

        Optional<Profile> result = service.getProfile(8L, 8L);

        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getRealName());
        assertEquals(8L, profileDAO.lastFindUserId);
        assertThrows(BusinessException.class, () -> service.getProfile(8L, 0L));
    }

    @Test
    void isAdminRequiresAdminRoleAndActiveStatus() {
        User admin = new User();
        admin.setRole("ADMIN");
        admin.setStatus(1);

        User disabledAdmin = new User();
        disabledAdmin.setRole("ADMIN");
        disabledAdmin.setStatus(0);

        assertTrue(service.isAdmin(admin));
        assertFalse(service.isAdmin(disabledAdmin));
        assertFalse(service.isAdmin(null));
    }

    @Test
    void loginAndLogoutRemainSuccessfulWhenMongoAuditIsUnavailable() {
        service.register("alice", "password123", "alice@example.com", "13900000000");
        systemLogDAO.fail = true;

        LoginResult result = service.login("alice", "password123", "127.0.0.1");

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("审计日志写入失败"));
        assertFalse(service.logout(result.getUser(), "127.0.0.1"));
    }

    private static class InMemoryUserDAO extends UserDAO {
        private long nextId = 1L;
        private final Map<String, User> usersByName = new LinkedHashMap<>();

        @Override
        public long create(User user) {
            user.setUserId(nextId);
            nextId += 1;
            usersByName.put(user.getUsername(), user);
            return user.getUserId();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(usersByName.get(username));
        }

        private void addActiveUser(long userId) {
            User user = new User();
            user.setUserId(userId);
            user.setUsername("user" + userId);
            user.setRole("USER");
            user.setStatus(1);
            usersByName.put(user.getUsername(), user);
        }

        @Override
        public Optional<User> findById(long userId) {
            return usersByName.values().stream().filter(user -> user.getUserId() == userId).findFirst();
        }
    }

    private static class CapturingProfileDAO extends ProfileDAO {
        private Profile profile;
        private long lastFindUserId;

        @Override
        public boolean upsert(Profile profile) {
            this.profile = profile;
            return true;
        }

        @Override
        public Optional<Profile> findByUserId(long userId) {
            lastFindUserId = userId;
            return Optional.ofNullable(profile);
        }
    }

    private static class CapturingSystemLogDAO extends SystemLogDAO {
        private long lastUserId;
        private String lastLogType;
        private String lastLogLevel;
        private String lastMessage;
        private Document lastActionDetail;
        private int writeCount;
        private boolean fail;

        @Override
        public void record(long userId, String logType, String logLevel, String message, Document actionDetail) {
            if (fail) {
                throw new IllegalStateException("Mongo unavailable");
            }
            lastUserId = userId;
            lastLogType = logType;
            lastLogLevel = logLevel;
            lastMessage = message;
            lastActionDetail = actionDetail;
            writeCount += 1;
        }

        private void clear() {
            lastUserId = 0L;
            lastLogType = null;
            lastLogLevel = null;
            lastMessage = null;
            lastActionDetail = null;
            writeCount = 0;
        }
    }
}
