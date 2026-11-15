package com.velocityseat.modules.venue.dto;

import com.velocityseat.modules.venue.model.Venue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private Integer totalCapacity;
    private Long configuredSeatsCount;
    private Instant createdAt;

    public static VenueResponse fromEntity(Venue venue, Long configuredSeats) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .state(venue.getState())
                .country(venue.getCountry())
                .postalCode(venue.getPostalCode())
                .totalCapacity(venue.getTotalCapacity())
                .configuredSeatsCount(configuredSeats != null ? configuredSeats : 0L)
                .createdAt(venue.getCreatedAt())
                .build();
    }
}
