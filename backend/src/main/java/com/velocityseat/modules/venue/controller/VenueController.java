package com.velocityseat.modules.venue.controller;

import com.velocityseat.common.dto.ApiResponse;
import com.velocityseat.modules.venue.dto.*;
import com.velocityseat.modules.venue.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
@Tag(name = "Venues & Seats", description = "Endpoints for managing venues and seating geometry")
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create venue (Admin only)", description = "Registers a new arena, stadium, or theater")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(@Valid @RequestBody CreateVenueRequest request) {
        VenueResponse response = venueService.createVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Venue created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all venues", description = "Returns a list of all registered venues")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues() {
        return ResponseEntity.ok(ApiResponse.success(venueService.getAllVenues()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get venue details", description = "Returns details and capacity for a specific venue")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(venueService.getVenueById(id)));
    }

    @PostMapping("/{id}/seats/generate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate seating grid (Admin only)", description = "Batch creates rows and seats for a venue section")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> generateSeats(
            @PathVariable Long id,
            @Valid @RequestBody GenerateSeatsRequest request) {
        List<SeatResponse> response = venueService.generateSeats(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.size() + " seats generated successfully"));
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "Get venue physical seats", description = "Returns all configured physical seats for a venue")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getVenueSeats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(venueService.getSeatsByVenueId(id)));
    }
}
