package com.scenicticket.dao;

import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.model.User;
import com.scenicticket.util.PasswordUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOTest {
    private final UserDAO userDAO = new UserDAO();

    @Test
    void createAndFindByUsername() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true after initializing the local MySQL database.");

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername("test_user_" + suffix);
        user.setPasswordHash(PasswordUtil.hashPassword("password123"));
        user.setEmail("test_" + suffix + "@example.com");
        user.setPhone("1390000" + suffix.substring(0, 4));
        user.setRole("USER");
        user.setStatus(1);

        long userId = userDAO.create(user);
        Optional<User> savedUser = userDAO.findByUsername(user.getUsername());

        assertTrue(savedUser.isPresent());
        assertEquals(userId, savedUser.get().getUserId());
        assertEquals(user.getEmail(), savedUser.get().getEmail());
        assertTrue(PasswordUtil.verifyPassword("password123", savedUser.get().getPasswordHash()));
    }
}
