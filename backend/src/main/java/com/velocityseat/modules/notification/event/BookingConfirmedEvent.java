package com.velocityseat.modules.notification.event;

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
public class BookingConfirmedEvent implements Serializable {

    private String bookingReference;
    private Long userId;
    private String customerEmail;
    private String customerName;
    private Long eventId;
    private String eventTitle;
    private String venueName;
    private Instant eventStartTime;
    private List<String> seatLabels;
    private BigDecimal totalAmount;
    private String paymentReference;
    @Builder.Default
    private Instant confirmedAt = Instant.now();
}
