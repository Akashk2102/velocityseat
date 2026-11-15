package com.velocityseat.modules.notification.producer;

import com.velocityseat.config.KafkaConfig;
import com.velocityseat.modules.notification.event.BookingCancelledEvent;
import com.velocityseat.modules.notification.event.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes booking confirmed event to Kafka.
     * Uses bookingReference as partition key to ensure all events for a booking are ordered.
     */
    public void sendBookingConfirmedEvent(BookingConfirmedEvent event) {
        String topic = KafkaConfig.BOOKING_CONFIRMED_TOPIC;
        String partitionKey = event.getBookingReference();

        log.info("Publishing BookingConfirmedEvent to topic {} with key {}", topic, partitionKey);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, partitionKey, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully delivered BookingConfirmedEvent [{}] to partition {} with offset {}",
                        partitionKey,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to deliver BookingConfirmedEvent [{}] to Kafka: {}",
                        partitionKey, ex.getMessage(), ex);
            }
        });
    }

    /**
     * Publishes booking cancelled event to Kafka.
     */
    public void sendBookingCancelledEvent(BookingCancelledEvent event) {
        String topic = KafkaConfig.BOOKING_CANCELLED_TOPIC;
        String partitionKey = event.getBookingReference();

        log.info("Publishing BookingCancelledEvent to topic {} with key {}", topic, partitionKey);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, partitionKey, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully delivered BookingCancelledEvent [{}] to partition {} with offset {}",
                        partitionKey,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to deliver BookingCancelledEvent [{}] to Kafka: {}",
                        partitionKey, ex.getMessage(), ex);
            }
        });
    }
}
