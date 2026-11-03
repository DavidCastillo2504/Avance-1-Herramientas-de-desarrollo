package com.sabortradicion.app.service;

import com.sabortradicion.app.dto.CreateReservationRequest;
import com.sabortradicion.app.dto.ReservationResponse;
import com.sabortradicion.app.entity.Reservation;
import com.sabortradicion.app.entity.ReservationStatus;
import com.sabortradicion.app.exception.BusinessRuleException;
import com.sabortradicion.app.exception.ResourceNotFoundException;
import com.sabortradicion.app.repository.ReservationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ReservationService {
    static final int SLOT_CAPACITY = 40;
    private static final LocalTime OPENING_TIME = LocalTime.of(12, 0);
    private static final LocalTime LAST_RESERVATION_TIME = LocalTime.of(22, 0);
    private static final ZoneId RESTAURANT_ZONE = ZoneId.of("America/Lima");

    private final ReservationRepository reservationRepository;
    private final Clock clock;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository) {
        this(reservationRepository, Clock.system(RESTAURANT_ZONE));
    }

    ReservationService(ReservationRepository reservationRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(CreateReservationRequest request) {
        LocalDateTime requestedAt = LocalDateTime.of(request.reservationDate(), request.reservationTime());
        if (!requestedAt.isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("La reserva debe programarse para una fecha y hora futura");
        }
        if (request.reservationDate().isAfter(LocalDateTime.now(clock).toLocalDate().plusDays(90))) {
            throw new BusinessRuleException("Las reservas solo pueden registrarse con 90 dias de anticipacion");
        }
        if (request.reservationTime().isBefore(OPENING_TIME) || request.reservationTime().isAfter(LAST_RESERVATION_TIME)) {
            throw new BusinessRuleException("El horario de reservas es de 12:00 a 22:00");
        }
        long occupied = reservationRepository.reservedGuests(request.reservationDate(), request.reservationTime(),
                ReservationStatus.CANCELLED);
        if (occupied + request.partySize() > SLOT_CAPACITY) {
            throw new BusinessRuleException("No hay capacidad suficiente para esa fecha y hora");
        }

        Reservation reservation = new Reservation(request.customerName().trim(), request.phone().trim(),
                request.reservationDate(), request.reservationTime(), request.partySize(), normalizeNotes(request.notes()));
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findAll(int page, int size) {
        return reservationRepository.findAllByOrderByReservationDateAscReservationTimeAsc(
                PageRequest.of(page, Math.min(size, 50))).map(ReservationResponse::from);
    }

    @Transactional
    public ReservationResponse changeStatus(UUID id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        reservation.changeStatus(status);
        return ReservationResponse.from(reservation);
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}
