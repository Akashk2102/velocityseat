package com.velocityseat.modules.reservation.service;

import com.velocityseat.common.exception.BadRequestException;
import com.velocityseat.common.exception.ConflictException;
import com.velocityseat.common.exception.ResourceNotFoundException;
import com.velocityseat.modules.event.model.Event;
import com.velocityseat.modules.event.model.EventSeat;
import com.velocityseat.modules.event.model.EventStatus;
import com.velocityseat.modules.event.model.SeatStatus;
import com.velocityseat.modules.event.repository.EventRepository;
import com.velocityseat.modules.event.repository.EventSeatRepository;
import com.velocityseat.modules.reservation.dto.HoldSeatsRequest;
import com.velocityseat.modules.reservation.dto.ReservationPayload;
import com.velocityseat.modules.reservation.dto.ReservationResponse;
import com.velocityseat.modules.reservation.model.ReservationStatus;
import com.velocityseat.modules.reservation.model.SeatReservation;
import com.velocityseat.modules.reservation.repository.SeatReservationRepository;
import com.velocityseat.modules.user.model.User;
import com.velocityseat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final RedisSeatLockService redisSeatLockService;
    private final EventSeatRepository eventSeatRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SeatReservationRepository seatReservationRepository;

    private static final long HOLD_TTL_SECONDS = 600L; // 10 minutes hold

    @Transactional
    public ReservationResponse holdSeats(String userEmail, HoldSeatsRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", request.getEventId()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BadRequestException("Event is not currently open for bookings");
        }

        // Fetch target event seats
        List<EventSeat> requestedSeats = eventSeatRepository.findAllById(request.getSeatIds());
        if (requestedSeats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("One or more event seat IDs do not exist");
        }

        // Verify seats belong to the requested event and are available
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (EventSeat seat : requestedSeats) {
            if (!seat.getEvent().getId().equals(event.getId())) {
                throw new BadRequestException("Seat ID " + seat.getId() + " does not belong to event " + event.getId());
            }

            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new ConflictException("Seat " + seat.getSeat().getSection() + "-" +
                        seat.getSeat().getRowNumber() + "-" + seat.getSeat().getSeatNumber() + " is already booked.");
            }

            if (seat.getStatus() == SeatStatus.HELD) {
                // Check if the hold is still active in Redis
                boolean isHeldInCache = redisSeatLockService.isSeatHeldInRedis(event.getId(), seat.getId());
                if (isHeldInCache) {
                    throw new ConflictException("Seat " + seat.getSeat().getSection() + "-" +
                            seat.getSeat().getRowNumber() + "-" + seat.getSeat().getSeatNumber() + " is currently held by another user.");
                } else {
                    // Lazy reconciliation: Redis key expired, so reclaim seat to AVAILABLE
                    seat.setStatus(SeatStatus.AVAILABLE);
                }
            }

            totalAmount = totalAmount.add(seat.getPrice());
        }

        // 1. Generate unique reservation token
        String reservationToken = "res_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(HOLD_TTL_SECONDS);

        // 2. ATOMIC REDIS LOCK: Acquire locks for all requested seats
        boolean locksAcquired = redisSeatLockService.acquireSeatHolds(
                event.getId(),
                request.getSeatIds(),
                reservationToken,
                HOLD_TTL_SECONDS
        );

        if (!locksAcquired) {
            throw new ConflictException("One or more selected seats were just reserved by another customer. Please select different seats.");
        }

        // 3. Mark seats as HELD in database
        for (EventSeat seat : requestedSeats) {
            seat.setStatus(SeatStatus.HELD);
        }
        eventSeatRepository.saveAll(requestedSeats);

        // 4. Save DB audit record (persisting seat IDs directly in PostgreSQL)
        SeatReservation reservation = SeatReservation.builder()
                .reservationToken(reservationToken)
                .user(user)
                .event(event)
                .seatIds(request.getSeatIds())
                .expiresAt(expiresAt)
                .status(ReservationStatus.ACTIVE)
                .build();
        seatReservationRepository.save(reservation);

        // 5. Cache reservation details in Redis for fast checkout lookup
        ReservationPayload payload = ReservationPayload.builder()
                .reservationToken(reservationToken)
                .userId(user.getId())
                .eventId(event.getId())
                .seatIds(request.getSeatIds())
                .totalAmount(totalAmount)
                .expiresAt(expiresAt)
                .build();
        redisSeatLockService.saveReservationPayload(reservationToken, payload, HOLD_TTL_SECONDS);

        return ReservationResponse.builder()
                .reservationToken(reservationToken)
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .userId(user.getId())
                .seatIds(request.getSeatIds())
                .totalAmount(totalAmount)
                .expiresAt(expiresAt)
                .expiresInSeconds(HOLD_TTL_SECONDS)
                .status(ReservationStatus.ACTIVE)
                .build();
    }

    @Transactional
    public void releaseReservation(String userEmail, String reservationToken) {
        SeatReservation reservation = seatReservationRepository.findByReservationToken(reservationToken)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "token", reservationToken));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to release this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            return; // Already resolved
        }

        ReservationPayload payload = redisSeatLockService.getReservationPayload(reservationToken);
        List<Long> seatIds = (payload != null) ? payload.getSeatIds() : List.of();

        // Release Redis locks
        redisSeatLockService.releaseSeatHolds(reservation.getEvent().getId(), seatIds, reservationToken);

        // Revert DB seats to AVAILABLE
        if (!seatIds.isEmpty()) {
            List<EventSeat> seats = eventSeatRepository.findAllById(seatIds);
            for (EventSeat s : seats) {
                if (s.getStatus() == SeatStatus.HELD) {
                    s.setStatus(SeatStatus.AVAILABLE);
                }
            }
            eventSeatRepository.saveAll(seats);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        seatReservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(String reservationToken) {
        SeatReservation reservation = seatReservationRepository.findByReservationToken(reservationToken)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "token", reservationToken));

        ReservationPayload payload = redisSeatLockService.getReservationPayload(reservationToken);
        long remainingSeconds = Math.max(0, Duration.between(Instant.now(), reservation.getExpiresAt()).getSeconds());

        List<Long> seatIds = payload != null ? payload.getSeatIds() : List.of();
        BigDecimal total = payload != null ? payload.getTotalAmount() : BigDecimal.ZERO;

        return ReservationResponse.builder()
                .reservationToken(reservation.getReservationToken())
                .eventId(reservation.getEvent().getId())
                .eventTitle(reservation.getEvent().getTitle())
                .userId(reservation.getUser().getId())
                .seatIds(seatIds)
                .totalAmount(total)
                .expiresAt(reservation.getExpiresAt())
                .expiresInSeconds(remainingSeconds)
                .status(reservation.getStatus())
                .build();
    }

    /**
     * Background cleaner scheduled every 60 seconds to reconcile expired holds in PostgreSQL
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredReservations() {
        Instant now = Instant.now();
        List<SeatReservation> expiredReservations = seatReservationRepository.findExpiredReservations(
                ReservationStatus.ACTIVE, now);

        if (!expiredReservations.isEmpty()) {
            log.info("Found {} expired reservations to reconcile in database", expiredReservations.size());
            for (SeatReservation res : expiredReservations) {
                res.setStatus(ReservationStatus.EXPIRED);

                // Read the exact seat IDs directly from the database record!
                List<Long> seatIds = res.getSeatIds();
                if (!seatIds.isEmpty()) {
                    List<EventSeat> heldSeats = eventSeatRepository.findAllById(seatIds);
                    for (EventSeat s : heldSeats) {
                        if (s.getStatus() == SeatStatus.HELD) {
                            s.setStatus(SeatStatus.AVAILABLE);
                        }
                    }
                    eventSeatRepository.saveAll(heldSeats);
                }
            }
            seatReservationRepository.saveAll(expiredReservations);
        }
    }
}
