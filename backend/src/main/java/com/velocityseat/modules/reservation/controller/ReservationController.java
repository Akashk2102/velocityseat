package com.velocityseat.modules.reservation.controller;

import com.velocityseat.common.dto.ApiResponse;
import com.velocityseat.modules.reservation.dto.HoldSeatsRequest;
import com.velocityseat.modules.reservation.dto.ReservationResponse;
import com.velocityseat.modules.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Seat Holds & Reservations", description = "Endpoints for acquiring and managing temporary seat reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/hold")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Hold seats temporarily (10-minute hold)",
               description = "Atomically acquires temporary hold in Redis for requested seats. Returns a reservation token.")
    public ResponseEntity<ApiResponse<ReservationResponse>> holdSeats(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody HoldSeatsRequest request) {

        ReservationResponse response = reservationService.holdSeats(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Seats temporarily reserved for 10 minutes"));
    }

    @PostMapping("/{token}/release")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Release seat hold early",
               description = "Cancels active temporary reservation and releases seats back to AVAILABLE immediately")
    public ResponseEntity<ApiResponse<Void>> releaseReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String token) {

        reservationService.releaseReservation(userDetails.getUsername(), token);
        return ResponseEntity.ok(ApiResponse.success(null, "Reservation released successfully"));
    }

    @GetMapping("/{token}")
    @Operation(summary = "Get reservation status & remaining time",
               description = "Returns current reservation status, held seats, and remaining seconds before TTL expiration")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(@PathVariable String token) {
        ReservationResponse response = reservationService.getReservation(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
