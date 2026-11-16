package com.sabortradicion.app.repository;

import com.sabortradicion.app.entity.Reservation;
import com.sabortradicion.app.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Page<Reservation> findAllByOrderByReservationDateDescReservationTimeDesc(Pageable pageable);

    Page<Reservation> findByReservationDateGreaterThanEqualOrderByReservationDateAscReservationTimeAsc(
            LocalDate from, Pageable pageable);

    @Query("select coalesce(sum(r.partySize), 0) from Reservation r " +
            "where r.reservationDate = :date and r.reservationTime = :time and r.status <> :excludedStatus")
    long reservedGuests(@Param("date") LocalDate date, @Param("time") LocalTime time,
                        @Param("excludedStatus") ReservationStatus excludedStatus);

    /**
     * Bloqueo de PostgreSQL que dura hasta el fin de la transacción. Serializa las reservas de una misma
     * franja para que dos solicitudes simultáneas no superen el cupo.
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(:slotKey)) as slot_lock", nativeQuery = true)
    Integer lockSlot(@Param("slotKey") long slotKey);
}
