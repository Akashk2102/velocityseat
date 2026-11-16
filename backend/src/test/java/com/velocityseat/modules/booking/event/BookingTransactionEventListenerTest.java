package com.velocityseat.modules.booking.event;

import com.velocityseat.modules.notification.event.BookingCancelledEvent;
import com.velocityseat.modules.notification.event.BookingConfirmedEvent;
import com.velocityseat.modules.notification.producer.BookingEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingTransactionEventListenerTest {

    @Mock
    private BookingEventProducer bookingEventProducer;

    @InjectMocks
    private BookingTransactionEventListener eventListener;

    @Test
    @DisplayName("Should forward event to Kafka after commit")
    void onBookingConfirmed_Success() {
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingReference("BK-999")
                .userId(1L)
                .customerEmail("user@test.com")
                .customerName("User")
                .eventId(10L)
                .eventTitle("Show")
                .seatLabels(List.of("A-1-1"))
                .totalAmount(new BigDecimal("100.00"))
                .paymentReference("PAY-1")
                .confirmedAt(Instant.now())
                .build();

        eventListener.onBookingConfirmed(new BookingConfirmedInternalEvent(event));

        verify(bookingEventProducer, times(1)).sendBookingConfirmedEvent(event);
    }

    @Test
    @DisplayName("Should forward cancellation event to Kafka after commit")
    void onBookingCancelled_Success() {
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingReference("BK-999")
                .userId(1L)
                .customerEmail("user@test.com")
                .customerName("User")
                .eventId(10L)
                .eventTitle("Show")
                .refundAmount(new BigDecimal("100.00"))
                .cancelledAt(Instant.now())
                .build();

        eventListener.onBookingCancelled(new BookingCancelledInternalEvent(event));

        verify(bookingEventProducer, times(1)).sendBookingCancelledEvent(event);
    }
}
