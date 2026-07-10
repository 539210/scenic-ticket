package com.scenicticket.ui;

import com.scenicticket.model.Category;

import javax.swing.JComboBox;
import java.util.List;

public final class UiCategoryOptions {
    private UiCategoryOptions() {
    }

    public static void fill(JComboBox<CategoryOption> comboBox, List<Category> categories,
                            boolean includeEmpty, String emptyLabel) {
        comboBox.removeAllItems();
        if (includeEmpty) {
            comboBox.addItem(new CategoryOption(emptyLabel, null));
        }
        if (categories == null) {
            return;
        }
        for (Category category : categories) {
            comboBox.addItem(new CategoryOption(category.getName(), category.getCategoryId()));
        }
    }
}
