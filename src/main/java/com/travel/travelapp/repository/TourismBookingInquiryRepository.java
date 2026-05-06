package com.travel.travelapp.repository;

import com.travel.travelapp.entity.TourismBookingInquiry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourismBookingInquiryRepository extends JpaRepository<TourismBookingInquiry, Long> {

    List<TourismBookingInquiry> findAllByOrderByCreatedAtDesc();
}
