package com.scenicticket.dto;

import com.scenicticket.model.Item;
import org.bson.Document;

public class RecommendationDTO {
    private Item item;
    private Document detail;
    private Document ratingSummary;
    private double score;
    private String reason;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Document getDetail() {
        return detail;
    }

    public void setDetail(Document detail) {
        this.detail = detail;
    }

    public Document getRatingSummary() {
        return ratingSummary;
    }

    public void setRatingSummary(Document ratingSummary) {
        this.ratingSummary = ratingSummary;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
