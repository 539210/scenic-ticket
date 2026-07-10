package com.scenicticket.dto;

import com.scenicticket.model.Order;

public class OrderViewDTO {
    private Order order;
    private String itemTitle;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }
}
