package com.scenicticket.ui;

import com.scenicticket.dto.CommentListDTO;
import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.model.Item;
import org.bson.Document;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ItemDisplayFormatter {
    private final Function<Long, String> categoryName;

    public ItemDisplayFormatter(Function<Long, String> categoryName) {
        this.categoryName = Objects.requireNonNull(categoryName, "categoryName");
    }

    public String formatIntroduction(CrossDatabaseItemDTO dto) {
        Objects.requireNonNull(dto, "dto");
        Item item = Objects.requireNonNull(dto.getItem(), "item");
        StringBuilder builder = new StringBuilder("景点简介")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append(text(item.getTitle(), "未命名景点")).append(System.lineSeparator())
                .append("类型：").append(text(categoryName.apply(item.getCategoryId()), "未分类"))
                .append(System.lineSeparator())
                .append("门票原价：").append(UiFormatters.money(item.getPrice())).append(System.lineSeparator())
                .append("优惠：").append(UiFormatters.discount(item.getDiscountRate())).append(System.lineSeparator())
                .append("折后单价：").append(UiFormatters.money(UiFormatters.discountedUnitPrice(
                        item.getPrice(), item.getDiscountRate())))
                .append(System.lineSeparator())
                .append("状态：").append(UiFormatters.itemStatus(item.getStatus()))
                .append(System.lineSeparator()).append(System.lineSeparator());

        Document detail = dto.getDetail();
        Object description = detail == null ? null : detail.get("description");
        Object metadata = detail == null ? null : detail.get("metadata");
        builder.append("简介：").append(UiFormatters.readableText(description, "管理员暂未填写景点简介"))
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("扩展属性：").append(formatMetadata(metadata));
        return builder.toString();
    }

    public String formatComments(String itemTitle, CommentListDTO dto) {
        Objects.requireNonNull(dto, "dto");
        StringBuilder builder = new StringBuilder("游客评论")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("景点：").append(text(itemTitle, "未命名景点")).append(System.lineSeparator())
                .append("评分概览：").append(formatRatingSummary(dto.ratingSummary()))
                .append(System.lineSeparator()).append(System.lineSeparator());
        if (dto.comments() == null || dto.comments().isEmpty()) {
            return builder.append("暂无评论").toString();
        }
        int index = 1;
        for (var comment : dto.comments()) {
            List<String> tags = comment.tags() == null ? List.of() : comment.tags();
            builder.append(index++).append(". 用户：").append(text(comment.displayUsername(), "未知用户"))
                    .append("  评分：").append(comment.rating()).append(System.lineSeparator())
                    .append("   正文：").append(UiFormatters.readableText(
                            comment.content(), "该评论没有文字内容"))
                    .append(System.lineSeparator())
                    .append("   标签：").append(formatTags(tags))
                    .append(System.lineSeparator())
                    .append("   创建：").append(UiFormatters.date(comment.createdAt()))
                    .append("  更新：").append(UiFormatters.date(comment.updatedAt()))
                    .append(System.lineSeparator()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String formatRatingSummary(Document summary) {
        if (summary == null || summary.isEmpty()) {
            return "暂无评分";
        }
        return "评论数：" + integer(summary.get("comment_count"))
                + "，平均分：" + decimal(summary.get("avg_rating"))
                + "，最高分：" + integer(summary.get("max_rating"))
                + "，最低分：" + integer(summary.get("min_rating"));
    }

    private String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "无";
        }
        List<String> readableTags = tags.stream()
                .filter(Objects::nonNull)
                .map(tag -> UiFormatters.readableText(tag, ""))
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
        return readableTags.isEmpty() ? "无" : String.join("、", readableTags);
    }

    private String formatMetadata(Object value) {
        if (value instanceof Document document) {
            return document.isEmpty() ? "暂无" : document.toJson();
        }
        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
            Document document = new Document();
            map.forEach((key, entry) -> document.put(String.valueOf(key), entry));
            return document.toJson();
        }
        return UiFormatters.readableText(value, "暂无");
    }

    private String integer(Object value) {
        return value instanceof Number number ? String.valueOf(number.longValue()) : text(value, "-");
    }

    private String decimal(Object value) {
        return value instanceof Number number
                ? String.format(Locale.ROOT, "%.2f", number.doubleValue())
                : text(value, "-");
    }

    private String text(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }
}
