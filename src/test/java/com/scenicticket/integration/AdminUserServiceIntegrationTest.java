package com.scenicticket.integration;

import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.dto.AdminChangeResult;
import com.scenicticket.dto.AdminUserDetailDTO;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.model.User;
import com.scenicticket.service.AdminUserService;
import com.scenicticket.util.MongoDBUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUserServiceIntegrationTest {
    private static AdminUserService service;

    @BeforeAll
    static void verifyTargets() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true with explicit scenic_ticket_test overrides.");
        DatabaseTargetGuard.requireIsolatedTestTargets(
                DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));
        service = new AdminUserService();
    }

    @AfterAll
    static void closeMongoClient() {
        MongoDBUtil.closeClient();
    }

    @Test
    void administratorCanSearchAndReadUserProfileOrderAndBehaviorOverview() {
        List<User> users = service.searchUsers(1L,
                new UserSearchCriteria("user001", "example.com", "USER", 1, 50, 0));

        assertFalse(users.isEmpty());
        assertEquals(2L, users.get(0).getUserId());

        AdminUserDetailDTO detail = service.getUserDetail(1L, 2L);
        assertEquals(2L, detail.getUser().getUserId());
        assertTrue(detail.getOrderSummary().getTotalOrders() >= 1);
        assertTrue(detail.isBehaviorDataAvailable());
    }

    @Test
    void statusAndRoleChangesCommitInMysqlAndRecordAuditOutcome() {
        AdminChangeResult disabled = service.changeUserStatus(1L, 2L, 0);
        AdminChangeResult enabled = service.changeUserStatus(1L, 2L, 1);
        AdminChangeResult promoted = service.changeUserRole(1L, 2L, "ADMIN");
        AdminChangeResult restored = service.changeUserRole(1L, 2L, "USER");

        assertTrue(disabled.updated());
        assertTrue(enabled.updated());
        assertTrue(promoted.updated());
        assertTrue(restored.updated());
        assertTrue(disabled.auditRecorded());
        assertTrue(restored.auditRecorded());
    }
}
