package com.velocityseat.modules.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationPayload implements Serializable {

    private String reservationToken;
    private Long userId;
    private Long eventId;
    private List<Long> seatIds;
    private BigDecimal totalAmount;
    private Instant expiresAt;
}
