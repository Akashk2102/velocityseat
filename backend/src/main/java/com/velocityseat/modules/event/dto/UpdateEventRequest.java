package com.velocityseat.modules.event.dto;

import com.velocityseat.modules.event.model.EventCategory;
import com.velocityseat.modules.event.model.EventStatus;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    private String title;
    private String description;
    private EventCategory category;
    @Future(message = "Start time must be in the future")
    private Instant startTime;
    @Future(message = "End time must be in the future")
    private Instant endTime;
    private EventStatus status;
}
