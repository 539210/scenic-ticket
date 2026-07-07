package com.scenicticket.dto;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class StatisticsReportDTO {
    private List<Document> hotItems = new ArrayList<>();
    private List<Document> actionTypeSummary = new ArrayList<>();
    private List<Document> dailyTrend = new ArrayList<>();
    private List<Document> hotTags = new ArrayList<>();

    public List<Document> getHotItems() {
        return hotItems;
    }

    public void setHotItems(List<Document> hotItems) {
        this.hotItems = hotItems;
    }

    public List<Document> getActionTypeSummary() {
        return actionTypeSummary;
    }

    public void setActionTypeSummary(List<Document> actionTypeSummary) {
        this.actionTypeSummary = actionTypeSummary;
    }

    public List<Document> getDailyTrend() {
        return dailyTrend;
    }

    public void setDailyTrend(List<Document> dailyTrend) {
        this.dailyTrend = dailyTrend;
    }

    public List<Document> getHotTags() {
        return hotTags;
    }

    public void setHotTags(List<Document> hotTags) {
        this.hotTags = hotTags;
    }
}
