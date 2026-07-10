package com.scenicticket.ui;

import com.scenicticket.model.Category;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryTreeFormatterTest {
    @Test
    void rendersPathsAndMarksDamagedRelationships() {
        Map<Long, String> paths = CategoryTreeFormatter.paths(List.of(
                category(1L, "自然风光", null),
                category(2L, "森林", 1L),
                category(3L, "古树", 2L),
                category(4L, "孤立", 99L),
                category(5L, "循环甲", 6L),
                category(6L, "循环乙", 5L)
        ));

        assertEquals("自然风光 / 森林 / 古树", paths.get(3L));
        assertEquals("[缺失上级] / 孤立", paths.get(4L));
        assertTrue(paths.get(5L).contains("[循环]"));
    }

    private static Category category(long id, String name, Long parentId) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        category.setParentId(parentId);
        return category;
    }
}
