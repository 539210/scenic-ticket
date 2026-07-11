package com.scenicticket.dto;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class StatisticsReportDTO {
    private List<HotItemRankingDTO> hotItems = new ArrayList<>();
    private List<Document> actionTypeSummary = new ArrayList<>();
    private List<Document> dailyTrend = new ArrayList<>();
    private List<Document> hotTags = new ArrayList<>();
    private List<Document> systemAuditSummary = new ArrayList<>();
    private List<Document> systemAuditTrend = new ArrayList<>();
    private List<MonthlyOrderReportDTO> monthlyOrderReport = new ArrayList<>();

    public List<HotItemRankingDTO> getHotItems() {
        return hotItems;
    }

    public void setHotItems(List<HotItemRankingDTO> hotItems) {
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

    public List<Document> getSystemAuditSummary() {
        return systemAuditSummary;
    }

    public void setSystemAuditSummary(List<Document> systemAuditSummary) {
        this.systemAuditSummary = systemAuditSummary;
    }

    public List<Document> getSystemAuditTrend() {
        return systemAuditTrend;
    }

    public void setSystemAuditTrend(List<Document> systemAuditTrend) {
        this.systemAuditTrend = systemAuditTrend;
    }

    public List<MonthlyOrderReportDTO> getMonthlyOrderReport() {
        return monthlyOrderReport;
    }

    public void setMonthlyOrderReport(List<MonthlyOrderReportDTO> monthlyOrderReport) {
        this.monthlyOrderReport = monthlyOrderReport;
    }
}
