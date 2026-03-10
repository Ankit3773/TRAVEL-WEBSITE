package com.travel.travelapp.repository.projection;

import com.travel.travelapp.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface BookingDailyTrendProjection {

    LocalDate getBookingDate();

    BookingStatus getBookingStatus();

    long getBookingCount();

    BigDecimal getTotalAmount();
}
