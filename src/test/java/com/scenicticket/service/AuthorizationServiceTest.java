package com.scenicticket.service;

import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.User;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceTest {
    private final StubUserDAO userDAO = new StubUserDAO(Map.of(
            1L, user(1L, "ADMIN", 1),
            2L, user(2L, "USER", 1),
            3L, user(3L, "ADMIN", 0)
    ));
    private final AuthorizationService service = new AuthorizationService(userDAO);

    @Test
    void requireAdminReadsCurrentDatabaseRoleAndStatus() {
        assertEquals(1L, service.requireAdmin(1L).getUserId());
        assertThrows(BusinessException.class, () -> service.requireAdmin(2L));
        assertThrows(BusinessException.class, () -> service.requireAdmin(3L));
        assertThrows(BusinessException.class, () -> service.requireAdmin(99L));
    }

    @Test
    void selfOrAdminRejectsCrossUserAccessForOrdinaryUser() {
        service.requireSelfOrAdmin(2L, 2L);
        service.requireSelfOrAdmin(1L, 2L);
        assertThrows(BusinessException.class, () -> service.requireSelfOrAdmin(2L, 1L));
    }

    @Test
    void invalidActorIdIsRejectedBeforeDaoAccess() {
        assertThrows(BusinessException.class, () -> service.requireActiveUser(0L));
        assertEquals(0, userDAO.zeroIdReads);
    }

    private static User user(long id, String role, int status) {
        User user = new User();
        user.setUserId(id);
        user.setUsername("user" + id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private static final class StubUserDAO extends UserDAO {
        private final Map<Long, User> users;
        private int zeroIdReads;

        private StubUserDAO(Map<Long, User> users) {
            this.users = users;
        }

        @Override
        public Optional<User> findById(long userId) {
            if (userId <= 0) {
                zeroIdReads += 1;
            }
            return Optional.ofNullable(users.get(userId));
        }
    }
}
