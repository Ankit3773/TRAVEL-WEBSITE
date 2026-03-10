package com.travel.travelapp.repository;

import com.travel.travelapp.entity.SeatBooking;
import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.repository.projection.BookingDailyTrendProjection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatBookingRepository extends JpaRepository<SeatBooking, Long> {

    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser"})
    List<SeatBooking> findByTripScheduleId(Long tripScheduleId);

    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser"})
    List<SeatBooking> findByTripScheduleIdAndBookingStatus(Long tripScheduleId, BookingStatus bookingStatus);

    Optional<SeatBooking> findByTripScheduleIdAndSeatNumberAndBookingStatus(
            Long tripScheduleId, Integer seatNumber, BookingStatus bookingStatus);

    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser"})
    List<SeatBooking> findByBookedByUserEmailIgnoreCaseOrderByBookedAtDesc(String email);

    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser"})
    Page<SeatBooking> findByBookedByUserEmailIgnoreCaseOrderByBookedAtDesc(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser"})
    Page<SeatBooking> findByBookedByUserEmailIgnoreCaseAndBookingStatusOrderByBookedAtDesc(
            String email, BookingStatus bookingStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser"})
    List<SeatBooking> findAllByOrderByBookedAtDesc();

    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser"})
    Page<SeatBooking> findAllByOrderByBookedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"tripSchedule", "tripSchedule.route", "tripSchedule.bus", "bookedByUser"})
    Page<SeatBooking> findByBookingStatusOrderByBookedAtDesc(BookingStatus bookingStatus, Pageable pageable);

    @Query("""
            select cast(sb.bookedAt as date) as bookingDate,
                   sb.bookingStatus as bookingStatus,
                   count(sb) as bookingCount,
                   coalesce(sum(sb.amount), 0) as totalAmount
            from SeatBooking sb
            where sb.bookedAt >= :fromDateTime
              and sb.bookedAt < :toExclusive
            group by cast(sb.bookedAt as date), sb.bookingStatus
            order by cast(sb.bookedAt as date)
            """)
    List<BookingDailyTrendProjection> findDailyBookingTrend(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toExclusive") LocalDateTime toExclusive);

    long countByBookingStatus(BookingStatus bookingStatus);
}
