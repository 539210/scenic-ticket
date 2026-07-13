package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterPanelTest {
    @Test
    void successfulRegistrationPassesAllFieldsAndClearsPassword() {
        AtomicReference<String> submitted = new AtomicReference<>();
        AtomicLong registeredUserId = new AtomicLong();
        RecordingTaskExecutor executor = new RecordingTaskExecutor();
        RegisterPanel panel = new RegisterPanel((username, password, email, phone) -> {
            submitted.set(String.join("|", username, password, email, phone));
            return 18L;
        }, registeredUserId::set, () -> { }, ignored -> { }, executor);
        panel.setFormValues("alice", "password123", "alice@example.com", "13900000000");

        click(panel, "注册");

        assertEquals("用户注册", executor.taskName);
        assertEquals("alice|password123|alice@example.com|13900000000", submitted.get());
        assertEquals(18L, registeredUserId.get());
        assertEquals(0, panel.passwordLength());
    }

    @Test
    void failedRegistrationShowsInlineReasonAndDoesNotNavigate() {
        AtomicReference<String> status = new AtomicReference<>();
        AtomicBoolean succeeded = new AtomicBoolean();
        RegisterPanel panel = new RegisterPanel((username, password, email, phone) -> {
            throw new IllegalArgumentException("用户名已存在");
        }, ignored -> succeeded.set(true), () -> { }, status::set, new RecordingTaskExecutor());
        panel.setFormValues("alice", "password123", "alice@example.com", "13900000000");

        click(panel, "注册");

        assertEquals("用户名已存在", panel.messageText());
        assertEquals("用户名已存在", status.get());
        assertEquals(0, panel.passwordLength());
        assertTrue(!succeeded.get());
    }

    @Test
    void backButtonReturnsToLoginWithoutSubmitting() {
        AtomicBoolean backRequested = new AtomicBoolean();
        AtomicBoolean submitted = new AtomicBoolean();
        RegisterPanel panel = new RegisterPanel((username, password, email, phone) -> {
            submitted.set(true);
            return 1L;
        }, ignored -> { }, () -> backRequested.set(true), ignored -> { }, new RecordingTaskExecutor());

        click(panel, "返回登录");

        assertTrue(backRequested.get());
        assertTrue(!submitted.get());
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
