package com.scenicticket.dto;

import com.scenicticket.model.Profile;
import com.scenicticket.model.Order;
import com.scenicticket.model.User;
import org.bson.Document;

import java.util.List;

public class AdminUserDetailDTO {
    private User user;
    private Profile profile;
    private UserOrderSummaryDTO orderSummary = new UserOrderSummaryDTO();
    private long behaviorCount;
    private boolean behaviorDataAvailable = true;
    private List<Order> recentOrders = List.of();
    private List<Document> recentComments = List.of();
    private List<Document> recentActions = List.of();
    private List<Document> recentAuditLogs = List.of();

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }
    public UserOrderSummaryDTO getOrderSummary() { return orderSummary; }
    public void setOrderSummary(UserOrderSummaryDTO orderSummary) { this.orderSummary = orderSummary; }
    public long getBehaviorCount() { return behaviorCount; }
    public void setBehaviorCount(long behaviorCount) { this.behaviorCount = behaviorCount; }
    public boolean isBehaviorDataAvailable() { return behaviorDataAvailable; }
    public void setBehaviorDataAvailable(boolean behaviorDataAvailable) { this.behaviorDataAvailable = behaviorDataAvailable; }
    public List<Order> getRecentOrders() { return recentOrders; }
    public void setRecentOrders(List<Order> recentOrders) { this.recentOrders = recentOrders == null ? List.of() : recentOrders; }
    public List<Document> getRecentComments() { return recentComments; }
    public void setRecentComments(List<Document> recentComments) { this.recentComments = recentComments == null ? List.of() : recentComments; }
    public List<Document> getRecentActions() { return recentActions; }
    public void setRecentActions(List<Document> recentActions) { this.recentActions = recentActions == null ? List.of() : recentActions; }
    public List<Document> getRecentAuditLogs() { return recentAuditLogs; }
    public void setRecentAuditLogs(List<Document> recentAuditLogs) { this.recentAuditLogs = recentAuditLogs == null ? List.of() : recentAuditLogs; }
}
