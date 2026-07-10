package com.scenicticket.ui;

import java.util.ArrayList;
import java.util.List;

public final class UiNavigationPolicy {
    private static final List<String> USER_PAGES = List.of("首页", "个人档案", "景点浏览", "我的订单", "统计报表");

    private UiNavigationPolicy() {
    }

    public static List<String> visiblePages(String role) {
        List<String> pages = new ArrayList<>(USER_PAGES);
        if ("ADMIN".equals(role)) {
            pages.add("后台管理");
            pages.add("系统审计");
        }
        return List.copyOf(pages);
    }
}
