package com.travel.travelapp.repository;

import com.travel.travelapp.entity.Bus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRepository extends JpaRepository<Bus, Long> {
    List<Bus> findByActiveTrue();

    List<Bus> findAllByOrderByBusNumberAsc();

    long countByActiveTrue();

    Optional<Bus> findByBusNumberIgnoreCase(String busNumber);
}
