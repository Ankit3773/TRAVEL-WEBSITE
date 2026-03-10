package com.travel.travelapp.repository;

import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.entity.BookingSeat;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);

    void deleteByBookingIdIn(Collection<Long> bookingIds);

    long countByBookingBookingStatusAndActiveTrue(BookingStatus bookingStatus);

    long countByBookingBookingStatusInAndActiveTrue(Collection<BookingStatus> statuses);
}
