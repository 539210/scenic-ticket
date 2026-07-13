package com.scenicticket.ui;

import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagementPanelTest {
    @Test
    void initialLoadBuildsCategoryTreeAndItemSelectionLoadsStructuredDetail() {
        FakeActions actions = new FakeActions();
        ManagementPanel panel = panel(actions);

        assertEquals(2, panel.categoryRowCount());
        assertEquals(1, panel.itemRowCount());
        assertFalse(panel.itemActionsEnabled());
        panel.selectItemRow(0);

        assertTrue(panel.itemActionsEnabled());
        assertEquals("中文景点简介", panel.itemIntroText());
        assertEquals(11L, actions.detailItemId);
        assertEquals(1, actions.categoriesLoadedCalls);
    }

    @Test
    void emptyItemQueryHasExplicitStatus() {
        FakeActions actions = new FakeActions();
        actions.items = List.of();
        ManagementPanel panel = panel(actions);

        click(panel, "查询");

        assertEquals(0, panel.itemRowCount());
        assertEquals("没有找到符合条件的景点", last(actions.statuses));
    }

    @Test
    void itemUpdatesFreezeSelectedIdParseValuesAndPreserveMessages() {
        FakeActions actions = new FakeActions();
        ManagementPanel panel = panel(actions);
        panel.selectItemRow(0);
        panel.setItemBasicForm("新景点名称", 1);

        click(panel, "更新名称和分类");
        assertEquals(11L, actions.updatedItemId);
        assertEquals("新景点名称", actions.updatedTitle);
        assertEquals(2L, actions.updatedCategoryId);
        assertEquals("景点名称和分类已更新", last(actions.statuses));

        panel.selectItemRow(0);
        panel.setItemPricingForm("88.50", "12.5");
        click(panel, "更新票价和优惠");

        assertEquals(new BigDecimal("88.50"), actions.updatedPrice);
        assertEquals(new BigDecimal("12.5"), actions.updatedDiscount);
        assertEquals("景点票价和优惠已保存", last(actions.statuses));
    }

    @Test
    void categoryCreateAndUpdateUseParentSelectionAndKeepResultMessage() {
        FakeActions actions = new FakeActions();
        ManagementPanel panel = panel(actions);
        panel.setCategoryForm("亲子主题", 1);

        click(panel, "新增分类");
        assertEquals("亲子主题", actions.createdCategoryName);
        assertEquals(1L, actions.createdParentId);
        assertEquals("分类创建成功，编号：8", last(actions.statuses));

        panel.selectCategoryRow(1);
        assertTrue(panel.categoryUpdateEnabled());
        panel.setCategoryForm("自然景区", 1);
        click(panel, "更新所选分类");

        assertEquals(2L, actions.updatedCategoryIdValue);
        assertEquals("自然景区", actions.updatedCategoryName);
        assertEquals(1L, actions.updatedParentId);
        assertEquals("分类名称和层级已更新", last(actions.statuses));
    }

    private static ManagementPanel panel(FakeActions actions) {
        return new ManagementPanel(new ImmediateTaskExecutor(), actions,
                new JPanel(), new JPanel(), new JPanel());
    }

    private static String last(List<String> values) {
        return values.get(values.size() - 1);
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

    private static final class FakeActions implements ManagementPanel.Actions {
        private List<Category> categories = List.of(
                category(1L, "旅游资源", null), category(2L, "自然风光", 1L));
        private List<Item> items = List.of(item());
        private final List<String> statuses = new ArrayList<>();
        private int categoriesLoadedCalls;
        private long detailItemId;
        private long updatedItemId;
        private String updatedTitle;
        private long updatedCategoryId;
        private BigDecimal updatedPrice;
        private BigDecimal updatedDiscount;
        private String createdCategoryName;
        private Long createdParentId;
        private long updatedCategoryIdValue;
        private String updatedCategoryName;
        private Long updatedParentId;

        @Override
        public List<Category> listCategories() {
            return categories;
        }

        @Override
        public void categoriesLoaded(List<Category> categories) {
            categoriesLoadedCalls++;
        }

        @Override
        public List<Item> searchItems(String keyword, Long categoryId) {
            return items;
        }

        @Override
        public Document itemDetail(long itemId) {
            detailItemId = itemId;
            return new Document("description", "中文景点简介")
                    .append("images", List.of("https://example.com/a.jpg"))
                    .append("metadata", new Document("source", "test"));
        }

        @Override
        public void showCreateItemDialog(Runnable refreshItems) {
            refreshItems.run();
        }

        @Override
        public boolean updateItem(long itemId, String title, long categoryId) {
            updatedItemId = itemId;
            updatedTitle = title;
            updatedCategoryId = categoryId;
            return true;
        }

        @Override
        public boolean updateItemPricing(long itemId, BigDecimal price, BigDecimal discount) {
            updatedItemId = itemId;
            updatedPrice = price;
            updatedDiscount = discount;
            return true;
        }

        @Override
        public boolean updateItemStatus(long itemId, int status) {
            return true;
        }

        @Override
        public boolean updateItemDetail(long itemId, String description, String images, String metadata) {
            return true;
        }

        @Override
        public long createCategory(String name, Long parentId) {
            createdCategoryName = name;
            createdParentId = parentId;
            return 8L;
        }

        @Override
        public boolean updateCategory(long categoryId, String name, Long parentId) {
            updatedCategoryIdValue = categoryId;
            updatedCategoryName = name;
            updatedParentId = parentId;
            return true;
        }

        @Override
        public void setStatus(String message) {
            statuses.add(message);
        }

        private static Category category(long id, String name, Long parentId) {
            Category category = new Category();
            category.setCategoryId(id);
            category.setName(name);
            category.setParentId(parentId);
            return category;
        }

        private static Item item() {
            Item item = new Item();
            item.setItemId(11L);
            item.setTitle("南山景区");
            item.setCategoryId(2L);
            item.setPrice(new BigDecimal("100"));
            item.setDiscountRate(new BigDecimal("20"));
            item.setStatus(1);
            return item;
        }
    }
}
