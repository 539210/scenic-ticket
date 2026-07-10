package com.scenicticket.ui;

import com.scenicticket.model.Category;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UiCategoryOptionsTest {
    @Test
    void buildsCategoryOptionsFromDatabaseValues() {
        Category category = new Category();
        category.setCategoryId(42L);
        category.setName("生态体验");
        JComboBox<CategoryOption> comboBox = new JComboBox<>();

        UiCategoryOptions.fill(comboBox, List.of(category), true, "全部类型");

        assertEquals(2, comboBox.getItemCount());
        assertNull(comboBox.getItemAt(0).categoryId());
        assertEquals("生态体验", comboBox.getItemAt(1).label());
        assertEquals(42L, comboBox.getItemAt(1).categoryId());
    }
}
