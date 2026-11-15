package com.velocityseat.modules.booking.dto;

import com.velocityseat.modules.booking.model.PaymentMethod;
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
public class ConfirmBookingRequest {

    @NotBlank(message = "Reservation token is required")
    private String reservationToken;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
