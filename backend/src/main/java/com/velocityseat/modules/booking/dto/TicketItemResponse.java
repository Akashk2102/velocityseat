package com.velocityseat.modules.booking.dto;

import com.velocityseat.modules.booking.model.BookingItem;
import com.velocityseat.modules.venue.model.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketItemResponse {

    private Long eventSeatId;
    private String section;
    private String rowNumber;
    private String seatNumber;
    private SeatType seatType;
    private BigDecimal price;

    public static TicketItemResponse fromEntity(BookingItem item) {
        return TicketItemResponse.builder()
                .eventSeatId(item.getEventSeat().getId())
                .section(item.getEventSeat().getSeat().getSection())
                .rowNumber(item.getEventSeat().getSeat().getRowNumber())
                .seatNumber(item.getEventSeat().getSeat().getSeatNumber())
                .seatType(item.getEventSeat().getSeat().getSeatType())
                .price(item.getUnitPrice())
                .build();
    }
}
