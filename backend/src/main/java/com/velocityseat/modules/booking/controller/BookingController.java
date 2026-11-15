package com.velocityseat.modules.booking.controller;

import com.velocityseat.common.dto.ApiResponse;
import com.velocityseat.modules.booking.dto.BookingResponse;
import com.velocityseat.modules.booking.dto.ConfirmBookingRequest;
import com.velocityseat.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings & Tickets", description = "Endpoints for confirming bookings, viewing tickets, and cancellations")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/confirm")
    @Operation(
        summary = "Confirm booking & process payment (Idempotent)",
        description = "Validates the temporary hold, acquires database row locks, confirms booking, and records payment. Safe to retry with the same Idempotency-Key.",
        parameters = {
            @Parameter(
                name = "Idempotency-Key",
                in = ParameterIn.HEADER,
                description = "Unique client-generated key (UUID) to prevent duplicate charges on network retries",
                required = false,
                schema = @Schema(type = "string")
            )
        }
    )
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ConfirmBookingRequest request) {

        BookingResponse response = bookingService.confirmBooking(userDetails.getUsername(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Booking confirmed successfully"));
    }

    @GetMapping("/my-bookings")
    @Operation(summary = "Get current user booking history", description = "Returns all confirmed, cancelled, and past bookings for the caller")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<BookingResponse> bookings = bookingService.getUserBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/{bookingReference}")
    @Operation(summary = "Get ticket details by reference", description = "Fetches complete digital ticket details, seating breakdown, and payment reference")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingReference) {

        BookingResponse response = bookingService.getBookingByReference(userDetails.getUsername(), bookingReference);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{bookingReference}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancels a confirmed booking, initiates refund, and releases seats back to AVAILABLE")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingReference) {

        bookingService.cancelBooking(userDetails.getUsername(), bookingReference);
        return ResponseEntity.ok(ApiResponse.success(null, "Booking cancelled and seats released successfully"));
    }
}
