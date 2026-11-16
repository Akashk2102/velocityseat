package com.velocityseat.modules.event.service;

import com.velocityseat.common.exception.BadRequestException;
import com.velocityseat.modules.event.dto.CreateEventRequest;
import com.velocityseat.modules.event.dto.EventResponse;
import com.velocityseat.modules.event.model.*;
import com.velocityseat.modules.event.repository.EventRepository;
import com.velocityseat.modules.event.repository.EventSeatRepository;
import com.velocityseat.modules.venue.model.Seat;
import com.velocityseat.modules.venue.model.SeatType;
import com.velocityseat.modules.venue.model.Venue;
import com.velocityseat.modules.venue.repository.SeatRepository;
import com.velocityseat.modules.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private EventService eventService;

    private Venue mockVenue;
    private Seat mockSeat;
    private Event mockEvent;

    @BeforeEach
    void setUp() {
        mockVenue = Venue.builder()
                .id(1L)
                .name("O2 Arena")
                .address("Peninsula Square")
                .city("London")
                .country("UK")
                .totalCapacity(20000)
                .build();

        mockSeat = Seat.builder()
                .id(101L)
                .venue(mockVenue)
                .section("A")
                .rowNumber("1")
                .seatNumber("1")
                .seatType(SeatType.VIP)
                .build();

        mockEvent = Event.builder()
                .id(10L)
                .venue(mockVenue)
                .title("Coldplay Music of the Spheres Tour")
                .category(EventCategory.CONCERT)
                .startTime(Instant.now().plus(7, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(7, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS))
                .status(EventStatus.PUBLISHED)
                .build();
    }

    @Test
    @DisplayName("Should successfully create and publish event with seats generated")
    void createAndPublishEvent_Success() {
        CreateEventRequest request = CreateEventRequest.builder()
                .venueId(1L)
                .title("Coldplay Music of the Spheres Tour")
                .category(EventCategory.CONCERT)
                .startTime(Instant.now().plus(7, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(7, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS))
                .standardPrice(new BigDecimal("99.99"))
                .vipPrice(new BigDecimal("299.99"))
                .build();

        when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
        when(seatRepository.findByVenueIdOrderBySectionAscRowNumberAscSeatNumberAsc(1L)).thenReturn(List.of(mockSeat));
        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

        EventResponse response = eventService.createAndPublishEvent(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Coldplay Music of the Spheres Tour");
        assertThat(response.getTotalSeats()).isEqualTo(1);
        assertThat(response.getAvailableSeats()).isEqualTo(1);
        verify(eventSeatRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw BadRequestException if end time is before start time")
    void createEvent_InvalidTimeRange_ThrowsBadRequest() {
        CreateEventRequest request = CreateEventRequest.builder()
                .venueId(1L)
                .title("Invalid Timing Concert")
                .category(EventCategory.CONCERT)
                .startTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(1, ChronoUnit.DAYS)) // End is before start
                .standardPrice(new BigDecimal("50.00"))
                .vipPrice(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> eventService.createAndPublishEvent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("end time must be after start time");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if venue has no configured physical seats")
    void createEvent_NoPhysicalSeats_ThrowsBadRequest() {
        CreateEventRequest request = CreateEventRequest.builder()
                .venueId(1L)
                .title("Empty Venue Show")
                .category(EventCategory.CONCERT)
                .startTime(Instant.now().plus(7, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(7, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS))
                .standardPrice(new BigDecimal("50.00"))
                .vipPrice(new BigDecimal("100.00"))
                .build();

        when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
        when(seatRepository.findByVenueIdOrderBySectionAscRowNumberAscSeatNumberAsc(1L)).thenReturn(List.of()); // Empty!

        assertThatThrownBy(() -> eventService.createAndPublishEvent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("venue with no configured seats");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully update event fields")
    void updateEvent_Success() {
        com.velocityseat.modules.event.dto.UpdateEventRequest updateReq =
                com.velocityseat.modules.event.dto.UpdateEventRequest.builder()
                        .title("Coldplay Updated Title")
                        .description("Updated description")
                        .build();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);
        when(eventSeatRepository.countByEventId(10L)).thenReturn(100L);
        when(eventSeatRepository.countByEventIdAndStatus(10L, SeatStatus.AVAILABLE)).thenReturn(90L);

        EventResponse updated = eventService.updateEvent(10L, updateReq);

        assertThat(updated).isNotNull();
        assertThat(mockEvent.getTitle()).isEqualTo("Coldplay Updated Title");
        verify(eventRepository, times(1)).save(mockEvent);
    }

    @Test
    @DisplayName("Should mark event as CANCELLED instead of deleting if bookings exist")
    void cancelOrDeleteEvent_WithBookings_MarksCancelled() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
        when(eventSeatRepository.countByEventIdAndStatus(10L, SeatStatus.BOOKED)).thenReturn(5L);

        eventService.cancelOrDeleteEvent(10L);

        assertThat(mockEvent.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository, times(1)).save(mockEvent);
        verify(eventRepository, never()).delete(any());
    }
}
