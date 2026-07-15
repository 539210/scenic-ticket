package com.scenicticket.ui;

import com.scenicticket.dto.AdminChangeResult;
import com.scenicticket.dto.AdminUserDetailDTO;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.model.User;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserManagementPanelTest {
    @Test
    void queryBuildsCompleteCriteriaAndRendersUsers() {
        FakeActions actions = new FakeActions(List.of(user(2L, "alice", "USER", 1)));
        UserManagementPanel panel = new UserManagementPanel(1L, new ImmediateTaskExecutor(), actions);
        panel.setFilters("2", " alice ", "a@example.com", 2, 1);

        click(panel, "查询用户");

        UserSearchCriteria criteria = actions.lastCriteria;
        assertEquals(2L, criteria.userId());
        assertEquals("alice", criteria.username());
        assertEquals("a@example.com", criteria.email());
        assertEquals("USER", criteria.role());
        assertEquals(1, criteria.status());
        assertEquals(50, criteria.limit());
        assertEquals(1, panel.rowCount());
    }

    @Test
    void currentAdministratorCannotMutateSelfAndMongoFailureStillShowsMysqlDetail() {
        FakeActions actions = new FakeActions(List.of(user(1L, "admin", "ADMIN", 1)));
        actions.behaviorAvailable = false;
        UserManagementPanel panel = new UserManagementPanel(1L, new ImmediateTaskExecutor(), actions);

        click(panel, "查询用户");
        panel.selectRow(0);

        assertFalse(panel.statusUpdateEnabled());
        assertFalse(panel.roleUpdateEnabled());
        assertTrue(panel.detailText().contains("MongoDB 行为数据暂不可用"));
        assertTrue(panel.detailText().contains("用户名：admin"));
    }

    @Test
    void otherUserChangesFreezeTargetAndPreserveServiceMessagesAfterRefresh() {
        User self = user(1L, "admin", "ADMIN", 1);
        User target = user(2L, "alice", "USER", 1);
        FakeActions actions = new FakeActions(List.of(self, target));
        UserManagementPanel panel = new UserManagementPanel(1L, new ImmediateTaskExecutor(), actions);

        click(panel, "查询用户");
        panel.selectRow(1);
        assertTrue(panel.statusUpdateEnabled());
        assertTrue(panel.banEnabled());
        panel.setBanReason("恶意刷单");
        click(panel, "封禁账号");

        assertEquals(2L, actions.statusTargetId);
        assertEquals(0, actions.newStatus);
        assertEquals("恶意刷单", actions.statusReason);
        assertEquals("用户账号已封禁", last(actions.statuses));

        panel.selectRow(1);
        panel.setTargetRoleIndex(1);
        click(panel, "更新角色");

        assertEquals(2L, actions.roleTargetId);
        assertEquals("ADMIN", actions.newRole);
        assertEquals("用户角色已更新", last(actions.statuses));
    }

    @Test
    void disabledUserCanBeUnblockedDirectly() {
        FakeActions actions = new FakeActions(List.of(user(2L, "blocked", "USER", 0)));
        UserManagementPanel panel = new UserManagementPanel(1L, new ImmediateTaskExecutor(), actions);

        click(panel, "查询用户");
        panel.selectRow(0);

        assertFalse(panel.banEnabled());
        assertTrue(panel.unbanEnabled());
        click(panel, "解除封禁");
        assertEquals(1, actions.newStatus);
    }

    @Test
    void fullPageEnablesNextPageAndPassesOffset() {
        List<User> users = java.util.stream.LongStream.rangeClosed(2, 51)
                .mapToObj(id -> user(id, "user" + id, "USER", 1)).toList();
        FakeActions actions = new FakeActions(users);
        UserManagementPanel panel = new UserManagementPanel(1L, new ImmediateTaskExecutor(), actions);

        click(panel, "查询用户");
        click(panel, "下一页");

        assertEquals(50, actions.lastCriteria.offset());
        assertEquals(50, actions.lastCriteria.limit());
    }

    private static String last(List<String> values) {
        return values.get(values.size() - 1);
    }

    private static User user(long id, String username, String role, int status) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPhone("13800000000");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private static void click(Container root, String text) {
        JButton button = findButton(root, text);
        assertNotNull(button);
        button.doClick();
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static final class ImmediateTaskExecutor implements UiTaskExecutor {
        @Override
        public <T> void run(String name, Callable<T> task, Consumer<T> onSuccess) {
            try {
                onSuccess.accept(task.call());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static final class FakeActions implements UserManagementPanel.Actions {
        private final List<User> users;
        private final List<String> statuses = new ArrayList<>();
        private UserSearchCriteria lastCriteria;
        private boolean behaviorAvailable = true;
        private long statusTargetId;
        private int newStatus;
        private String statusReason;
        private long roleTargetId;
        private String newRole;

        private FakeActions(List<User> users) {
            this.users = users;
        }

        @Override
        public List<User> search(UserSearchCriteria criteria) {
            lastCriteria = criteria;
            return users;
        }

        @Override
        public AdminUserDetailDTO detail(long targetUserId) {
            AdminUserDetailDTO detail = new AdminUserDetailDTO();
            detail.setUser(users.stream().filter(user -> user.getUserId() == targetUserId).findFirst().orElseThrow());
            detail.setBehaviorDataAvailable(behaviorAvailable);
            detail.setBehaviorCount(12);
            return detail;
        }

        @Override
        public AdminChangeResult changeStatus(long targetUserId, int status, String reason) {
            statusTargetId = targetUserId;
            newStatus = status;
            statusReason = reason;
            return new AdminChangeResult(true, true, status == 0 ? "用户账号已封禁" : "用户账号已解除封禁");
        }

        @Override
        public AdminChangeResult changeRole(long targetUserId, String role) {
            roleTargetId = targetUserId;
            newRole = role;
            return new AdminChangeResult(true, true, "用户角色已更新");
        }

        @Override
        public void setStatus(String message) {
            statuses.add(message);
        }
    }
}
