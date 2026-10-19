package com.sabortradicion.app.dto;

import com.sabortradicion.app.entity.CustomerOrder;
import com.sabortradicion.app.entity.OrderStatus;
import com.sabortradicion.app.entity.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerName,
        String phone,
        OrderType orderType,
        String notes,
        OrderStatus status,
        BigDecimal total,
        Instant createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(order.getId(), order.getCustomerName(), order.getPhone(), order.getOrderType(),
                order.getNotes(), order.getStatus(), order.getTotal(), order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
