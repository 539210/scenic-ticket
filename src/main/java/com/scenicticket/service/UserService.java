package com.scenicticket.service;

import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.ProfileDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.dto.LoginResult;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Profile;
import com.scenicticket.model.User;
import com.scenicticket.util.PasswordUtil;
import com.scenicticket.util.SecurityUtil;
import org.bson.Document;

import java.util.Optional;

public class UserService {
    private final UserDAO userDAO;
    private final ProfileDAO profileDAO;
    private final SystemLogDAO systemLogDAO;

    public UserService() {
        this(new UserDAO(), new ProfileDAO(), new SystemLogDAO());
    }

    public UserService(UserDAO userDAO, ProfileDAO profileDAO, SystemLogDAO systemLogDAO) {
        this.userDAO = userDAO;
        this.profileDAO = profileDAO;
        this.systemLogDAO = systemLogDAO;
    }

    public long register(String username, String password, String email, String phone) {
        validateRegisterInput(username, password, email, phone);
        String safeUsername = SecurityUtil.requireText(username, "用户名", 50);
        String safeEmail = SecurityUtil.normalizeEmail(email);
        String safePhone = SecurityUtil.normalizePhone(phone);
        if (userDAO.findByUsername(safeUsername).isPresent()) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(safeUsername);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setEmail(safeEmail);
        user.setPhone(safePhone);
        user.setRole("USER");
        user.setStatus(1);
        long userId = userDAO.create(user);
        systemLogDAO.record(userId, "REGISTER", "INFO", "User registered", new Document("username", safeUsername));
        return userId;
    }

    public LoginResult login(String username, String password, String ip) {
        String safeUsername = SecurityUtil.normalizeText(username, 50);
        if (safeUsername == null || safeUsername.isBlank() || password == null) {
            return new LoginResult(false, "用户名或密码错误", null);
        }
        String safeIp = SecurityUtil.normalizeIp(ip);
        Optional<User> userOptional = userDAO.findByUsername(safeUsername);
        if (userOptional.isEmpty()) {
            return new LoginResult(false, "用户名或密码错误", null);
        }
        User user = userOptional.get();
        if (user.getStatus() == null || user.getStatus() != 1) {
            return new LoginResult(false, "账号已被禁用", null);
        }
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            systemLogDAO.record(user.getUserId(), "LOGIN", "WARN", "Login failed", new Document("ip", safeIp));
            return new LoginResult(false, "用户名或密码错误", null);
        }
        systemLogDAO.record(user.getUserId(), "LOGIN", "INFO", "Login success", new Document("ip", safeIp));
        return new LoginResult(true, "登录成功", user);
    }

    public boolean updateProfile(Profile profile) {
        if (profile.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        profile.setRealName(SecurityUtil.normalizeText(profile.getRealName(), 50));
        profile.setIdCard(SecurityUtil.normalizeText(profile.getIdCard(), 20));
        profile.setAddress(SecurityUtil.normalizeText(profile.getAddress(), 500));
        profile.setNotes(SecurityUtil.normalizeText(profile.getNotes(), 1000));
        return profileDAO.upsert(profile);
    }

    public Optional<Profile> getProfile(long userId) {
        if (userId <= 0) {
            throw new BusinessException("用户ID必须大于 0");
        }
        return profileDAO.findByUserId(userId);
    }

    public boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole()) && user.getStatus() != null && user.getStatus() == 1;
    }

    private void validateRegisterInput(String username, String password, String email, String phone) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }
        if (password.length() > 72) {
            throw new BusinessException("密码长度不能超过 72 位");
        }
        SecurityUtil.normalizeEmail(email);
        SecurityUtil.normalizePhone(phone);
    }
}
