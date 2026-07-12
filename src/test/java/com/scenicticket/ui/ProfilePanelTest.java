package com.scenicticket.ui;

import com.scenicticket.model.Profile;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProfilePanelTest {
    @Test
    void refreshLoadsCurrentUsersProfile() {
        Profile stored = profile(8L, "张三", "证件", "苏州", "简介");
        ProfilePanel panel = new ProfilePanel(8L, () -> Optional.of(stored), ignored -> true,
                new ImmediateTaskExecutor());

        click(panel, "刷新档案");

        assertEquals("张三", panel.realNameText());
        assertEquals("档案已刷新", panel.messageText());
    }

    @Test
    void saveAlwaysBindsProfileToCurrentUser() {
        AtomicReference<Profile> saved = new AtomicReference<>();
        ProfilePanel panel = new ProfilePanel(8L, Optional::empty, profile -> {
            saved.set(profile);
            return true;
        }, new ImmediateTaskExecutor());
        panel.setFormValues("李四", "ID-8", "无锡", "新的简介");

        click(panel, "保存档案");

        assertEquals(8L, saved.get().getUserId());
        assertEquals("李四", saved.get().getRealName());
        assertEquals("档案已保存", panel.messageText());
    }

    private static Profile profile(long userId, String realName, String idCard, String address, String notes) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setRealName(realName);
        profile.setIdCard(idCard);
        profile.setAddress(address);
        profile.setNotes(notes);
        return profile;
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
}
