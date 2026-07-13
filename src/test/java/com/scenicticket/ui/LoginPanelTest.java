package com.scenicticket.ui;

import com.scenicticket.dto.LoginResult;
import com.scenicticket.model.User;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginPanelTest {
    @Test
    void successfulLoginClearsCredentialsAndHandsOffAuthenticatedResult() {
        AtomicReference<String> credentials = new AtomicReference<>();
        AtomicReference<LoginResult> authenticated = new AtomicReference<>();
        User user = new User();
        user.setUserId(7L);
        user.setUsername("alice");
        LoginResult success = new LoginResult(true, "登录成功", user);
        RecordingTaskExecutor executor = new RecordingTaskExecutor();
        LoginPanel panel = new LoginPanel((username, password) -> {
            credentials.set(username + ":" + password);
            return success;
        }, authenticated::set, () -> { }, ignored -> { }, executor);
        panel.setCredentials("alice", "password123");

        click(panel, "登录");

        assertEquals("用户登录", executor.taskName);
        assertEquals("alice:password123", credentials.get());
        assertEquals(success, authenticated.get());
        assertEquals("", panel.usernameText());
        assertEquals(0, panel.passwordLength());
        assertEquals("登录成功", panel.messageText());
    }

    @Test
    void rejectedLoginKeepsUsernameButClearsPasswordAndShowsReason() {
        AtomicReference<String> status = new AtomicReference<>();
        AtomicBoolean authenticated = new AtomicBoolean();
        LoginPanel panel = new LoginPanel((username, password) ->
                new LoginResult(false, "账号已被禁用", null),
                ignored -> authenticated.set(true), () -> { }, status::set, new RecordingTaskExecutor());
        panel.setCredentials("disabled-user", "secret123");

        click(panel, "登录");

        assertEquals("disabled-user", panel.usernameText());
        assertEquals(0, panel.passwordLength());
        assertEquals("账号已被禁用", panel.messageText());
        assertEquals("账号已被禁用", status.get());
        assertTrue(!authenticated.get());
    }

    @Test
    void loginFailureAndRegisterNavigationUsePanelCallbacks() {
        AtomicReference<String> status = new AtomicReference<>();
        AtomicBoolean registrationRequested = new AtomicBoolean();
        LoginPanel panel = new LoginPanel((username, password) -> {
            throw new IllegalStateException("数据库暂不可用");
        }, ignored -> { }, () -> registrationRequested.set(true), status::set, new RecordingTaskExecutor());
        panel.setCredentials("alice", "secret123");

        click(panel, "登录");
        click(panel, "注册新账号");

        assertEquals("数据库暂不可用", panel.messageText());
        assertEquals("数据库暂不可用", status.get());
        assertEquals(0, panel.passwordLength());
        assertTrue(registrationRequested.get());
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

    private static final class RecordingTaskExecutor implements UiTaskExecutor {
        private String taskName;

        @Override
        public <T> void run(String name, Callable<T> task, Consumer<T> onSuccess) {
            run(name, task, onSuccess, message -> { });
        }

        @Override
        public <T> void run(String name, Callable<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
            taskName = name;
            try {
                onSuccess.accept(task.call());
            } catch (Exception exception) {
                onError.accept(exception.getMessage());
            }
        }
    }
}
