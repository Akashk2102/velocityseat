package com.velocityseat.modules.event.service;

import com.velocityseat.common.exception.BadRequestException;
import com.velocityseat.common.exception.ResourceNotFoundException;
import com.velocityseat.modules.event.dto.CreateEventRequest;
import com.velocityseat.modules.event.dto.EventResponse;
import com.velocityseat.modules.event.dto.EventSeatResponse;
import com.velocityseat.modules.event.dto.UpdateEventRequest;
import com.velocityseat.modules.event.model.*;
import com.velocityseat.modules.event.repository.EventRepository;
import com.velocityseat.modules.event.repository.EventSeatRepository;
import com.velocityseat.modules.venue.model.Seat;
import com.velocityseat.modules.venue.model.SeatType;
import com.velocityseat.modules.venue.model.Venue;
import com.velocityseat.modules.venue.repository.SeatRepository;
import com.velocityseat.modules.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public EventResponse createAndPublishEvent(CreateEventRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Event end time must be after start time");
        }

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.getVenueId()));

        List<Seat> physicalSeats = seatRepository.findByVenueIdOrderBySectionAscRowNumberAscSeatNumberAsc(venue.getId());
        if (physicalSeats.isEmpty()) {
            throw new BadRequestException("Cannot create an event for a venue with no configured seats. Please generate seats first.");
        }

        Event event = Event.builder()
                .venue(venue)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(EventStatus.PUBLISHED)
                .build();

        Event savedEvent = eventRepository.save(event);

        // Populate EventSeat inventory based on physical seating and pricing tiers
        BigDecimal accessiblePrice = request.getAccessiblePrice() != null
                ? request.getAccessiblePrice()
                : request.getStandardPrice();

        List<EventSeat> eventSeats = new ArrayList<>();
        for (Seat seat : physicalSeats) {
            BigDecimal price = switch (seat.getSeatType()) {
                case VIP -> request.getVipPrice();
                case ACCESSIBLE -> accessiblePrice;
                case STANDARD -> request.getStandardPrice();
            };

            eventSeats.add(EventSeat.builder()
                    .event(savedEvent)
                    .seat(seat)
                    .price(price)
                    .status(SeatStatus.AVAILABLE)
                    .version(0)
                    .build());
        }

        eventSeatRepository.saveAll(eventSeats);

        long totalSeats = eventSeats.size();
        return EventResponse.fromEntity(savedEvent, totalSeats, totalSeats);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        long total = eventSeatRepository.countByEventId(eventId);
        long available = eventSeatRepository.countByEventIdAndStatus(eventId, SeatStatus.AVAILABLE);

        return EventResponse.fromEntity(event, total, available);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> searchEvents(
            EventStatus status,
            EventCategory category,
            String keyword,
            Instant fromTime,
            Instant toTime) {

        List<Event> events = eventRepository.searchEvents(status, category, keyword, fromTime, toTime);
        return events.stream().map(e -> {
            long total = eventSeatRepository.countByEventId(e.getId());
            long available = eventSeatRepository.countByEventIdAndStatus(e.getId(), SeatStatus.AVAILABLE);
            return EventResponse.fromEntity(e, total, available);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<EventSeatResponse> getEventSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }
        return eventSeatRepository.findByEventId(eventId)
                .stream()
                .map(EventSeatResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventSeatResponse> getAvailableSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }
        return eventSeatRepository.findByEventIdAndStatus(eventId, SeatStatus.AVAILABLE)
                .stream()
                .map(EventSeatResponse::fromEntity)
                .toList();
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null) {
            event.setCategory(request.getCategory());
        }
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            if (request.getStartTime() != null && !request.getEndTime().isAfter(request.getStartTime())) {
                throw new BadRequestException("End time must be after start time");
            }
            event.setEndTime(request.getEndTime());
        }

        Event updated = eventRepository.save(event);
        long total = eventSeatRepository.countByEventId(eventId);
        long available = eventSeatRepository.countByEventIdAndStatus(eventId, SeatStatus.AVAILABLE);
        return EventResponse.fromEntity(updated, total, available);
    }

    @Transactional
    public void cancelOrDeleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        long bookedSeats = eventSeatRepository.countByEventIdAndStatus(eventId, SeatStatus.BOOKED);
        if (bookedSeats > 0) {
            // Cannot hard delete an event with active bookings; mark status as CANCELLED
            event.setStatus(EventStatus.CANCELLED);
            eventRepository.save(event);
        } else {
            // Safe to delete if no bookings exist
            eventRepository.delete(event);
        }
    }
}
