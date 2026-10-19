package com.sabortradicion.app.dto;

import com.sabortradicion.app.entity.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres") String customerName,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9+() -]{7,20}$", message = "Ingresa un teléfono válido") String phone,

        @NotNull(message = "El tipo de pedido es obligatorio") OrderType orderType,

        @Size(max = 500, message = "Las indicaciones no pueden superar 500 caracteres") String notes,

        @NotEmpty(message = "Agrega al menos un plato")
        List<@Valid CreateOrderItemRequest> items
) {
}
