package com.scenicticket.dto;

public record UserSearchCriteria(
        String username,
        String email,
        String role,
        Integer status,
        int limit,
        int offset
) {
}
