package com.velocityseat.modules.booking.dto;

import com.velocityseat.modules.booking.model.Booking;
import com.velocityseat.modules.booking.model.BookingStatus;
import com.velocityseat.modules.booking.model.Payment;
import com.velocityseat.modules.booking.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private String bookingReference;
    private Long eventId;
    private String eventTitle;
    private String venueName;
    private String venueAddress;
    private Instant eventStartTime;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private String paymentReference;
    private PaymentMethod paymentMethod;
    private List<TicketItemResponse> tickets;
    private Instant createdAt;

    public static BookingResponse fromEntity(Booking booking) {
        Payment latestPayment = booking.getPayments().isEmpty() ? null : booking.getPayments().getFirst();

        List<TicketItemResponse> ticketResponses = booking.getItems().stream()
                .map(TicketItemResponse::fromEntity)
                .toList();

        return BookingResponse.builder()
                .bookingReference(booking.getBookingReference())
                .eventId(booking.getEvent().getId())
                .eventTitle(booking.getEvent().getTitle())
                .venueName(booking.getEvent().getVenue().getName())
                .venueAddress(booking.getEvent().getVenue().getAddress() + ", " + booking.getEvent().getVenue().getCity())
                .eventStartTime(booking.getEvent().getStartTime())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .paymentReference(latestPayment != null ? latestPayment.getPaymentReference() : null)
                .paymentMethod(latestPayment != null ? latestPayment.getPaymentMethod() : null)
                .tickets(ticketResponses)
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
