package com.scenicticket.dto;

import com.scenicticket.model.Item;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class CrossDatabaseItemDTO {
    private Item item;
    private Document detail;
    private Document ratingSummary;
    private List<Document> comments = new ArrayList<>();
    private List<Document> behaviorSummary = new ArrayList<>();

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

    public List<Document> getComments() {
        return comments;
    }

    public void setComments(List<Document> comments) {
        this.comments = comments;
    }

    public List<Document> getBehaviorSummary() {
        return behaviorSummary;
    }

    public void setBehaviorSummary(List<Document> behaviorSummary) {
        this.behaviorSummary = behaviorSummary;
    }
}
