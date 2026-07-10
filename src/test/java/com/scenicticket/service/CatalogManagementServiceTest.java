package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mysql.CategoryDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import com.scenicticket.model.User;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogManagementServiceTest {
    @Test
    void categoryParentMustExistAndCannotCreateCycle() {
        FakeCategoryDAO categories = new FakeCategoryDAO(List.of(
                category(1L, "自然风光", null),
                category(2L, "森林", 1L),
                category(3L, "湿地", 2L)
        ));
        BusinessService service = service(categories, new FakeItemDAO(), new CapturingDetailDAO());

        assertThrows(BusinessException.class, () -> service.createCategory(9L, "无效分类", 99L));
        assertThrows(BusinessException.class, () -> service.updateCategory(9L, 1L, "自然景观", 1L));
        assertThrows(BusinessException.class, () -> service.updateCategory(9L, 1L, "自然景观", 3L));
    }

    @Test
    void administratorCanRenameAndMoveCategory() {
        FakeCategoryDAO categories = new FakeCategoryDAO(List.of(
                category(1L, "自然风光", null), category(2L, "森林", 1L)
        ));
        BusinessService service = service(categories, new FakeItemDAO(), new CapturingDetailDAO());

        assertTrue(service.updateCategory(9L, 2L, "  森林公园  ", null));

        assertEquals("森林公园", categories.updated.getName());
        assertEquals(null, categories.updated.getParentId());
    }

    @Test
    void administratorCanUpdateItemTitleAndCategoryWithoutChangingPricingOrStatus() {
        FakeCategoryDAO categories = new FakeCategoryDAO(List.of(category(4L, "文化古迹", null)));
        FakeItemDAO items = new FakeItemDAO();
        BusinessService service = service(categories, items, new CapturingDetailDAO());

        assertTrue(service.updateItem(9L, 7L, "  古城夜游  ", 4L));

        assertEquals("古城夜游", items.updated.getTitle());
        assertEquals(4L, items.updated.getCategoryId());
        assertEquals(new BigDecimal("88.00"), items.updated.getPrice());
        assertEquals(new BigDecimal("20.00"), items.updated.getDiscountRate());
        assertEquals(0, items.updated.getStatus());
    }

    @Test
    void detailUpdateNormalizesImagesAndPreservesStructuredMetadata() {
        FakeCategoryDAO categories = new FakeCategoryDAO(List.of(category(1L, "自然风光", null)));
        CapturingDetailDAO details = new CapturingDetailDAO();
        BusinessService service = service(categories, new FakeItemDAO(), details);
        Document metadata = new Document("open_time", "08:30-18:00").append("features", List.of("索道", "步道"));

        assertTrue(service.updateItemDetail(9L, 7L, "  UTF-8 中文简介  ",
                List.of(" cover.jpg ", "", "map.png", "cover.jpg"), metadata));

        assertEquals("UTF-8 中文简介", details.description);
        assertEquals(List.of("cover.jpg", "map.png"), details.images);
        assertEquals(metadata, details.metadata);
    }

    @Test
    void scenicItemIsAutomaticallyTakenOfflineWhenInitialMongoDetailWriteFails() {
        FakeCategoryDAO categories = new FakeCategoryDAO(List.of(category(1L, "自然风光", null)));
        CreatingItemDAO items = new CreatingItemDAO();
        BusinessService service = service(categories, items, new DetailDAO() {
            @Override
            public void upsertDetail(long itemId, String description, List<String> images, Document metadata) {
                throw new IllegalStateException("Mongo unavailable");
            }
        });

        assertThrows(DBException.class, () -> service.createItem(9L, "测试景点", 1L, "简介",
                List.of(), new Document(), new BigDecimal("80.00"), BigDecimal.ZERO));

        assertEquals(0, items.compensatingStatus);
        assertEquals(77L, items.compensatingItemId);
    }

    private static BusinessService service(CategoryDAO categories, ItemDAO items, DetailDAO details) {
        return new BusinessService(categories, items, new OrderDAO(), details, new LogDAO(), new CommentDAO(),
                () -> {
                    throw new AssertionError("No connection expected");
                }, allowAllAuthorization());
    }

    private static AuthorizationService allowAllAuthorization() {
        return new AuthorizationService(new UserDAO()) {
            @Override
            public User requireAdmin(long actorUserId) {
                User user = new User();
                user.setUserId(actorUserId);
                user.setRole("ADMIN");
                user.setStatus(1);
                return user;
            }
        };
    }

    private static Category category(long id, String name, Long parentId) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        category.setParentId(parentId);
        return category;
    }

    private static class FakeCategoryDAO extends CategoryDAO {
        private final List<Category> categories;
        private Category updated;

        private FakeCategoryDAO(List<Category> categories) {
            this.categories = new ArrayList<>(categories);
        }

        @Override
        public Optional<Category> findById(long categoryId) {
            return categories.stream().filter(category -> category.getCategoryId() == categoryId).findFirst();
        }

        @Override
        public List<Category> findAll() {
            return List.copyOf(categories);
        }

        @Override
        public boolean update(Category category) {
            updated = category;
            return true;
        }
    }

    private static class FakeItemDAO extends ItemDAO {
        private Item updated;

        @Override
        public Optional<Item> findById(long itemId) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setTitle("旧名称");
            item.setCategoryId(1L);
            item.setPrice(new BigDecimal("88.00"));
            item.setDiscountRate(new BigDecimal("20.00"));
            item.setStatus(0);
            return Optional.of(item);
        }

        @Override
        public boolean update(Item item) {
            updated = item;
            return true;
        }
    }

    private static class CreatingItemDAO extends FakeItemDAO {
        private long compensatingItemId;
        private int compensatingStatus = -1;

        @Override
        public long create(Item item) {
            return 77L;
        }

        @Override
        public boolean updateStatus(long itemId, int status) {
            compensatingItemId = itemId;
            compensatingStatus = status;
            return true;
        }
    }

    private static class CapturingDetailDAO extends DetailDAO {
        private String description;
        private List<String> images;
        private Document metadata;

        @Override
        public void upsertDetail(long itemId, String description, List<String> images, Document metadata) {
            this.description = description;
            this.images = images;
            this.metadata = metadata;
        }
    }
}
