package com.sabortradicion.app.dto;

import com.sabortradicion.app.entity.MenuItem;

import java.math.BigDecimal;

public record MenuItemResponse(
        Long id,
        String name,
        String category,
        BigDecimal price,
        String emoji,
        String description,
        boolean available
) {
    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getName(), item.getCategory(), item.getPrice(),
                item.getEmoji(), item.getDescription(), item.isAvailable());
    }
}
