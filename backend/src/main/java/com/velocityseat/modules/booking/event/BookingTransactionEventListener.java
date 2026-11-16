package com.velocityseat.modules.booking.event;

import com.velocityseat.modules.notification.producer.BookingEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingTransactionEventListener {

    private final BookingEventProducer bookingEventProducer;

    /**
     * Dispatches the Kafka event strictly AFTER the database transaction commits to PostgreSQL.
     * If the DB transaction rolls back, this listener will NOT execute.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedInternalEvent internalEvent) {
        log.info("DB transaction committed successfully. Dispatching BookingConfirmedEvent to Kafka for reference: {}",
                internalEvent.getEvent().getBookingReference());
        bookingEventProducer.sendBookingConfirmedEvent(internalEvent.getEvent());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledInternalEvent internalEvent) {
        log.info("DB transaction committed successfully. Dispatching BookingCancelledEvent to Kafka for reference: {}",
                internalEvent.getEvent().getBookingReference());
        bookingEventProducer.sendBookingCancelledEvent(internalEvent.getEvent());
    }
}
