package com.travel.travelapp.repository;

import com.travel.travelapp.entity.MarriageBookingInquiry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarriageBookingInquiryRepository extends JpaRepository<MarriageBookingInquiry, Long> {

    List<MarriageBookingInquiry> findAllByOrderByCreatedAtDesc();
}
