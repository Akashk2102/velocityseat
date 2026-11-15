package com.velocityseat.modules.venue.service;

import com.velocityseat.common.exception.BadRequestException;
import com.velocityseat.common.exception.ResourceNotFoundException;
import com.velocityseat.modules.venue.dto.*;
import com.velocityseat.modules.venue.model.Seat;
import com.velocityseat.modules.venue.model.Venue;
import com.velocityseat.modules.venue.repository.SeatRepository;
import com.velocityseat.modules.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public VenueResponse createVenue(CreateVenueRequest request) {
        if (venueRepository.existsByName(request.getName())) {
            throw new BadRequestException("Venue with name '" + request.getName() + "' already exists");
        }

        Venue venue = Venue.builder()
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .state(request.getState() != null ? request.getState().trim() : null)
                .country(request.getCountry().trim())
                .postalCode(request.getPostalCode() != null ? request.getPostalCode().trim() : null)
                .totalCapacity(request.getTotalCapacity())
                .build();

        Venue savedVenue = venueRepository.save(venue);
        return VenueResponse.fromEntity(savedVenue, 0L);
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        long count = seatRepository.countByVenueId(venueId);
        return VenueResponse.fromEntity(venue, count);
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(venue -> {
                    long count = seatRepository.countByVenueId(venue.getId());
                    return VenueResponse.fromEntity(venue, count);
                })
                .toList();
    }

    @Transactional
    public List<SeatResponse> generateSeats(Long venueId, GenerateSeatsRequest request) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));

        int newSeatsCount = request.getTotalRows() * request.getSeatsPerRow();
        long currentSeatCount = seatRepository.countByVenueId(venueId);

        if (currentSeatCount + newSeatsCount > venue.getTotalCapacity()) {
            throw new BadRequestException(String.format(
                    "Cannot generate %d seats. Venue total capacity is %d, already configured %d seats.",
                    newSeatsCount, venue.getTotalCapacity(), currentSeatCount
            ));
        }

        List<Seat> seatsToSave = new ArrayList<>();
        for (int r = 1; r <= request.getTotalRows(); r++) {
            String rowNumber = String.valueOf(r);
            for (int s = 1; s <= request.getSeatsPerRow(); s++) {
                String seatNumber = String.valueOf(s);

                if (!seatRepository.existsByVenueIdAndSectionAndRowNumberAndSeatNumber(
                        venueId, request.getSection(), rowNumber, seatNumber)) {
                    seatsToSave.add(Seat.builder()
                            .venue(venue)
                            .section(request.getSection())
                            .rowNumber(rowNumber)
                            .seatNumber(seatNumber)
                            .seatType(request.getSeatType())
                            .build());
                }
            }
        }

        List<Seat> savedSeats = seatRepository.saveAll(seatsToSave);
        return savedSeats.stream().map(SeatResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByVenueId(Long venueId) {
        if (!venueRepository.existsById(venueId)) {
            throw new ResourceNotFoundException("Venue", "id", venueId);
        }
        return seatRepository.findByVenueIdOrderBySectionAscRowNumberAscSeatNumberAsc(venueId)
                .stream()
                .map(SeatResponse::fromEntity)
                .toList();
    }
}
