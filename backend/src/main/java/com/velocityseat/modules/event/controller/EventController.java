package com.velocityseat.modules.event.controller;

import com.velocityseat.common.dto.ApiResponse;
import com.velocityseat.modules.event.dto.CreateEventRequest;
import com.velocityseat.modules.event.dto.EventResponse;
import com.velocityseat.modules.event.dto.EventSeatResponse;
import com.velocityseat.modules.event.model.EventCategory;
import com.velocityseat.modules.event.model.EventStatus;
import com.velocityseat.modules.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Events & Inventory", description = "Endpoints for event discovery, management, and seat availability")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create and publish event (Admin only)", description = "Creates an event and generates event seat inventory")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createAndPublishEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Event created and seats published successfully"));
    }

    @GetMapping
    @Operation(summary = "Search events", description = "Filter events by keyword, category, status, and time range")
    public ResponseEntity<ApiResponse<List<EventResponse>>> searchEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTime) {

        List<EventResponse> events = eventService.searchEvents(status, category, keyword, fromTime, toTime);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event details", description = "Retrieves an event by its ID with live seat count")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getEventById(id)));
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "Get all event seats", description = "Fetches the full seat map layout and availability status for an event")
    public ResponseEntity<ApiResponse<List<EventSeatResponse>>> getEventSeats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getEventSeats(id)));
    }

    @GetMapping("/{id}/seats/available")
    @Operation(summary = "Get available event seats only", description = "Returns seats with status AVAILABLE for quick booking view")
    public ResponseEntity<ApiResponse<List<EventSeatResponse>>> getAvailableSeats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getAvailableSeats(id)));
    }
}
