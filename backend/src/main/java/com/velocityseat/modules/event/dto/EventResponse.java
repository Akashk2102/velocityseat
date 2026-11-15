package com.velocityseat.modules.event.dto;

import com.velocityseat.modules.event.model.Event;
import com.velocityseat.modules.event.model.EventCategory;
import com.velocityseat.modules.event.model.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private Long venueId;
    private String venueName;
    private String venueCity;
    private String title;
    private String description;
    private EventCategory category;
    private Instant startTime;
    private Instant endTime;
    private EventStatus status;
    private long totalSeats;
    private long availableSeats;
    private Instant createdAt;

    public static EventResponse fromEntity(Event event, long totalSeats, long availableSeats) {
        return EventResponse.builder()
                .id(event.getId())
                .venueId(event.getVenue().getId())
                .venueName(event.getVenue().getName())
                .venueCity(event.getVenue().getCity())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .createdAt(event.getCreatedAt())
                .build();
    }
}
