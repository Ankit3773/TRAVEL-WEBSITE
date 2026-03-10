package com.travel.travelapp.repository;

import com.travel.travelapp.entity.Route;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByActiveTrue();

    List<Route> findAllByOrderBySourceAscDestinationAsc();

    List<Route> findByActiveTrueAndTourismRouteTrueOrderBySourceAscDestinationAsc();

    long countByActiveTrue();

    Optional<Route> findBySourceIgnoreCaseAndDestinationIgnoreCase(String source, String destination);
}
