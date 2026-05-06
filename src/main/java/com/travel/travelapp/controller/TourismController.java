package com.travel.travelapp.controller;

import com.travel.travelapp.dto.CreateTourismInquiryRequest;
import com.travel.travelapp.dto.TourismInquiryResponse;
import com.travel.travelapp.entity.TourismBookingInquiry;
import com.travel.travelapp.exception.BadRequestException;
import com.travel.travelapp.repository.TourismBookingInquiryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tourism Booking")
public class TourismController {

    private static final BigDecimal RATE_PER_KM = new BigDecimal("5.00");
    private static final int MAX_GROUP_SIZE = 36;

    private static final Map<String, Integer> CIRCUIT_DISTANCES = Map.of(
            "Bodh Gaya",  125,
            "Rajgir",     100,
            "Nalanda",     95,
            "Vaishali",    60,
            "Pawapuri",    95
    );

    private final TourismBookingInquiryRepository repository;

    @PostMapping("/tourism/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public TourismInquiryResponse submitInquiry(@RequestBody CreateTourismInquiryRequest request) {
        validate(request);

        int distanceKm = CIRCUIT_DISTANCES.getOrDefault(request.getCircuit(), request.getDistanceKm() != null ? request.getDistanceKm() : 0);

        TourismBookingInquiry inquiry = new TourismBookingInquiry();
        inquiry.setContactName(request.getContactName().trim());
        inquiry.setContactPhone(request.getContactPhone().trim());
        inquiry.setCircuit(request.getCircuit().trim());
        inquiry.setDistanceKm(distanceKm);
        inquiry.setTravelDate(request.getTravelDate());
        inquiry.setGroupSize(request.getGroupSize());
        inquiry.setMessage(request.getMessage() != null ? request.getMessage().trim() : null);
        inquiry.setEstimatedFare(RATE_PER_KM.multiply(BigDecimal.valueOf(distanceKm)));

        return toResponse(repository.save(inquiry));
    }

    @GetMapping("/admin/tourism/inquiries")
    public List<TourismInquiryResponse> getAllInquiries() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validate(CreateTourismInquiryRequest req) {
        if (req.getContactName() == null || req.getContactName().isBlank()) {
            throw new BadRequestException("Contact name is required.");
        }
        if (req.getContactPhone() == null || req.getContactPhone().isBlank()) {
            throw new BadRequestException("Contact phone is required.");
        }
        if (req.getCircuit() == null || req.getCircuit().isBlank()) {
            throw new BadRequestException("Circuit / destination is required.");
        }
        if (!CIRCUIT_DISTANCES.containsKey(req.getCircuit())) {
            throw new BadRequestException("Unknown circuit. Choose from: " + String.join(", ", CIRCUIT_DISTANCES.keySet()));
        }
        if (req.getTravelDate() == null) {
            throw new BadRequestException("Travel date is required.");
        }
        if (req.getTravelDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Travel date cannot be in the past.");
        }
        if (req.getGroupSize() == null || req.getGroupSize() < 1) {
            throw new BadRequestException("Group size must be at least 1.");
        }
        if (req.getGroupSize() > MAX_GROUP_SIZE) {
            throw new BadRequestException("Maximum group size per bus is " + MAX_GROUP_SIZE + ".");
        }
    }

    private TourismInquiryResponse toResponse(TourismBookingInquiry entity) {
        TourismInquiryResponse res = new TourismInquiryResponse();
        res.setId(entity.getId());
        res.setContactName(entity.getContactName());
        res.setContactPhone(entity.getContactPhone());
        res.setCircuit(entity.getCircuit());
        res.setDistanceKm(entity.getDistanceKm());
        res.setTravelDate(entity.getTravelDate());
        res.setGroupSize(entity.getGroupSize());
        res.setEstimatedFare(entity.getEstimatedFare());
        res.setMessage(entity.getMessage());
        res.setStatus(entity.getStatus());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }
}
