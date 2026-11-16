package com.sabortradicion.app.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(
        @NotNull(message = "La disponibilidad es obligatoria") Boolean available
) {
}
