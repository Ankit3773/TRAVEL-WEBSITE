package com.travel.travelapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "seat_bookings")
public class SeatBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_schedule_id", nullable = false)
    private TripSchedule tripSchedule;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booked_by_user_id", nullable = false)
    @JsonIgnore
    private AppUser bookedByUser;

    @Column(nullable = false)
    private Integer seatNumber;

    @Column(nullable = false, length = 80)
    private String passengerName;

    @Column(nullable = false, length = 15)
    private String passengerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", length = 20)
    private BookingStatus bookingStatus;

    @Column
    private LocalDateTime cancelledAt;

    @ManyToOne
    @JoinColumn(name = "cancelled_by_user_id")
    @JsonIgnore
    private AppUser cancelledByUser;
}
