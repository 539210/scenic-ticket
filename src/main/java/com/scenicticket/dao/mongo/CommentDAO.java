package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import org.bson.Document;

public class CommentDAO extends MongoBaseDAO {
    public void insertComment(Document comment) {
        getCollection("comments").insertOne(comment);
    }
}
