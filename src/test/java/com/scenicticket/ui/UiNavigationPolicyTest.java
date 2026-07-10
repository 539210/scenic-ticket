package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiNavigationPolicyTest {
    @Test
    void normalUserCannotSeeAdministratorPages() {
        var pages = UiNavigationPolicy.visiblePages("USER");

        assertTrue(pages.contains("景点浏览"));
        assertTrue(pages.contains("我的订单"));
        assertFalse(pages.contains("后台管理"));
        assertFalse(pages.contains("系统审计"));
    }

    @Test
    void administratorCanSeeCompleteNavigation() {
        var pages = UiNavigationPolicy.visiblePages("ADMIN");

        assertTrue(pages.contains("后台管理"));
        assertTrue(pages.contains("系统审计"));
    }
}
