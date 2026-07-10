package com.scenicticket.service;

import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.User;

public class AuthorizationService {
    private final UserDAO userDAO;

    public AuthorizationService() {
        this(new UserDAO());
    }

    public AuthorizationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User requireActiveUser(long actorUserId) {
        if (actorUserId <= 0) {
            throw new BusinessException("请先登录");
        }
        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new BusinessException("当前登录用户不存在，请重新登录"));
        if (actor.getStatus() == null || actor.getStatus() != 1) {
            throw new BusinessException("当前账号已被禁用，请重新登录");
        }
        return actor;
    }

    public User requireAdmin(long actorUserId) {
        User actor = requireActiveUser(actorUserId);
        if (!"ADMIN".equals(actor.getRole())) {
            throw new BusinessException("当前账号没有管理员权限");
        }
        return actor;
    }

    public User requireSelfOrAdmin(long actorUserId, long targetUserId) {
        if (targetUserId <= 0) {
            throw new BusinessException("目标用户ID必须大于 0");
        }
        User actor = requireActiveUser(actorUserId);
        if (actorUserId != targetUserId && !"ADMIN".equals(actor.getRole())) {
            throw new BusinessException("不能访问其他用户的数据");
        }
        return actor;
    }
}
