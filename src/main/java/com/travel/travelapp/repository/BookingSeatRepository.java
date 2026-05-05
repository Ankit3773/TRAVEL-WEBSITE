package com.travel.travelapp.repository;

import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.entity.BookingSeat;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);

    /**
     * Bulk delete child rows for a set of bookings as a single SQL statement.
     * Using a @Modifying @Query (with clearAutomatically) guarantees the DELETE
     * is flushed to the DB before any subsequent operation on the parent
     * `bookings` table runs — preventing FK violations when the parent rows
     * are deleted right after.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookingSeat bs where bs.booking.id in :bookingIds")
    void deleteByBookingIdIn(@Param("bookingIds") Collection<Long> bookingIds);

    long countByBookingBookingStatusAndActiveTrue(BookingStatus bookingStatus);

    long countByBookingBookingStatusInAndActiveTrue(Collection<BookingStatus> statuses);
}
