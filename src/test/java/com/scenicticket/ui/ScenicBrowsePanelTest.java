package com.scenicticket.ui;

import com.scenicticket.dto.CommentListDTO;
import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenicBrowsePanelTest {
    @Test
    void searchRendersCategoryPriceAndAvailabilityState() {
        Item available = item(11L, "南山风景区", 3L, "120", "20", 1);
        Item unavailable = item(12L, "云岭公园", 3L, "50", "0", 0);
        FakeActions actions = new FakeActions(List.of(available, unavailable));
        ScenicBrowsePanel panel = new ScenicBrowsePanel(new ImmediateTaskExecutor(), actions);

        click(panel, "查询");

        assertEquals(2, panel.itemRowCount());
        assertEquals("自然风光", panel.tableValueAt(0, 1));
        assertEquals("¥96.00", panel.tableValueAt(0, 4));
        assertEquals("4.6 / 5", panel.tableValueAt(0, 5));
        panel.selectRow(0);
        assertTrue(panel.purchaseEnabled());
        assertTrue(panel.overviewText().contains("南山风景区"));
        panel.selectRow(1);
        assertFalse(panel.purchaseEnabled());
        assertFalse(panel.overviewText().contains("状态："));
    }

    @Test
    void recommendationShowsClampedScoreAndReason() {
        Item item = item(21L, "星湖", 3L, "80", "10", 1);
        RecommendationDTO recommendation = new RecommendationDTO();
        recommendation.setItem(item);
        recommendation.setScore(4.8);
        recommendation.setReason("近期高分且适合亲子游");
        ScenicBrowsePanel panel = new ScenicBrowsePanel(
                new ImmediateTaskExecutor(), new FakeActions(List.of()));

        panel.fillRecommendations(List.of(recommendation));
        panel.selectRow(0);

        assertEquals("4.8 / 5", panel.tableValueAt(0, 5));
        assertTrue(panel.overviewText().contains("推荐理由：近期高分且适合亲子游"));
    }

    @Test
    void introductionDisplaysLoadedImageInsteadOfItsAddress() {
        Item item = item(11L, "南山风景区", 3L, "120", "20", 1);
        CrossDatabaseItemDTO detail = detail(item, List.of("https://img.example/scenic.jpg"));
        FakeActions actions = new FakeActions(List.of(item), detail);
        AtomicReference<List<?>> requestedSources = new AtomicReference<>();
        ScenicBrowsePanel panel = new ScenicBrowsePanel(new ImmediateTaskExecutor(), actions, sources -> {
            requestedSources.set(sources);
            return Optional.of(new ImageIcon(new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB)));
        });

        click(panel, "查询");
        panel.selectRow(0);
        click(panel, "景点简介");

        assertEquals(List.of("https://img.example/scenic.jpg"), requestedSources.get());
        assertTrue(panel.introductionImageVisible());
    }

    @Test
    void missingOrFailedImagesKeepTheWholeImageAreaHidden() {
        Item item = item(11L, "南山风景区", 3L, "120", "20", 1);
        FakeActions actions = new FakeActions(List.of(item), detail(item, List.of("https://invalid/image.jpg")));
        ScenicBrowsePanel panel = new ScenicBrowsePanel(
                new ImmediateTaskExecutor(), actions, ignored -> Optional.empty());

        click(panel, "查询");
        panel.selectRow(0);
        click(panel, "景点简介");

        assertFalse(panel.introductionImageVisible());
    }

    @Test
    void upperInfoTabsControlContentAndLowerDuplicateButtonsAreGone() {
        Item item = item(11L, "南山风景区", 3L, "120", "20", 1);
        FakeActions actions = new FakeActions(List.of(item), detail(item, List.of()));
        ScenicBrowsePanel panel = new ScenicBrowsePanel(new ImmediateTaskExecutor(), actions);

        click(panel, "查询");
        panel.selectRow(0);
        assertEquals(0, panel.selectedInfoTab());
        assertTrue(panel.infoIndicatorSelected(0));
        assertTrue(panel.infoTabClickable(0));
        assertTrue(panel.infoTabClickable(1));
        assertTrue(panel.infoTabClickable(2));

        click(panel, "游客评论");
        assertEquals(2, panel.selectedInfoTab());
        assertTrue(panel.infoIndicatorSelected(2));

        click(panel, "景点简介");
        assertEquals(1, panel.selectedInfoTab());
        assertTrue(panel.infoIndicatorSelected(1));
        assertEquals(1, actions.detailCalls);
        assertEquals(1, actions.commentCalls);
    }

    private static CrossDatabaseItemDTO detail(Item item, List<String> images) {
        CrossDatabaseItemDTO dto = new CrossDatabaseItemDTO();
        dto.setItem(item);
        dto.setDetail(new Document("description", "直接展示图片的景点简介")
                .append("images", images)
                .append("metadata", new Document()));
        return dto;
    }

    private static Item item(long id, String title, long categoryId,
                             String price, String discount, int status) {
        Item item = new Item();
        item.setItemId(id);
        item.setTitle(title);
        item.setCategoryId(categoryId);
        item.setPrice(new BigDecimal(price));
        item.setDiscountRate(new BigDecimal(discount));
        item.setStatus(status);
        return item;
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

    private static final class FakeActions implements ScenicBrowsePanel.Actions {
        private final List<Item> items;
        private final CrossDatabaseItemDTO detail;
        private int detailCalls;
        private int commentCalls;

        private FakeActions(List<Item> items) {
            this(items, null);
        }

        private FakeActions(List<Item> items, CrossDatabaseItemDTO detail) {
            this.items = items;
            this.detail = detail;
        }

        @Override
        public List<Category> loadCategories() {
            Category category = new Category();
            category.setCategoryId(3L);
            category.setName("自然风光");
            return List.of(category);
        }

        @Override
        public void categoriesLoaded(List<Category> categories) {
        }

        @Override
        public List<Item> searchItems(String keyword, Long categoryId) {
            return items;
        }

        @Override
        public List<RecommendationDTO> recommendForUser() {
            return List.of();
        }

        @Override
        public List<RecommendationDTO> recommendTopRated() {
            return List.of();
        }

        @Override
        public List<RecommendationDTO> recommendHot() {
            return List.of();
        }

        @Override
        public java.util.Map<Long, Double> loadRatings(List<Long> itemIds) {
            return java.util.Map.of(11L, 4.6);
        }

        @Override
        public CrossDatabaseItemDTO loadItemDetail(long itemId) {
            detailCalls++;
            return detail;
        }

        @Override
        public CommentListDTO loadComments(long itemId) {
            commentCalls++;
            return null;
        }

        @Override
        public String formatItemIntroduction(CrossDatabaseItemDTO dto) {
            return "简介";
        }

        @Override
        public String formatCommentViews(String itemTitle, CommentListDTO dto) {
            return "评论";
        }

        @Override
        public void showTicketAvailability(Item item) {
        }

        @Override
        public void showPurchase(Item item, JTextArea detailArea) {
        }

        @Override
        public void showComment(Item item, Runnable refreshComments) {
        }

        @Override
        public void setStatus(String message) {
        }
    }
}
