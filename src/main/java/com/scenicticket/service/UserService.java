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
        validateRegisterInput(username, password, email);
        if (userDAO.findByUsername(username).isPresent()) {
            throw new BusinessException("Username already exists.");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setEmail(email.trim());
        user.setPhone(phone);
        user.setRole("USER");
        user.setStatus(1);
        long userId = userDAO.create(user);
        systemLogDAO.record(userId, "REGISTER", "INFO", "User registered", new Document("username", username));
        return userId;
    }

    public LoginResult login(String username, String password, String ip) {
        Optional<User> userOptional = userDAO.findByUsername(username);
        if (userOptional.isEmpty()) {
            return new LoginResult(false, "用户名或密码错误", null);
        }
        User user = userOptional.get();
        if (user.getStatus() == null || user.getStatus() != 1) {
            return new LoginResult(false, "账号已被禁用", null);
        }
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            systemLogDAO.record(user.getUserId(), "LOGIN", "WARN", "Login failed", new Document("ip", ip));
            return new LoginResult(false, "用户名或密码错误", null);
        }
        systemLogDAO.record(user.getUserId(), "LOGIN", "INFO", "Login success", new Document("ip", ip));
        return new LoginResult(true, "登录成功", user);
    }

    public boolean updateProfile(Profile profile) {
        if (profile.getUserId() == null) {
            throw new BusinessException("Profile user id is required.");
        }
        return profileDAO.upsert(profile);
    }

    public boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole()) && user.getStatus() != null && user.getStatus() == 1;
    }

    private void validateRegisterInput(String username, String password, String email) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Username is required.");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("Password length must be at least 6.");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BusinessException("Valid email is required.");
        }
    }
}
