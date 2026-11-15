package com.velocityseat.modules.notification.consumer;

import com.velocityseat.modules.notification.event.BookingCancelledEvent;
import com.velocityseat.modules.notification.event.BookingConfirmedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TicketNotificationConsumerTest {

    private final TicketNotificationConsumer consumer = new TicketNotificationConsumer();

    @Test
    @DisplayName("Should successfully consume BookingConfirmedEvent without error")
    void handleBookingConfirmed_Success() {
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingReference("BK-CONFIRM-99")
                .userId(1L)
                .customerEmail("alex@velocityseat.com")
                .customerName("Alex")
                .eventId(10L)
                .eventTitle("Championship Final")
                .venueName("Stadium")
                .eventStartTime(Instant.now())
                .seatLabels(List.of("Section 101 Row A Seat 10"))
                .totalAmount(new BigDecimal("150.00"))
                .paymentReference("PAY-999")
                .build();

        assertDoesNotThrow(() -> consumer.handleBookingConfirmed(event));
    }

    @Test
    @DisplayName("Should successfully consume BookingCancelledEvent without error")
    void handleBookingCancelled_Success() {
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingReference("BK-CANCEL-99")
                .userId(1L)
                .customerEmail("alex@velocityseat.com")
                .customerName("Alex")
                .eventId(10L)
                .eventTitle("Championship Final")
                .refundAmount(new BigDecimal("150.00"))
                .build();

        assertDoesNotThrow(() -> consumer.handleBookingCancelled(event));
    }
}
