package com.velocityseat.modules.notification.producer;

import com.velocityseat.config.KafkaConfig;
import com.velocityseat.modules.notification.event.BookingConfirmedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BookingEventProducer producer;

    @Test
    @DisplayName("Should publish BookingConfirmedEvent with bookingReference as partition key")
    void sendBookingConfirmedEvent_Success() {
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingReference("BK-TEST-123")
                .userId(1L)
                .customerEmail("customer@test.com")
                .customerName("Alice")
                .eventId(10L)
                .eventTitle("Concert")
                .venueName("Arena")
                .eventStartTime(Instant.now())
                .seatLabels(List.of("VIP Row 1 Seat 1"))
                .totalAmount(new BigDecimal("100.00"))
                .paymentReference("PAY-123")
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaConfig.BOOKING_CONFIRMED_TOPIC), eq("BK-TEST-123"), eq(event)))
                .thenReturn(future);

        producer.sendBookingConfirmedEvent(event);

        verify(kafkaTemplate, times(1))
                .send(eq(KafkaConfig.BOOKING_CONFIRMED_TOPIC), eq("BK-TEST-123"), eq(event));
    }
}
