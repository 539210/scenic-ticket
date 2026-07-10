package com.scenicticket.ui;

public record CategoryOption(String label, Long categoryId) {
    @Override
    public String toString() {
        return label;
    }
}
