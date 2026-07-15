package com.scenicticket.ui;

import com.scenicticket.dto.CommentListDTO;
import com.scenicticket.dto.CommentViewDTO;
import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.model.Item;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDisplayFormatterTest {
    private final ItemDisplayFormatter formatter = new ItemDisplayFormatter(
            categoryId -> categoryId != null && categoryId == 2L ? "古典园林" : "未分类");

    @Test
    void introductionShowsIndependentBusinessFieldsAndStructuredDetail() {
        CrossDatabaseItemDTO dto = dto("苏州园林中文简介",
                List.of("https://img/a.jpg", "https://img/b.jpg"),
                new Document("open_time", "08:30-17:30")
                        .append("address", "苏州市园林路 1 号")
                        .append("notice", "请提前预约"));

        String text = formatter.formatIntroduction(dto);

        assertTrue(text.contains("拙政园"));
        assertTrue(text.contains("类型：古典园林"));
        assertTrue(text.contains("门票原价：¥100.00"));
        assertTrue(text.contains("优惠：减免20%"));
        assertTrue(text.contains("折后单价：¥80.00"));
        assertTrue(!text.contains("状态："));
        assertTrue(text.contains("简介：苏州园林中文简介"));
        assertTrue(!text.contains("图片地址"));
        assertTrue(!text.contains("https://img/a.jpg"));
        assertTrue(text.contains("开放时间：08:30-17:30"));
        assertTrue(text.contains("景点地址：苏州市园林路 1 号"));
        assertTrue(text.contains("游览提示：请提前预约"));
        assertTrue(!text.contains("扩展属性"));
    }

    @Test
    void missingAndHistoricallyCorruptedDescriptionsHaveExplicitFallbacks() {
        CrossDatabaseItemDTO missing = dto(null, null, null);
        CrossDatabaseItemDTO corrupted = dto("????????", List.of(), Map.of("legacy", true));

        assertTrue(formatter.formatIntroduction(missing).contains("简介：管理员暂未填写景点简介"));
        String corruptedText = formatter.formatIntroduction(corrupted);
        assertTrue(corruptedText.contains("简介：历史数据编码异常，暂无法显示"));
        assertTrue(corruptedText.contains("开放时间：暂未填写"));
        assertTrue(!corruptedText.contains("legacy"));
    }

    @Test
    void commentsShowMaskedUserRatingBodyAndBothTimestamps() {
        Date created = Date.from(Instant.parse("2026-07-12T02:03:04Z"));
        Date updated = Date.from(Instant.parse("2026-07-13T03:04:05Z"));
        CommentListDTO dto = new CommentListDTO(new Document("comment_count", 1)
                .append("avg_rating", 4.5).append("max_rating", 5).append("min_rating", 4),
                List.of(new CommentViewDTO(8L, "张***", 5, "环境很好", created, updated)));

        String text = formatter.formatComments("拙政园", dto);

        assertTrue(text.contains("评论数：1，平均分：4.50，最高分：5，最低分：4"));
        assertTrue(text.contains("用户：张***  评分：5"));
        assertTrue(text.contains("正文：环境很好"));
        assertTrue(!text.contains("标签："));
        assertTrue(text.contains("创建：" + UiFormatters.date(created)));
        assertTrue(text.contains("更新：" + UiFormatters.date(updated)));
    }

    @Test
    void emptyCommentsAndCorruptedBodiesAreNotRenderedAsQuestionMarks() {
        CommentListDTO empty = new CommentListDTO(new Document(), List.of());
        assertTrue(formatter.formatComments("狮子林", empty).contains("暂无评分"));
        assertTrue(formatter.formatComments("狮子林", empty).contains("暂无评论"));

        CommentListDTO corrupted = new CommentListDTO(new Document(), List.of(
                new CommentViewDTO(9L, "李***", 3, "????????", null, null)));
        String text = formatter.formatComments("狮子林", corrupted);
        assertTrue(text.contains("正文：历史数据编码异常，暂无法显示"));
        assertTrue(!text.contains("正文：????????"));
        assertTrue(!text.contains("标签："));
    }

    private CrossDatabaseItemDTO dto(Object description, Object images, Object metadata) {
        Item item = new Item();
        item.setItemId(7L);
        item.setTitle("拙政园");
        item.setCategoryId(2L);
        item.setPrice(new BigDecimal("100.00"));
        item.setDiscountRate(new BigDecimal("20"));
        item.setStatus(1);
        CrossDatabaseItemDTO dto = new CrossDatabaseItemDTO();
        dto.setItem(item);
        if (description != null || images != null || metadata != null) {
            dto.setDetail(new Document("description", description)
                    .append("images", images).append("metadata", metadata));
        }
        return dto;
    }
}
