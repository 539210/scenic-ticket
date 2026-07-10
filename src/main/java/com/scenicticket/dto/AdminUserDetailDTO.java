package com.scenicticket.dto;

import com.scenicticket.model.Profile;
import com.scenicticket.model.User;

public class AdminUserDetailDTO {
    private User user;
    private Profile profile;
    private UserOrderSummaryDTO orderSummary = new UserOrderSummaryDTO();
    private long behaviorCount;
    private boolean behaviorDataAvailable = true;

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
}
