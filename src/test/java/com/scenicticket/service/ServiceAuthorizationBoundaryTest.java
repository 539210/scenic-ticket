package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.CategoryDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.ReportDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.User;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceAuthorizationBoundaryTest {
    private final BoundaryUserDAO userDAO = new BoundaryUserDAO();
    private final AuthorizationService authorization = new AuthorizationService(userDAO);

    @Test
    void ordinaryUserCannotCallBusinessAdministratorMutationOrCrossUserOrderQuery() {
        CapturingItemDAO itemDAO = new CapturingItemDAO();
        CapturingOrderDAO orderDAO = new CapturingOrderDAO();
        BusinessService service = new BusinessService(new CategoryDAO(), itemDAO, orderDAO, new DetailDAO(),
                new LogDAO(), new CommentDAO(), () -> { throw new AssertionError("connection not expected"); },
                authorization);

        assertThrows(BusinessException.class, () -> service.updateItemStatus(2L, 9L, 0));
        assertThrows(BusinessException.class, () -> service.searchOrderViews(2L, 1L, null, null, 20, 0));
        assertEquals(0, itemDAO.updateCount);
        assertEquals(0, orderDAO.searchCount);
    }

    @Test
    void ordinaryUserCannotReadSystemAuditOrAnotherUsersStatistics() {
        CapturingSystemLogDAO systemLogDAO = new CapturingSystemLogDAO();
        SystemLogService systemLogs = new SystemLogService(systemLogDAO, authorization);
        StatisticsService statistics = new StatisticsService(new LogDAO(), new CommentDAO(), new ReportDAO(),
                systemLogDAO, authorization);

        assertThrows(BusinessException.class,
                () -> systemLogs.queryAuditLogs(2L, null, null, null, null, null, 20));
        assertThrows(BusinessException.class, () -> statistics.getUserReport(2L, 1L, null, null));
        assertEquals(0, systemLogDAO.queryCount);
    }

    private static final class BoundaryUserDAO extends UserDAO {
        @Override
        public Optional<User> findById(long userId) {
            if (userId != 1L && userId != 2L) {
                return Optional.empty();
            }
            User user = new User();
            user.setUserId(userId);
            user.setRole(userId == 1L ? "ADMIN" : "USER");
            user.setStatus(1);
            return Optional.of(user);
        }
    }

    private static final class CapturingItemDAO extends ItemDAO {
        private int updateCount;

        @Override
        public boolean updateStatus(long itemId, int status) {
            updateCount += 1;
            return true;
        }
    }

    private static final class CapturingOrderDAO extends OrderDAO {
        private int searchCount;

        @Override
        public List<com.scenicticket.dto.OrderViewDTO> searchViews(
                Long userId, Long orderId, Integer status, int limit, int offset) {
            searchCount += 1;
            return List.of();
        }
    }

    private static final class CapturingSystemLogDAO extends SystemLogDAO {
        private int queryCount;

        @Override
        public List<Document> findByCondition(Long userId, String logType, String logLevel,
                                              Date startTime, Date endTime, int limit) {
            queryCount += 1;
            return List.of();
        }
    }
}
