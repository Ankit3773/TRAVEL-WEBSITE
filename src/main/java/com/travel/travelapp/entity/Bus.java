package com.travel.travelapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "buses")
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String busNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusType busType;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "bus")
    @JsonIgnore
    private List<TripSchedule> schedules;

    @OneToMany(mappedBy = "bus")
    @JsonIgnore
    private List<Seat> seats;
}
