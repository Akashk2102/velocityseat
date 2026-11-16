package com.velocityseat.modules.booking.event;

import com.velocityseat.modules.notification.event.BookingConfirmedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BookingConfirmedInternalEvent {
    private final BookingConfirmedEvent event;
}
