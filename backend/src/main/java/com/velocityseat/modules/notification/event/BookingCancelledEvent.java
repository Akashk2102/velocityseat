package com.velocityseat.modules.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent implements Serializable {

    private String bookingReference;
    private Long userId;
    private String customerEmail;
    private String customerName;
    private Long eventId;
    private String eventTitle;
    private BigDecimal refundAmount;
    @Builder.Default
    private Instant cancelledAt = Instant.now();
}
