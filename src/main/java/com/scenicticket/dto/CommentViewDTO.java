package com.scenicticket.dto;

import java.util.Date;
import java.util.List;

public record CommentViewDTO(long userId, String displayUsername, int rating, String content,
                             List<String> tags, Date createdAt, Date updatedAt) {
}
