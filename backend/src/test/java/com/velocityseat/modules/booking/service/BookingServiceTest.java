package com.velocityseat.modules.booking.service;

import com.velocityseat.common.exception.BadRequestException;
import com.velocityseat.modules.booking.dto.BookingResponse;
import com.velocityseat.modules.booking.dto.ConfirmBookingRequest;
import com.velocityseat.modules.booking.model.Booking;
import com.velocityseat.modules.booking.model.BookingStatus;
import com.velocityseat.modules.booking.model.PaymentMethod;
import com.velocityseat.modules.booking.repository.BookingRepository;
import com.velocityseat.modules.event.model.*;
import com.velocityseat.modules.event.repository.EventSeatRepository;
import com.velocityseat.modules.reservation.dto.ReservationPayload;
import com.velocityseat.modules.reservation.model.ReservationStatus;
import com.velocityseat.modules.reservation.model.SeatReservation;
import com.velocityseat.modules.reservation.repository.SeatReservationRepository;
import com.velocityseat.modules.reservation.service.RedisSeatLockService;
import com.velocityseat.modules.user.model.Role;
import com.velocityseat.modules.user.model.User;
import com.velocityseat.modules.user.repository.UserRepository;
import com.velocityseat.modules.venue.model.Seat;
import com.velocityseat.modules.venue.model.Venue;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @Mock
    private RedisSeatLockService redisSeatLockService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    private User mockUser;
    private Event mockEvent;
    private EventSeat mockEventSeat;
    private SeatReservation mockReservation;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("david@velocityseat.com")
                .role(Role.ROLE_CUSTOMER)
                .build();

        Venue venue = Venue.builder().id(1L).name("Madison Square Garden").address("4 Penn Plaza").city("NYC").build();
        mockEvent = Event.builder()
                .id(10L)
                .venue(venue)
                .title("Coldplay Music of the Spheres")
                .startTime(Instant.now().plus(7, ChronoUnit.DAYS))
                .status(EventStatus.PUBLISHED)
                .build();

        Seat seat = Seat.builder().id(101L).venue(venue).section("VIP").rowNumber("1").seatNumber("1").build();

        mockEventSeat = EventSeat.builder()
                .id(501L)
                .event(mockEvent)
                .seat(seat)
                .price(new BigDecimal("250.00"))
                .status(SeatStatus.HELD)
                .version(0)
                .build();

        mockReservation = SeatReservation.builder()
                .id(1L)
                .reservationToken("res_valid_token")
                .user(mockUser)
                .event(mockEvent)
                .status(ReservationStatus.ACTIVE)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
    }

    @Test
    @DisplayName("Should successfully confirm booking, lock rows, record payment, and release Redis hold")
    void confirmBooking_Success() {
        ConfirmBookingRequest request = ConfirmBookingRequest.builder()
                .reservationToken("res_valid_token")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(bookingRepository.findByIdempotencyKey("idem-key-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("david@velocityseat.com")).thenReturn(Optional.of(mockUser));
        when(seatReservationRepository.findByReservationToken("res_valid_token")).thenReturn(Optional.of(mockReservation));
        when(redisSeatLockService.getReservationPayload("res_valid_token")).thenReturn(
                ReservationPayload.builder().seatIds(List.of(501L)).build()
        );
        when(eventSeatRepository.findAllByIdWithLock(List.of(501L))).thenReturn(List.of(mockEventSeat));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(99L);
            b.setCreatedAt(Instant.now());
            return b;
        });

        BookingResponse response = bookingService.confirmBooking("david@velocityseat.com", "idem-key-123", request);

        assertThat(response).isNotNull();
        assertThat(response.getBookingReference()).startsWith("BK-");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("250.00");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(mockEventSeat.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(mockReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        verify(eventSeatRepository, times(1)).saveAll(anyList());
        verify(redisSeatLockService, times(1)).releaseSeatHolds(eq(10L), eq(List.of(501L)), eq("res_valid_token"));
        verify(eventPublisher, times(1)).publishEvent(any(com.velocityseat.modules.booking.event.BookingConfirmedInternalEvent.class));
    }

    @Test
    @DisplayName("Should return existing cached booking without duplicate processing on identical Idempotency-Key")
    void confirmBooking_IdempotencyKeyReturnsCachedResponse() {
        Booking existingBooking = Booking.builder()
                .id(100L)
                .bookingReference("BK-EXISTING-99")
                .user(mockUser)
                .event(mockEvent)
                .totalAmount(new BigDecimal("250.00"))
                .status(BookingStatus.CONFIRMED)
                .idempotencyKey("idem-key-duplicate")
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        when(bookingRepository.findByIdempotencyKey("idem-key-duplicate")).thenReturn(Optional.of(existingBooking));

        ConfirmBookingRequest request = ConfirmBookingRequest.builder()
                .reservationToken("res_already_confirmed")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        BookingResponse response = bookingService.confirmBooking("david@velocityseat.com", "idem-key-duplicate", request);

        assertThat(response).isNotNull();
        assertThat(response.getBookingReference()).isEqualTo("BK-EXISTING-99");

        // Verify no second DB write or seat locking was executed
        verify(eventSeatRepository, never()).findAllByIdWithLock(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if temporary reservation has expired")
    void confirmBooking_ExpiredReservation_ThrowsBadRequest() {
        mockReservation.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)); // Expired!

        ConfirmBookingRequest request = ConfirmBookingRequest.builder()
                .reservationToken("res_valid_token")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(bookingRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("david@velocityseat.com")).thenReturn(Optional.of(mockUser));
        when(seatReservationRepository.findByReservationToken("res_valid_token")).thenReturn(Optional.of(mockReservation));

        assertThatThrownBy(() -> bookingService.confirmBooking("david@velocityseat.com", "key-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reservation has expired");

        verify(eventSeatRepository, never()).findAllByIdWithLock(any());
    }
}
