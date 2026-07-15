package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.model.Item;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecommendService {
    private final ItemDAO itemDAO;
    private final LogDAO logDAO;
    private final CommentDAO commentDAO;
    private final DetailDAO detailDAO;

    public RecommendService() {
        this(new ItemDAO(), new LogDAO(), new CommentDAO(), new DetailDAO());
    }

    public RecommendService(ItemDAO itemDAO, LogDAO logDAO, CommentDAO commentDAO, DetailDAO detailDAO) {
        this.itemDAO = itemDAO;
        this.logDAO = logDAO;
        this.commentDAO = commentDAO;
        this.detailDAO = detailDAO;
    }

    public List<RecommendationDTO> recommendForUser(long userId, int limit) {
        return recommendTopRatedItems(limit);
    }

    public List<RecommendationDTO> recommendHotItems(Date startTime, Date endTime, int limit) {
        return recommendTopRatedItems(limit);
    }

    public List<RecommendationDTO> recommendTopRatedItems(int limit) {
        int safeLimit = normalizeLimit(limit);
        List<Document> ratedItems = commentDAO.aggregateTopRatedItems(safeLimit);
        Map<Long, Double> scores = new LinkedHashMap<>();
        for (Document document : ratedItems) {
            Long itemId = readLong(document.get("_id"));
            if (itemId != null) {
                scores.put(itemId, readDouble(document.get("avg_rating")));
            }
        }
        List<Item> items = itemDAO.findByIds(new ArrayList<>(scores.keySet()));
        return items.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .map(item -> toRecommendation(item, scores.getOrDefault(item.getItemId(), 0.0),
                        "游客平均评分 " + String.format("%.1f / 5", scores.getOrDefault(item.getItemId(), 0.0))))
                .sorted(Comparator.comparingDouble(RecommendationDTO::getScore).reversed())
                .limit(safeLimit)
                .toList();
    }

    public Map<Long, Double> ratingScores(List<Long> itemIds) {
        Map<Long, Double> ratings = new LinkedHashMap<>();
        if (itemIds == null) {
            return ratings;
        }
        for (Long itemId : itemIds.stream().filter(java.util.Objects::nonNull).distinct().toList()) {
            Document summary = commentDAO.aggregateRatingByItem(itemId);
            double average = summary == null ? 0.0 : readDouble(summary.get("avg_rating"));
            if (average > 0) {
                ratings.put(itemId, average);
            }
        }
        return ratings;
    }

    private List<RecommendationDTO> fillWithHotItems(Date startTime, Date endTime, int limit, Set<Long> excludedItemIds, String reason) {
        List<Document> hotItems = logDAO.aggregateHotItems(startTime, endTime, Math.max(limit * 2, 10));
        Map<Long, Double> scores = new LinkedHashMap<>();
        double maxActions = hotItems.stream()
                .mapToDouble(document -> readDouble(document.get("total_actions")))
                .max()
                .orElse(0.0);
        for (Document document : hotItems) {
            Long itemId = readLong(document.get("_id"));
            if (itemId != null && !excludedItemIds.contains(itemId)) {
                scores.put(itemId, toPercentScore(readDouble(document.get("total_actions")), maxActions));
            }
        }
        List<Item> items = itemDAO.findByIds(new ArrayList<>(scores.keySet()));
        return items.stream()
                .map(item -> toRecommendation(item, scores.getOrDefault(item.getItemId(), 0.0), reason))
                .sorted(Comparator.comparingDouble(RecommendationDTO::getScore).reversed())
                .limit(limit)
                .toList();
    }

    private RecommendationDTO toRecommendation(Item item, double score, String reason) {
        RecommendationDTO dto = new RecommendationDTO();
        dto.setItem(item);
        dto.setDetail(detailDAO.findByItemId(item.getItemId()));
        dto.setRatingSummary(commentDAO.aggregateRatingByItem(item.getItemId()));
        dto.setScore(score);
        dto.setReason(reason);
        return dto;
    }

    private List<Long> extractItemIds(List<Document> documents) {
        List<Long> itemIds = new ArrayList<>();
        for (Document document : documents) {
            Long itemId = readLong(document.get("_id"));
            if (itemId != null && itemId > 0) {
                itemIds.add(itemId);
            }
        }
        return itemIds;
    }

    private boolean containsItem(List<RecommendationDTO> recommendations, long itemId) {
        return recommendations.stream()
                .anyMatch(recommendation -> recommendation.getItem() != null
                        && recommendation.getItem().getItemId() == itemId);
    }

    private Long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private double readDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private double toPercentScore(double value, double maxValue) {
        if (value <= 0 || maxValue <= 0) {
            return 0.0;
        }
        return Math.min(100.0, value / maxValue * 100.0);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 50);
    }
}
