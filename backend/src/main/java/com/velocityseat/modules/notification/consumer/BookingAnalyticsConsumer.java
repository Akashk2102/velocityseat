package com.velocityseat.modules.notification.consumer;

import com.velocityseat.config.KafkaConfig;
import com.velocityseat.modules.notification.event.BookingConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

@Slf4j
@Component
public class BookingAnalyticsConsumer {

    private final AtomicLong totalTicketsSold = new AtomicLong(0);
    private final DoubleAdder totalGrossRevenue = new DoubleAdder();

    @KafkaListener(
        topics = KafkaConfig.BOOKING_CONFIRMED_TOPIC,
        groupId = "velocityseat-analytics-group"
    )
    public void trackBookingMetrics(BookingConfirmedEvent event) {
        int ticketCount = event.getSeatLabels().size();
        totalTicketsSold.addAndGet(ticketCount);
        totalGrossRevenue.add(event.getTotalAmount().doubleValue());

        log.info("[ANALYTICS CONSUMER] Real-time metrics updated: +{} seats sold for '${}'. Cumulative Seats: {}, Cumulative Revenue: ${}",
                ticketCount,
                event.getEventTitle(),
                totalTicketsSold.get(),
                String.format("%.2f", totalGrossRevenue.doubleValue()));
    }

    public long getTotalTicketsSold() {
        return totalTicketsSold.get();
    }

    public double getTotalGrossRevenue() {
        return totalGrossRevenue.doubleValue();
    }
}
