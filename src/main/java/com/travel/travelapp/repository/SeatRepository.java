package com.travel.travelapp.repository;

import com.travel.travelapp.entity.Seat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBusIdAndActiveTrueOrderBySeatNumberAsc(Long busId);

    List<Seat> findByBusIdOrderBySeatNumberAsc(Long busId);

    Optional<Seat> findByBusIdAndSeatNumber(Long busId, Integer seatNumber);

    List<Seat> findByBusIdAndSeatNumberInAndActiveTrueOrderBySeatNumberAsc(Long busId, List<Integer> seatNumbers);

    long countByBusIdAndActiveTrue(Long busId);
}
