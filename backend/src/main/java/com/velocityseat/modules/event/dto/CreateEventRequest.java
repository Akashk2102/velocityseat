package com.velocityseat.modules.event.dto;

import com.velocityseat.modules.event.model.EventCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotNull(message = "Venue ID is required")
    private Long venueId;

    @NotBlank(message = "Event title is required")
    private String title;

    private String description;

    @NotNull(message = "Category is required")
    private EventCategory category;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private Instant startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private Instant endTime;

    @NotNull(message = "Standard seat price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Standard price must be greater than zero")
    private BigDecimal standardPrice;

    @NotNull(message = "VIP seat price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "VIP price must be greater than zero")
    private BigDecimal vipPrice;

    private BigDecimal accessiblePrice;
}
