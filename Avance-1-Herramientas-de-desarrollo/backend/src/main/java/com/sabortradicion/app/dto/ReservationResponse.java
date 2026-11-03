package com.sabortradicion.app.dto;

import com.sabortradicion.app.entity.Reservation;
import com.sabortradicion.app.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id, String customerName, String phone, LocalDate reservationDate,
        LocalTime reservationTime, int partySize, String notes,
        ReservationStatus status, Instant createdAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getCustomerName(), reservation.getPhone(),
                reservation.getReservationDate(), reservation.getReservationTime(), reservation.getPartySize(),
                reservation.getNotes(), reservation.getStatus(), reservation.getCreatedAt());
    }
}
