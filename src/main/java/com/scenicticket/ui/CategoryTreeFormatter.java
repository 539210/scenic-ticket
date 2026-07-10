package com.scenicticket.ui;

import com.scenicticket.model.Category;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CategoryTreeFormatter {
    private CategoryTreeFormatter() {
    }

    public static Map<Long, String> paths(List<Category> categories) {
        Map<Long, Category> byId = new HashMap<>();
        for (Category category : categories) {
            byId.put(category.getCategoryId(), category);
        }
        Map<Long, String> result = new LinkedHashMap<>();
        for (Category category : categories) {
            result.put(category.getCategoryId(), path(category, byId, new HashSet<>()));
        }
        return result;
    }

    private static String path(Category category, Map<Long, Category> byId, Set<Long> visiting) {
        if (!visiting.add(category.getCategoryId())) {
            return "[循环]";
        }
        Long parentId = category.getParentId();
        if (parentId == null) {
            return category.getName();
        }
        Category parent = byId.get(parentId);
        if (parent == null) {
            return "[缺失上级] / " + category.getName();
        }
        return path(parent, byId, visiting) + " / " + category.getName();
    }
}
