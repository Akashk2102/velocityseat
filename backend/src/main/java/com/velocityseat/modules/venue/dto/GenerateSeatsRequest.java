package com.velocityseat.modules.venue.dto;

import com.velocityseat.modules.venue.model.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSeatsRequest {

    @NotBlank(message = "Section name is required")
    private String section;

    @NotNull(message = "Number of rows is required")
    @Min(value = 1, message = "Rows must be at least 1")
    private Integer totalRows;

    @NotNull(message = "Seats per row is required")
    @Min(value = 1, message = "Seats per row must be at least 1")
    private Integer seatsPerRow;

    @Builder.Default
    private SeatType seatType = SeatType.STANDARD;
}
