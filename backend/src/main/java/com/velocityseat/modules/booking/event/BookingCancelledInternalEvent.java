package com.velocityseat.modules.booking.event;

import com.velocityseat.modules.notification.event.BookingCancelledEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BookingCancelledInternalEvent {
    private final BookingCancelledEvent event;
}
