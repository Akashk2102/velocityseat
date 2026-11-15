package com.velocityseat.modules.reservation.dto;

import com.velocityseat.modules.reservation.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private String reservationToken;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private List<Long> seatIds;
    private BigDecimal totalAmount;
    private Instant expiresAt;
    private long expiresInSeconds;
    private ReservationStatus status;
}
