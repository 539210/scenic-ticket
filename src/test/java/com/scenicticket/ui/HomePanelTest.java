package com.scenicticket.ui;

import com.scenicticket.model.User;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomePanelTest {
    @Test
    void userHomeShowsIdentityAndNavigatesToUserPages() {
        List<String> pages = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        HomePanel panel = new HomePanel(user(2L, "alice", "USER"), pages::add, statuses::add);

        assertEquals("alice / ID 2", panel.accountText());
        assertEquals(UiFormatters.role("USER"), panel.roleText());
        assertTrue(panel.summaryText().contains("alice"));
        assertNull(findButton(panel, "系统审计"));

        JButton orders = findButton(panel, "我的订单");
        assertNotNull(orders);
        orders.doClick();
        assertEquals(List.of("我的订单"), pages);

        JButton refresh = findButton(panel, "刷新");
        assertNotNull(refresh);
        refresh.doClick();
        assertEquals(List.of("首页已刷新"), statuses);
    }

    @Test
    void adminHomeExposesAdministrationPages() {
        List<String> pages = new ArrayList<>();
        HomePanel panel = new HomePanel(user(1L, "admin", "ADMIN"), pages::add, ignored -> { });

        JButton audit = findButton(panel, "系统审计");
        assertNotNull(audit);
        audit.doClick();

        assertEquals(List.of("系统审计"), pages);
        assertTrue(panel.summaryText().contains("管理员账户"));
    }

    private static User user(long id, String username, String role) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
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
}
