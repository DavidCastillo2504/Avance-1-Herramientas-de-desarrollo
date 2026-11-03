package com.sabortradicion.app.controller;

import com.sabortradicion.app.dto.CreateReservationRequest;
import com.sabortradicion.app.dto.ReservationResponse;
import com.sabortradicion.app.dto.UpdateReservationStatusRequest;
import com.sabortradicion.app.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse created = reservationService.create(request);
        return ResponseEntity.created(URI.create("/api/reservations/" + created.id())).body(created);
    }

    @GetMapping
    public Page<ReservationResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return reservationService.findAll(page, size);
    }

    @PatchMapping("/{id}/status")
    public ReservationResponse changeStatus(@PathVariable UUID id,
            @Valid @RequestBody UpdateReservationStatusRequest request) {
        return reservationService.changeStatus(id, request.status());
    }
}
