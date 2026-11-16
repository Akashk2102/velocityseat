package com.velocityseat.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private long totalVenues;
    private long totalEvents;
    private long totalBookings;
    private long totalTicketsSold;
    private BigDecimal totalGrossRevenue;
    private long activeHoldsCount;
}
