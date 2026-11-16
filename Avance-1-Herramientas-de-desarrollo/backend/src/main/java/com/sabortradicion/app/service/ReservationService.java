package com.sabortradicion.app.service;

import com.sabortradicion.app.dto.CreateReservationRequest;
import com.sabortradicion.app.dto.ReservationResponse;
import com.sabortradicion.app.entity.Reservation;
import com.sabortradicion.app.entity.ReservationStatus;
import com.sabortradicion.app.exception.BusinessRuleException;
import com.sabortradicion.app.exception.ResourceNotFoundException;
import com.sabortradicion.app.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ReservationService {
    static final int SLOT_CAPACITY = 40;
    static final int SLOT_MINUTES = 30;
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
        LocalDate date = request.reservationDate();
        LocalTime time = request.reservationTime();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!LocalDateTime.of(date, time).isAfter(now)) {
            throw new BusinessRuleException("La reserva debe programarse para una fecha y hora futura");
        }
        if (date.isAfter(now.toLocalDate().plusDays(90))) {
            throw new BusinessRuleException("Las reservas solo pueden registrarse con 90 días de anticipación");
        }
        if (time.isBefore(OPENING_TIME) || time.isAfter(LAST_RESERVATION_TIME)) {
            throw new BusinessRuleException("El horario de reservas es de 12:00 a 22:00");
        }
        if (time.getMinute() % SLOT_MINUTES != 0 || time.getSecond() != 0 || time.getNano() != 0) {
            throw new BusinessRuleException("Las reservas se registran en punto o y media (por ejemplo 19:00 o 19:30)");
        }
        ensureCapacity(date, time, request.partySize());

        Reservation reservation = new Reservation(request.customerName().trim(), request.phone().trim(),
                date, time, request.partySize(), normalizeNotes(request.notes()));
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    /** Por defecto lista solo las reservas desde hoy (las próximas primero); con upcoming=false, todo el historial. */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> findAll(int page, int size, boolean upcoming) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 50));
        Page<Reservation> result = upcoming
                ? reservationRepository.findByReservationDateGreaterThanEqualOrderByReservationDateAscReservationTimeAsc(
                        LocalDate.now(clock), pageRequest)
                : reservationRepository.findAllByOrderByReservationDateDescReservationTimeDesc(pageRequest);
        return result.map(ReservationResponse::from);
    }

    @Transactional
    public ReservationResponse changeStatus(UUID id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        // Al reactivar una reserva cancelada vuelve a ocupar cupo: se valida que todavía haya espacio.
        if (reservation.getStatus() == ReservationStatus.CANCELLED && status != ReservationStatus.CANCELLED) {
            ensureCapacity(reservation.getReservationDate(), reservation.getReservationTime(), reservation.getPartySize());
        }
        reservation.changeStatus(status);
        return ReservationResponse.from(reservation);
    }

    private void ensureCapacity(LocalDate date, LocalTime time, int partySize) {
        reservationRepository.lockSlot(slotKey(date, time));
        long occupied = reservationRepository.reservedGuests(date, time, ReservationStatus.CANCELLED);
        if (occupied + partySize > SLOT_CAPACITY) {
            throw new BusinessRuleException("No hay capacidad suficiente para esa fecha y hora");
        }
    }

    static long slotKey(LocalDate date, LocalTime time) {
        return date.toEpochDay() * 10_000 + time.getHour() * 100L + time.getMinute();
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}
