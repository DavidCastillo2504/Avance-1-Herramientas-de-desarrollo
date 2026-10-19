package com.sabortradicion.app.dto;

import com.sabortradicion.app.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long menuItemId,
        String name,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getMenuItemId(), item.getItemName(), item.getUnitPrice(),
                item.getQuantity(), item.getSubtotal());
    }
}
