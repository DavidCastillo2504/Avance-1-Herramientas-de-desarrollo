package com.sabortradicion.app.service;

import com.sabortradicion.app.dto.CreateReservationRequest;
import com.sabortradicion.app.entity.Reservation;
import com.sabortradicion.app.exception.BusinessRuleException;
import com.sabortradicion.app.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    private ReservationRepository repository;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-19T15:00:00Z"), ZoneId.of("America/Lima"));
        service = new ReservationService(repository, clock);
    }

    @Test
    void createsAReservationWhenTheSlotHasCapacity() {
        when(repository.reservedGuests(any(), any(), any())).thenReturn(10L);
        when(repository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new CreateReservationRequest("Ana", "999111222", LocalDate.of(2026, 10, 20),
                LocalTime.of(19, 30), 4, "Cumpleanos");

        var response = service.create(request);

        assertThat(response.partySize()).isEqualTo(4);
        assertThat(response.status().name()).isEqualTo("PENDING");
    }

    @Test
    void rejectsAReservationWhenTheSlotExceedsCapacity() {
        when(repository.reservedGuests(any(), any(), any())).thenReturn(38L);
        var request = new CreateReservationRequest("Ana", "999111222", LocalDate.of(2026, 10, 20),
                LocalTime.of(19, 30), 4, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacidad");
    }

    @Test
    void rejectsReservationsOutsideOpeningHours() {
        var request = new CreateReservationRequest("Ana", "999111222", LocalDate.of(2026, 10, 20),
                LocalTime.of(10, 0), 2, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("12:00 a 22:00");
    }
}
