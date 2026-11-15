package com.velocityseat.modules.booking.service;

import com.velocityseat.common.exception.BadRequestException;
import com.velocityseat.common.exception.ConflictException;
import com.velocityseat.common.exception.ResourceNotFoundException;
import com.velocityseat.modules.booking.dto.BookingResponse;
import com.velocityseat.modules.booking.dto.ConfirmBookingRequest;
import com.velocityseat.modules.booking.model.*;
import com.velocityseat.modules.booking.repository.BookingRepository;
import com.velocityseat.modules.event.model.EventSeat;
import com.velocityseat.modules.event.model.SeatStatus;
import com.velocityseat.modules.event.repository.EventSeatRepository;
import com.velocityseat.modules.notification.event.BookingCancelledEvent;
import com.velocityseat.modules.notification.event.BookingConfirmedEvent;
import com.velocityseat.modules.notification.producer.BookingEventProducer;
import com.velocityseat.modules.reservation.dto.ReservationPayload;
import com.velocityseat.modules.reservation.model.ReservationStatus;
import com.velocityseat.modules.reservation.model.SeatReservation;
import com.velocityseat.modules.reservation.repository.SeatReservationRepository;
import com.velocityseat.modules.reservation.service.RedisSeatLockService;
import com.velocityseat.modules.user.model.Role;
import com.velocityseat.modules.user.model.User;
import com.velocityseat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventSeatRepository eventSeatRepository;
    private final UserRepository userRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final RedisSeatLockService redisSeatLockService;
    private final BookingEventProducer bookingEventProducer;

    @Transactional
    public BookingResponse confirmBooking(String userEmail, String idempotencyKey, ConfirmBookingRequest request) {
        // 1. IDEMPOTENCY CHECK: Return existing booking if request already succeeded
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Booking> existingBooking = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existingBooking.isPresent()) {
                log.info("Idempotent request detected for key: {}. Returning existing booking.", idempotencyKey);
                return BookingResponse.fromEntity(existingBooking.get());
            }
        }

        // 2. Validate user identity
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        // 3. Validate reservation token in database & cache
        SeatReservation reservation = seatReservationRepository.findByReservationToken(request.getReservationToken())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "token", request.getReservationToken()));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("This reservation does not belong to your account");
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new BadRequestException("This reservation has already been confirmed into a booking");
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE || reservation.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reservation has expired. Please select and hold your seats again.");
        }

        ReservationPayload payload = redisSeatLockService.getReservationPayload(request.getReservationToken());
        List<Long> seatIds = (payload != null) ? payload.getSeatIds() : List.of();
        if (seatIds.isEmpty()) {
            throw new BadRequestException("No active seat hold found in cache for token: " + request.getReservationToken());
        }

        // 4. PESSIMISTIC WRITE LOCK on PostgreSQL: SELECT ... FOR UPDATE
        List<EventSeat> lockedSeats = eventSeatRepository.findAllByIdWithLock(seatIds);
        if (lockedSeats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more event seats could not be located in database");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (EventSeat seat : lockedSeats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new ConflictException("Seat " + seat.getSeat().getSection() + "-" +
                        seat.getSeat().getRowNumber() + "-" + seat.getSeat().getSeatNumber() +
                        " is already permanently booked.");
            }
            totalAmount = totalAmount.add(seat.getPrice());
        }

        // 5. Update locked seats to permanently BOOKED
        for (EventSeat seat : lockedSeats) {
            seat.setStatus(SeatStatus.BOOKED);
        }
        eventSeatRepository.saveAll(lockedSeats);

        // 6. Create Booking & Items
        String bookingReference = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .user(user)
                .event(reservation.getEvent())
                .totalAmount(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .idempotencyKey(idempotencyKey)
                .build();

        List<BookingItem> items = new ArrayList<>();
        for (EventSeat seat : lockedSeats) {
            items.add(BookingItem.builder()
                    .booking(booking)
                    .eventSeat(seat)
                    .unitPrice(seat.getPrice())
                    .build());
        }
        booking.setItems(items);

        // 7. Record Successful Payment
        String paymentReference = "PAY-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentReference(paymentReference)
                .amount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.SUCCESS)
                .build();
        booking.setPayments(List.of(payment));

        Booking savedBooking = bookingRepository.save(booking);

        // 8. Update Reservation status & clean up Redis temporary hold
        reservation.setStatus(ReservationStatus.CONFIRMED);
        seatReservationRepository.save(reservation);
        redisSeatLockService.releaseSeatHolds(reservation.getEvent().getId(), seatIds, request.getReservationToken());

        // 9. ASYNCHRONOUS KAFKA STREAMING: Dispatch event to Kafka
        List<String> seatLabels = lockedSeats.stream()
                .map(s -> s.getSeat().getSection() + " Row " + s.getSeat().getRowNumber() + " Seat " + s.getSeat().getSeatNumber())
                .toList();

        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingReference(bookingReference)
                .userId(user.getId())
                .customerEmail(user.getEmail())
                .customerName(user.getFullName())
                .eventId(reservation.getEvent().getId())
                .eventTitle(reservation.getEvent().getTitle())
                .venueName(reservation.getEvent().getVenue().getName())
                .eventStartTime(reservation.getEvent().getStartTime())
                .seatLabels(seatLabels)
                .totalAmount(totalAmount)
                .paymentReference(paymentReference)
                .confirmedAt(Instant.now())
                .build();

        bookingEventProducer.sendBookingConfirmedEvent(event);

        log.info("Booking confirmed successfully. Reference: {}, Amount: ${}", bookingReference, totalAmount);
        return BookingResponse.fromEntity(savedBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String userEmail, String bookingReference) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ROLE_ADMIN) {
            throw new BadRequestException("You are not authorized to view this booking");
        }

        return BookingResponse.fromEntity(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return bookingRepository.findByUserIdWithDetails(user.getId()).stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void cancelBooking(String userEmail, String bookingReference) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ROLE_ADMIN) {
            throw new BadRequestException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking is already " + booking.getStatus());
        }

        if (booking.getEvent().getStartTime().isBefore(Instant.now())) {
            throw new BadRequestException("Cannot cancel booking for an event that has already started");
        }

        // 1. Release booked seats back to AVAILABLE
        List<EventSeat> seatsToRelease = new ArrayList<>();
        for (BookingItem item : booking.getItems()) {
            EventSeat es = item.getEventSeat();
            es.setStatus(SeatStatus.AVAILABLE);
            seatsToRelease.add(es);
        }
        eventSeatRepository.saveAll(seatsToRelease);

        // 2. Mark booking as CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);

        // 3. Mark payment as REFUNDED
        for (Payment p : booking.getPayments()) {
            p.setStatus(PaymentStatus.REFUNDED);
        }
        bookingRepository.save(booking);

        // 4. ASYNCHRONOUS KAFKA STREAMING: Dispatch cancellation event
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingReference(bookingReference)
                .userId(user.getId())
                .customerEmail(user.getEmail())
                .customerName(user.getFullName())
                .eventId(booking.getEvent().getId())
                .eventTitle(booking.getEvent().getTitle())
                .refundAmount(booking.getTotalAmount())
                .cancelledAt(Instant.now())
                .build();
        bookingEventProducer.sendBookingCancelledEvent(event);

        log.info("Booking {} successfully cancelled and seats released", bookingReference);
    }
}
