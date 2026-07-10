package com.scenicticket.service;

import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.ProfileDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.dto.AdminChangeResult;
import com.scenicticket.dto.AdminUserDetailDTO;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.User;
import com.scenicticket.util.ConnectionProvider;
import com.scenicticket.util.MySQLDBUtil;
import com.scenicticket.util.SecurityUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AdminUserService {
    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserDAO userDAO;
    private final ProfileDAO profileDAO;
    private final OrderDAO orderDAO;
    private final LogDAO logDAO;
    private final SystemLogDAO systemLogDAO;
    private final AuthorizationService authorizationService;
    private final ConnectionProvider connectionProvider;

    public AdminUserService() {
        this(new UserDAO(), new ProfileDAO(), new OrderDAO(), new LogDAO(), new SystemLogDAO(),
                new AuthorizationService(), MySQLDBUtil::getConnection);
    }

    public AdminUserService(UserDAO userDAO, ProfileDAO profileDAO, OrderDAO orderDAO, LogDAO logDAO,
                            SystemLogDAO systemLogDAO, AuthorizationService authorizationService,
                            ConnectionProvider connectionProvider) {
        this.userDAO = userDAO;
        this.profileDAO = profileDAO;
        this.orderDAO = orderDAO;
        this.logDAO = logDAO;
        this.systemLogDAO = systemLogDAO;
        this.authorizationService = authorizationService;
        this.connectionProvider = connectionProvider;
    }

    public List<User> searchUsers(long actorUserId, UserSearchCriteria criteria) {
        authorizationService.requireAdmin(actorUserId);
        UserSearchCriteria safeCriteria = criteria == null
                ? new UserSearchCriteria(null, null, null, null, 50, 0)
                : criteria;
        String role = normalizeRoleFilter(safeCriteria.role());
        Integer status = normalizeStatusFilter(safeCriteria.status());
        return userDAO.search(
                SecurityUtil.normalizeText(safeCriteria.username(), 50),
                SecurityUtil.normalizeText(safeCriteria.email(), 100),
                role,
                status,
                SecurityUtil.normalizeLimit(safeCriteria.limit(), 50, 200),
                SecurityUtil.normalizeOffset(safeCriteria.offset())
        );
    }

    public AdminUserDetailDTO getUserDetail(long actorUserId, long targetUserId) {
        authorizationService.requireAdmin(actorUserId);
        if (targetUserId <= 0) {
            throw new BusinessException("目标用户ID必须大于 0");
        }
        User target = userDAO.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("目标用户不存在"));
        AdminUserDetailDTO detail = new AdminUserDetailDTO();
        detail.setUser(target);
        detail.setProfile(profileDAO.findByUserId(targetUserId).orElse(null));
        detail.setOrderSummary(orderDAO.summarizeByUserId(targetUserId));
        try {
            detail.setBehaviorCount(logDAO.countByUserId(targetUserId));
        } catch (RuntimeException e) {
            detail.setBehaviorDataAvailable(false);
            log.warn("Failed to read MongoDB behavior summary for user {}", targetUserId, e);
        }
        return detail;
    }

    public AdminChangeResult changeUserStatus(long actorUserId, long targetUserId, int newStatus) {
        authorizationService.requireAdmin(actorUserId);
        if (targetUserId <= 0) {
            throw new BusinessException("目标用户ID必须大于 0");
        }
        if (newStatus != 0 && newStatus != 1) {
            throw new BusinessException("用户状态只能是启用或禁用");
        }

        try (Connection connection = connectionProvider.getConnection()) {
            try {
                connection.setAutoCommit(false);
                User actor = requireLockedAdmin(connection, actorUserId);
                User target = actorUserId == targetUserId ? actor : userDAO.findByIdForUpdate(connection, targetUserId)
                        .orElseThrow(() -> new BusinessException("目标用户不存在"));
                if (actorUserId == targetUserId && newStatus == 0) {
                    throw new BusinessException("管理员不能禁用当前登录的自己");
                }
                if (target.getStatus() != null && target.getStatus() == newStatus) {
                    connection.rollback();
                    return new AdminChangeResult(false, true, "用户状态未变化");
                }
                if ("ADMIN".equals(target.getRole()) && target.getStatus() != null
                        && target.getStatus() == 1 && newStatus == 0) {
                    requireAnotherActiveAdmin(connection);
                }
                boolean updated = userDAO.updateStatus(connection, targetUserId, newStatus);
                connection.commit();
                return auditChange(actorUserId, "USER_STATUS_UPDATE",
                        "用户状态已更新", new Document("target_user_id", targetUserId)
                                .append("new_status", newStatus), updated);
            } catch (SQLException e) {
                rollback(connection, e);
                throw new DBException("更新用户状态失败", e);
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            } finally {
                restoreAutoCommit(connection);
            }
        } catch (SQLException e) {
            throw new DBException("更新用户状态事务失败", e);
        }
    }

    public AdminChangeResult changeUserRole(long actorUserId, long targetUserId, String newRole) {
        authorizationService.requireAdmin(actorUserId);
        if (targetUserId <= 0) {
            throw new BusinessException("目标用户ID必须大于 0");
        }
        String safeRole = normalizeRequiredRole(newRole);

        try (Connection connection = connectionProvider.getConnection()) {
            try {
                connection.setAutoCommit(false);
                User actor = requireLockedAdmin(connection, actorUserId);
                User target = actorUserId == targetUserId ? actor : userDAO.findByIdForUpdate(connection, targetUserId)
                        .orElseThrow(() -> new BusinessException("目标用户不存在"));
                if (safeRole.equals(target.getRole())) {
                    connection.rollback();
                    return new AdminChangeResult(false, true, "用户角色未变化");
                }
                if (actorUserId == targetUserId) {
                    throw new BusinessException("管理员不能修改当前登录自己的角色");
                }
                if ("ADMIN".equals(target.getRole()) && "USER".equals(safeRole)
                        && target.getStatus() != null && target.getStatus() == 1) {
                    requireAnotherActiveAdmin(connection);
                }
                boolean updated = userDAO.updateRole(connection, targetUserId, safeRole);
                connection.commit();
                return auditChange(actorUserId, "USER_ROLE_UPDATE",
                        "用户角色已更新", new Document("target_user_id", targetUserId)
                                .append("new_role", safeRole), updated);
            } catch (SQLException e) {
                rollback(connection, e);
                throw new DBException("更新用户角色失败", e);
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            } finally {
                restoreAutoCommit(connection);
            }
        } catch (SQLException e) {
            throw new DBException("更新用户角色事务失败", e);
        }
    }

    private User requireLockedAdmin(Connection connection, long actorUserId) throws SQLException {
        User actor = userDAO.findByIdForUpdate(connection, actorUserId)
                .orElseThrow(() -> new BusinessException("当前登录用户不存在，请重新登录"));
        if (actor.getStatus() == null || actor.getStatus() != 1 || !"ADMIN".equals(actor.getRole())) {
            throw new BusinessException("当前账号没有有效管理员权限");
        }
        return actor;
    }

    private void requireAnotherActiveAdmin(Connection connection) throws SQLException {
        if (userDAO.lockActiveAdminIds(connection).size() <= 1) {
            throw new BusinessException("不能禁用或降级系统中最后一个有效管理员");
        }
    }

    private AdminChangeResult auditChange(long actorUserId, String type, String message,
                                          Document detail, boolean updated) {
        if (!updated) {
            return new AdminChangeResult(false, true, "用户信息未更新");
        }
        try {
            systemLogDAO.record(actorUserId, type, "INFO", message, detail);
            return new AdminChangeResult(true, true, message);
        } catch (RuntimeException e) {
            log.warn("MySQL user administration committed but MongoDB audit failed: type={}, actor={}",
                    type, actorUserId, e);
            return new AdminChangeResult(true, false, message + "，但审计日志写入失败，请稍后补偿");
        }
    }

    private String normalizeRoleFilter(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return normalizeRequiredRole(role);
    }

    private String normalizeRequiredRole(String role) {
        String safeRole = SecurityUtil.requireText(role, "用户角色", 20).toUpperCase(java.util.Locale.ROOT);
        if (!List.of("ADMIN", "USER").contains(safeRole)) {
            throw new BusinessException("用户角色只能是 ADMIN 或 USER");
        }
        return safeRole;
    }

    private Integer normalizeStatusFilter(Integer status) {
        if (status == null) {
            return null;
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("用户状态只能是启用或禁用");
        }
        return status;
    }

    private void rollback(Connection connection, Throwable original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    private void restoreAutoCommit(Connection connection) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            log.warn("Failed to restore auto-commit after user administration transaction", e);
        }
    }
}
