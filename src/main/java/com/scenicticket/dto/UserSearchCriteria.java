package com.scenicticket.dto;

public record UserSearchCriteria(
        Long userId,
        String username,
        String email,
        String role,
        Integer status,
        int limit,
        int offset
) {
    public UserSearchCriteria(String username, String email, String role, Integer status, int limit, int offset) {
        this(null, username, email, role, status, limit, offset);
    }
}
