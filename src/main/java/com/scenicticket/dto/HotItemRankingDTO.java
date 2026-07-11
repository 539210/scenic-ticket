package com.scenicticket.dto;

public class HotItemRankingDTO {
    private long itemId;
    private String itemTitle;
    private String categoryName;
    private Integer itemStatus;
    private long totalActions;
    private long viewCount;
    private long orderCount;
    private double avgDuration;
    private boolean itemFound;

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(Integer itemStatus) {
        this.itemStatus = itemStatus;
    }

    public long getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(long totalActions) {
        this.totalActions = totalActions;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public double getAvgDuration() {
        return avgDuration;
    }

    public void setAvgDuration(double avgDuration) {
        this.avgDuration = avgDuration;
    }

    public boolean isItemFound() {
        return itemFound;
    }

    public void setItemFound(boolean itemFound) {
        this.itemFound = itemFound;
    }
}
