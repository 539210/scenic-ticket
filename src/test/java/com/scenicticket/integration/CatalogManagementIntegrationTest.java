package com.scenicticket.integration;

import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import com.scenicticket.service.BusinessService;
import com.scenicticket.util.MongoDBUtil;
import com.scenicticket.util.MySQLDBUtil;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogManagementIntegrationTest {
    @BeforeAll
    static void verifyTargets() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true with explicit scenic_ticket_test overrides.");
        DatabaseTargetGuard.requireIsolatedTestTargets(DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));
    }

    @Test
    void administratorMaintainsCategoryTreeScenicItemAndStructuredDetail() throws Exception {
        BusinessService service = new BusinessService();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long rootCategoryId = 0;
        long childCategoryId = 0;
        long itemId = 0;
        try {
            rootCategoryId = service.createCategory(1L, "集成根分类-" + suffix, null);
            childCategoryId = service.createCategory(1L, "集成子分类-" + suffix, rootCategoryId);
            assertTrue(service.updateCategory(1L, childCategoryId, "集成子分类已更新-" + suffix, rootCategoryId));

            itemId = service.createItem(1L, "集成景点-" + suffix, childCategoryId, "初始中文简介",
                    List.of("cover.jpg"), new Document("open_time", "08:00-18:00"),
                    new BigDecimal("120.00"), new BigDecimal("10.00"));
            assertTrue(service.updateItem(1L, itemId, "集成景点已更新-" + suffix, rootCategoryId));
            assertTrue(service.updateItemDetail(1L, itemId, "更新后的 UTF-8 中文简介",
                    List.of("cover.jpg", "map.png"),
                    new Document("open_time", "08:30-19:00").append("level", "AAAA")));
            assertTrue(service.updateItemStatus(1L, itemId, 0));

            long createdItemId = itemId;
            long createdChildCategoryId = childCategoryId;
            Item item = service.searchAllItemsForAdmin(1L, suffix, null, 20, 0).stream()
                    .filter(candidate -> candidate.getItemId() == createdItemId).findFirst().orElseThrow();
            Document detail = service.getItemDetailForAdmin(1L, itemId);
            Category child = service.listCategories().stream()
                    .filter(category -> category.getCategoryId() == createdChildCategoryId).findFirst().orElseThrow();

            assertEquals(rootCategoryId, item.getCategoryId());
            assertEquals(0, item.getStatus());
            assertEquals(rootCategoryId, child.getParentId());
            assertEquals("更新后的 UTF-8 中文简介", detail.getString("description"));
            assertEquals(List.of("cover.jpg", "map.png"), detail.getList("images", String.class));
            assertEquals("AAAA", detail.get("metadata", Document.class).getString("level"));
        } finally {
            cleanup(itemId, childCategoryId, rootCategoryId);
        }
    }

    private static void cleanup(long itemId, long childCategoryId, long rootCategoryId) throws Exception {
        if (itemId > 0) {
            MongoDBUtil.getDatabase().getCollection("item_details")
                    .deleteMany(Filters.in("item_id", itemId, String.valueOf(itemId)));
        }
        try (Connection connection = MySQLDBUtil.getConnection()) {
            if (itemId > 0) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM items WHERE item_id = ?")) {
                    statement.setLong(1, itemId);
                    statement.executeUpdate();
                }
            }
            for (long categoryId : new long[]{childCategoryId, rootCategoryId}) {
                if (categoryId > 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM categories WHERE category_id = ?")) {
                        statement.setLong(1, categoryId);
                        statement.executeUpdate();
                    }
                }
            }
        }
    }
}
