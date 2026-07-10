package com.scenicticket.dto;

public record AdminChangeResult(boolean updated, boolean auditRecorded, String message) {
}
