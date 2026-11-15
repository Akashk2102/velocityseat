package com.velocityseat.modules.venue.service;

import com.velocityseat.common.exception.BadRequestException;
import com.velocityseat.modules.venue.dto.CreateVenueRequest;
import com.velocityseat.modules.venue.dto.GenerateSeatsRequest;
import com.velocityseat.modules.venue.dto.SeatResponse;
import com.velocityseat.modules.venue.dto.VenueResponse;
import com.velocityseat.modules.venue.model.Seat;
import com.velocityseat.modules.venue.model.SeatType;
import com.velocityseat.modules.venue.model.Venue;
import com.velocityseat.modules.venue.repository.SeatRepository;
import com.velocityseat.modules.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private VenueService venueService;

    private Venue mockVenue;

    @BeforeEach
    void setUp() {
        mockVenue = Venue.builder()
                .id(1L)
                .name("Madison Square Garden")
                .address("4 Pennsylvania Plaza")
                .city("New York")
                .state("NY")
                .country("USA")
                .postalCode("10001")
                .totalCapacity(20000)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new venue")
    void createVenue_Success() {
        CreateVenueRequest request = CreateVenueRequest.builder()
                .name("Madison Square Garden")
                .address("4 Pennsylvania Plaza")
                .city("New York")
                .state("NY")
                .country("USA")
                .postalCode("10001")
                .totalCapacity(20000)
                .build();

        when(venueRepository.existsByName("Madison Square Garden")).thenReturn(false);
        when(venueRepository.save(any(Venue.class))).thenReturn(mockVenue);

        VenueResponse response = venueService.createVenue(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Madison Square Garden");
        assertThat(response.getTotalCapacity()).isEqualTo(20000);
        verify(venueRepository, times(1)).save(any(Venue.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when venue name already exists")
    void createVenue_DuplicateName_ThrowsBadRequest() {
        CreateVenueRequest request = CreateVenueRequest.builder()
                .name("Madison Square Garden")
                .address("4 Pennsylvania Plaza")
                .city("New York")
                .country("USA")
                .totalCapacity(20000)
                .build();

        when(venueRepository.existsByName("Madison Square Garden")).thenReturn(true);

        assertThatThrownBy(() -> venueService.createVenue(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(venueRepository, never()).save(any(Venue.class));
    }

    @Test
    @DisplayName("Should batch generate seats for a section")
    void generateSeats_Success() {
        GenerateSeatsRequest request = GenerateSeatsRequest.builder()
                .section("Orchestra")
                .totalRows(2)
                .seatsPerRow(3)
                .seatType(SeatType.VIP)
                .build();

        when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
        when(seatRepository.countByVenueId(1L)).thenReturn(0L);
        when(seatRepository.existsByVenueIdAndSectionAndRowNumberAndSeatNumber(any(), any(), any(), any()))
                .thenReturn(false);

        Seat seat1 = Seat.builder().id(101L).venue(mockVenue).section("Orchestra").rowNumber("1").seatNumber("1").seatType(SeatType.VIP).build();
        Seat seat2 = Seat.builder().id(102L).venue(mockVenue).section("Orchestra").rowNumber("1").seatNumber("2").seatType(SeatType.VIP).build();

        when(seatRepository.saveAll(anyList())).thenReturn(List.of(seat1, seat2));

        List<SeatResponse> responses = venueService.generateSeats(1L, request);

        assertThat(responses).hasSize(2);
        verify(seatRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw BadRequestException if generated seats exceed venue capacity")
    void generateSeats_ExceedCapacity_ThrowsBadRequest() {
        mockVenue.setTotalCapacity(5); // Capacity only 5
        GenerateSeatsRequest request = GenerateSeatsRequest.builder()
                .section("Balcony")
                .totalRows(2)
                .seatsPerRow(5) // 10 seats requested
                .seatType(SeatType.STANDARD)
                .build();

        when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
        when(seatRepository.countByVenueId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> venueService.generateSeats(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot generate 10 seats");

        verify(seatRepository, never()).saveAll(any());
    }
}
