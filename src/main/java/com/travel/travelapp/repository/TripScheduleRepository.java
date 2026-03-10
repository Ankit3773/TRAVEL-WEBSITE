package com.travel.travelapp.repository;

import com.travel.travelapp.entity.TripSchedule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripScheduleRepository extends JpaRepository<TripSchedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ts from TripSchedule ts where ts.id = :id")
    Optional<TripSchedule> findByIdForUpdate(Long id);

    List<TripSchedule> findByActiveTrue();

    List<TripSchedule> findAllByOrderByTravelDateAscDepartureTimeAsc();

    List<TripSchedule> findByTravelDateAndActiveTrue(LocalDate travelDate);

    List<TripSchedule> findByTravelDateAndRouteSourceIgnoreCaseAndRouteDestinationIgnoreCaseAndActiveTrue(
            LocalDate travelDate,
            String source,
            String destination);

    List<TripSchedule> findByBusIdAndActiveTrue(Long busId);

    List<TripSchedule> findByRouteIdAndActiveTrue(Long routeId);

    List<TripSchedule> findAllByBusIdAndTravelDateAndRouteIdAndDepartureTimeOrderByIdAsc(
            Long busId, LocalDate travelDate, Long routeId, LocalTime departureTime);

    boolean existsByBusIdAndTravelDateAndRouteIdAndDepartureTime(
            Long busId, LocalDate travelDate, Long routeId, LocalTime departureTime);

    long countByActiveTrue();

    long countByTravelDateGreaterThanEqualAndActiveTrue(LocalDate date);

    @Query("select coalesce(sum(ts.bus.totalSeats), 0) from TripSchedule ts where ts.active = true")
    Long sumSeatCapacityForActiveSchedules();
}
