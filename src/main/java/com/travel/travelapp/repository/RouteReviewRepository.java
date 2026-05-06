package com.travel.travelapp.repository;

import com.travel.travelapp.entity.RouteReview;
import com.travel.travelapp.repository.projection.RouteReviewSummaryProjection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteReviewRepository extends JpaRepository<RouteReview, Long> {

    @EntityGraph(attributePaths = {"route", "booking", "reviewedByUser"})
    Optional<RouteReview> findByBookingId(Long bookingId);

    @EntityGraph(attributePaths = {"route", "booking", "reviewedByUser"})
    List<RouteReview> findByBookingIdIn(Collection<Long> bookingIds);

    @EntityGraph(attributePaths = {"route", "booking", "reviewedByUser"})
    List<RouteReview> findByRouteIdOrderByCreatedAtDesc(Long routeId, Pageable pageable);

    @EntityGraph(attributePaths = {"route", "booking", "reviewedByUser"})
    List<RouteReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select rr.route.id as routeId,
                   avg(rr.rating) as averageRating,
                   count(rr) as reviewCount
            from RouteReview rr
            where rr.route.id in :routeIds
            group by rr.route.id
            """)
    List<RouteReviewSummaryProjection> summarizeByRouteIds(@Param("routeIds") Collection<Long> routeIds);
}
