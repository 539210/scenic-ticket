package com.scenicticket.dto;

import com.scenicticket.model.Item;
import org.bson.Document;

import java.util.List;

public class ItemDetailDTO {
    private Item item;
    private Document detail;
    private List<Document> comments;

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

    public List<Document> getComments() {
        return comments;
    }

    public void setComments(List<Document> comments) {
        this.comments = comments;
    }
}
