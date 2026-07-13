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
        panel.setFilters(" alice ", "a@example.com", 2, 1);

        click(panel, "查询用户");

        UserSearchCriteria criteria = actions.lastCriteria;
        assertEquals("alice", criteria.username());
        assertEquals("a@example.com", criteria.email());
        assertEquals("USER", criteria.role());
        assertEquals(1, criteria.status());
        assertEquals(100, criteria.limit());
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
        panel.setTargetStatusIndex(0);
        click(panel, "更新状态");

        assertEquals(2L, actions.statusTargetId);
        assertEquals(0, actions.newStatus);
        assertEquals("用户状态已更新", last(actions.statuses));

        panel.selectRow(1);
        panel.setTargetRoleIndex(1);
        click(panel, "更新角色");

        assertEquals(2L, actions.roleTargetId);
        assertEquals("ADMIN", actions.newRole);
        assertEquals("用户角色已更新", last(actions.statuses));
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
        public AdminChangeResult changeStatus(long targetUserId, int status) {
            statusTargetId = targetUserId;
            newStatus = status;
            return new AdminChangeResult(true, true, "用户状态已更新");
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
