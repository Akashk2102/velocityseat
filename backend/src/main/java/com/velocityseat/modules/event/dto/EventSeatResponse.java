package com.velocityseat.modules.event.dto;

import com.velocityseat.modules.event.model.EventSeat;
import com.velocityseat.modules.event.model.SeatStatus;
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
public class EventSeatResponse {

    private Long id;
    private Long eventId;
    private Long seatId;
    private String section;
    private String rowNumber;
    private String seatNumber;
    private SeatType seatType;
    private BigDecimal price;
    private SeatStatus status;
    private Integer version;

    public static EventSeatResponse fromEntity(EventSeat eventSeat) {
        return EventSeatResponse.builder()
                .id(eventSeat.getId())
                .eventId(eventSeat.getEvent().getId())
                .seatId(eventSeat.getSeat().getId())
                .section(eventSeat.getSeat().getSection())
                .rowNumber(eventSeat.getSeat().getRowNumber())
                .seatNumber(eventSeat.getSeat().getSeatNumber())
                .seatType(eventSeat.getSeat().getSeatType())
                .price(eventSeat.getPrice())
                .status(eventSeat.getStatus())
                .version(eventSeat.getVersion())
                .build();
    }
}
