package com.sabortradicion.app.dto;

import com.sabortradicion.app.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReservationStatusRequest(
        @NotNull(message = "El estado es obligatorio") ReservationStatus status
) {
}
