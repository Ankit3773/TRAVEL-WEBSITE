package com.travel.travelapp.repository;

import com.travel.travelapp.entity.Booking;
import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.repository.projection.BookingDailyTrendProjection;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser", "bookingSeats", "bookingSeats.seat"})
    List<Booking> findByTripScheduleIdAndBookingStatusIn(Long tripScheduleId, Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = {
        "tripSchedule",
        "tripSchedule.route",
        "tripSchedule.bus",
        "bookedByUser",
        "cancelledByUser",
        "bookingSeats",
        "bookingSeats.seat"
    })
    Optional<Booking> findDetailedById(Long id);

    @EntityGraph(attributePaths = {
        "tripSchedule",
        "tripSchedule.route",
        "tripSchedule.bus",
        "bookedByUser",
        "cancelledByUser",
        "bookingSeats",
        "bookingSeats.seat"
    })
    List<Booking> findByBookedByUserEmailIgnoreCaseAndBookingStatusNotOrderByBookedAtDesc(
            String email, BookingStatus excludedStatus);

    @EntityGraph(attributePaths = {
        "tripSchedule",
        "tripSchedule.route",
        "tripSchedule.bus",
        "bookedByUser",
        "cancelledByUser",
        "bookingSeats",
        "bookingSeats.seat"
    })
    Page<Booking> findByBookedByUserEmailIgnoreCaseAndBookingStatusNotOrderByBookedAtDesc(
            String email, BookingStatus excludedStatus, Pageable pageable);

    @EntityGraph(attributePaths = {
        "tripSchedule",
        "tripSchedule.route",
        "tripSchedule.bus",
        "bookedByUser",
        "cancelledByUser",
        "bookingSeats",
        "bookingSeats.seat"
    })
    Page<Booking> findByBookedByUserEmailIgnoreCaseAndBookingStatusOrderByBookedAtDesc(
            String email, BookingStatus bookingStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"bookingSeats"})
    List<Booking> findByBookingStatusAndLockExpiresAtBefore(BookingStatus bookingStatus, LocalDateTime lockExpiresAt);

    @EntityGraph(attributePaths = {
        "tripSchedule",
        "tripSchedule.route",
        "tripSchedule.bus",
        "bookedByUser",
        "cancelledByUser",
        "bookingSeats",
        "bookingSeats.seat"
    })
    List<Booking> findByBookingStatusNotOrderByBookedAtDesc(BookingStatus excludedStatus);

    @EntityGraph(attributePaths = {
        "tripSchedule",
        "tripSchedule.route",
        "tripSchedule.bus",
        "bookedByUser",
        "cancelledByUser",
        "bookingSeats",
        "bookingSeats.seat"
    })
    Page<Booking> findByBookingStatusNotOrderByBookedAtDesc(BookingStatus excludedStatus, Pageable pageable);

    @EntityGraph(attributePaths = {
        "tripSchedule",
        "tripSchedule.route",
        "tripSchedule.bus",
        "bookedByUser",
        "cancelledByUser",
        "bookingSeats",
        "bookingSeats.seat"
    })
    Page<Booking> findByBookingStatusOrderByBookedAtDesc(BookingStatus bookingStatus, Pageable pageable);

    @Query("""
            select cast(b.bookedAt as date) as bookingDate,
                   b.bookingStatus as bookingStatus,
                   count(b) as bookingCount,
                   coalesce(sum(b.amount), 0) as totalAmount
            from Booking b
            where b.bookedAt >= :fromDateTime
              and b.bookedAt < :toExclusive
              and b.bookingStatus in :statuses
            group by cast(b.bookedAt as date), b.bookingStatus
            order by cast(b.bookedAt as date)
            """)
    List<BookingDailyTrendProjection> findDailyBookingTrend(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("statuses") Collection<BookingStatus> statuses);

    long countByBookingStatusNot(BookingStatus bookingStatus);

    long countByBookingStatus(BookingStatus bookingStatus);

    long countByBookingStatusIn(Collection<BookingStatus> statuses);
}
