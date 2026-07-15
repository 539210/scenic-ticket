package com.scenicticket.dto;

import java.util.Date;

public record CommentViewDTO(long userId, String displayUsername, int rating, String content,
                             Date createdAt, Date updatedAt) {
}
