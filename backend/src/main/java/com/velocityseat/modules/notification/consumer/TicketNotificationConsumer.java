package com.velocityseat.modules.notification.consumer;

import com.velocityseat.config.KafkaConfig;
import com.velocityseat.modules.notification.event.BookingCancelledEvent;
import com.velocityseat.modules.notification.event.BookingConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicketNotificationConsumer {

    @KafkaListener(
        topics = KafkaConfig.BOOKING_CONFIRMED_TOPIC,
        groupId = "velocityseat-ticket-dispatch-group"
    )
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("=================================================================");
        log.info("[TICKET NOTIFICATION CONSUMER] Processing booking confirmation:");
        log.info("  Booking Ref  : {}", event.getBookingReference());
        log.info("  Customer     : {} <{}>", event.getCustomerName(), event.getCustomerEmail());
        log.info("  Event        : {} @ {}", event.getEventTitle(), event.getVenueName());
        log.info("  Start Time   : {}", event.getEventStartTime());
        log.info("  Seats Booked : {}", String.join(", ", event.getSeatLabels()));
        log.info("  Total Amount : ${}", event.getTotalAmount());
        log.info("  Payment Ref  : {}", event.getPaymentReference());
        log.info("  ACTION       : Dispatched PDF Ticket & QR Code to {}", event.getCustomerEmail());
        log.info("=================================================================");
    }

    @KafkaListener(
        topics = KafkaConfig.BOOKING_CANCELLED_TOPIC,
        groupId = "velocityseat-ticket-dispatch-group"
    )
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("=================================================================");
        log.info("[TICKET NOTIFICATION CONSUMER] Processing booking cancellation:");
        log.info("  Booking Ref  : {}", event.getBookingReference());
        log.info("  Customer     : {} <{}>", event.getCustomerName(), event.getCustomerEmail());
        log.info("  Event        : {}", event.getEventTitle());
        log.info("  Refund Amount: ${}", event.getRefundAmount());
        log.info("  ACTION       : Sent cancellation & refund notice email to {}", event.getCustomerEmail());
        log.info("=================================================================");
    }
}
