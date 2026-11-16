package com.sabortradicion.app.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres") String name,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 60, message = "La categoría no puede superar 60 caracteres") String category,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.10", message = "El precio mínimo es S/ 0.10")
        @DecimalMax(value = "9999.99", message = "El precio máximo es S/ 9999.99")
        @Digits(integer = 4, fraction = 2, message = "El precio admite como máximo 2 decimales") BigDecimal price,

        @Size(max = 16, message = "El emoji no puede superar 16 caracteres") String emoji,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 300, message = "La descripción no puede superar 300 caracteres") String description,

        Boolean available,

        @Size(max = 500, message = "La URL de la imagen no puede superar 500 caracteres")
        @Pattern(regexp = "^https?://\\S+$", message = "La imagen debe ser una URL http(s) válida") String imageUrl
) {
}
