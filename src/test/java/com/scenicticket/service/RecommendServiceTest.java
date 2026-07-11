package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.model.Item;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendServiceTest {
    private final StubItemDAO itemDAO = new StubItemDAO();
    private final StubLogDAO logDAO = new StubLogDAO();
    private final StubCommentDAO commentDAO = new StubCommentDAO();
    private final RecommendService service = new RecommendService(itemDAO, logDAO, commentDAO, new StubDetailDAO());

    @Test
    void hotRecommendationsUseUnifiedPercentScore() {
        logDAO.hotItems = List.of(
                new Document("_id", 1L).append("total_actions", 20),
                new Document("_id", 2L).append("total_actions", 10)
        );

        List<RecommendationDTO> recommendations = service.recommendHotItems(null, null, 10);

        assertEquals(2, recommendations.size());
        assertEquals(1L, recommendations.get(0).getItem().getItemId());
        assertEquals(100.0, recommendations.get(0).getScore(), 0.001);
        assertEquals("近期热门景点", recommendations.get(0).getReason());
        assertEquals(50.0, recommendations.get(1).getScore(), 0.001);
    }

    @Test
    void topRatedRecommendationsUseUnifiedPercentScore() {
        commentDAO.topRatedItems = List.of(
                new Document("_id", 1L).append("avg_rating", 4.5),
                new Document("_id", 2L).append("avg_rating", 3.0)
        );

        List<RecommendationDTO> recommendations = service.recommendTopRatedItems(10);

        assertEquals(2, recommendations.size());
        assertEquals(1L, recommendations.get(0).getItem().getItemId());
        assertEquals(90.0, recommendations.get(0).getScore(), 0.001);
        assertEquals("高评分景点推荐", recommendations.get(0).getReason());
        assertEquals(60.0, recommendations.get(1).getScore(), 0.001);
    }

    private static class StubItemDAO extends ItemDAO {
        private final Map<Long, Item> items = Map.of(
                1L, item(1L, "南山日出观景区"),
                2L, item(2L, "青河峡谷漂流")
        );

        @Override
        public List<Item> findByIds(List<Long> itemIds) {
            return itemIds.stream()
                    .map(items::get)
                    .filter(item -> item != null)
                    .toList();
        }

        private static Item item(long itemId, String title) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setTitle(title);
            item.setCategoryId(1L);
            item.setStatus(1);
            return item;
        }
    }

    private static class StubLogDAO extends LogDAO {
        private List<Document> hotItems = List.of();

        @Override
        public List<Document> aggregateHotItems(Date startTime, Date endTime, int limit) {
            return hotItems;
        }
    }

    private static class StubCommentDAO extends CommentDAO {
        private List<Document> topRatedItems = List.of();

        @Override
        public List<Document> aggregateTopRatedItems(int limit) {
            return topRatedItems;
        }

        @Override
        public Document aggregateRatingByItem(long itemId) {
            return new Document();
        }
    }

    private static class StubDetailDAO extends DetailDAO {
        @Override
        public Document findByItemId(long itemId) {
            return new Document();
        }
    }
}
