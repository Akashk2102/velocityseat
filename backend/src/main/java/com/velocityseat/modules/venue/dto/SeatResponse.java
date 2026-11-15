package com.velocityseat.modules.venue.dto;

import com.velocityseat.modules.venue.model.Seat;
import com.velocityseat.modules.venue.model.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Long id;
    private Long venueId;
    private String section;
    private String rowNumber;
    private String seatNumber;
    private SeatType seatType;

    public static SeatResponse fromEntity(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .venueId(seat.getVenue().getId())
                .section(seat.getSection())
                .rowNumber(seat.getRowNumber())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .build();
    }
}
