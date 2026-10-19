package com.sabortradicion.app.dto;

import com.sabortradicion.app.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "El estado es obligatorio") OrderStatus status
) {
}
