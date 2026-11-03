package com.sabortradicion.app.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateReservationRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres") String customerName,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^[0-9+() -]{7,20}$", message = "Ingresa un telefono valido") String phone,
        @NotNull(message = "La fecha es obligatoria")
        @FutureOrPresent(message = "La fecha no puede estar en el pasado") LocalDate reservationDate,
        @NotNull(message = "La hora es obligatoria") LocalTime reservationTime,
        @Min(value = 1, message = "La reserva debe ser para al menos una persona")
        @Max(value = 20, message = "La reserva admite hasta 20 personas") int partySize,
        @Size(max = 500, message = "Las indicaciones no pueden superar 500 caracteres") String notes
) {
}
