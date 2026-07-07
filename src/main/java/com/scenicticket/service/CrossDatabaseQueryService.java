package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Item;

import java.util.ArrayList;
import java.util.List;

public class CrossDatabaseQueryService {
    private final ItemDAO itemDAO;
    private final DetailDAO detailDAO;
    private final CommentDAO commentDAO;
    private final LogDAO logDAO;

    public CrossDatabaseQueryService() {
        this(new ItemDAO(), new DetailDAO(), new CommentDAO(), new LogDAO());
    }

    public CrossDatabaseQueryService(ItemDAO itemDAO, DetailDAO detailDAO, CommentDAO commentDAO, LogDAO logDAO) {
        this.itemDAO = itemDAO;
        this.detailDAO = detailDAO;
        this.commentDAO = commentDAO;
        this.logDAO = logDAO;
    }

    public CrossDatabaseItemDTO getItemDetail(long itemId, int commentLimit) {
        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new BusinessException("Item not found."));
        return enrich(item, commentLimit);
    }

    public List<CrossDatabaseItemDTO> searchItemDetails(String keyword, Long categoryId, int limit, int offset) {
        List<Item> items = itemDAO.search(keyword, categoryId, 1, limit, offset);
        List<CrossDatabaseItemDTO> results = new ArrayList<>();
        for (Item item : items) {
            results.add(enrich(item, 5));
        }
        return results;
    }

    public List<CrossDatabaseItemDTO> getItemDetailsByIds(List<Long> itemIds, int commentLimit) {
        List<Item> items = itemDAO.findByIds(itemIds);
        List<CrossDatabaseItemDTO> results = new ArrayList<>();
        for (Item item : items) {
            results.add(enrich(item, commentLimit));
        }
        return results;
    }

    private CrossDatabaseItemDTO enrich(Item item, int commentLimit) {
        CrossDatabaseItemDTO dto = new CrossDatabaseItemDTO();
        dto.setItem(item);
        dto.setDetail(detailDAO.findByItemId(item.getItemId()));
        dto.setRatingSummary(commentDAO.aggregateRatingByItem(item.getItemId()));
        dto.setComments(commentDAO.findByItemId(item.getItemId(), normalizeLimit(commentLimit)));
        dto.setBehaviorSummary(logDAO.findByItemId(item.getItemId(), null, 20));
        return dto;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 50);
    }
}
