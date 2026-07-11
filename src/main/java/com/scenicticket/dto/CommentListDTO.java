package com.scenicticket.dto;

import org.bson.Document;

import java.util.List;

public record CommentListDTO(Document ratingSummary, List<CommentViewDTO> comments) {
}
