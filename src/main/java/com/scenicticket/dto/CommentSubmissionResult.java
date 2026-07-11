package com.scenicticket.dto;

public record CommentSubmissionResult(boolean updated, boolean auditRecorded, String message) {
}
