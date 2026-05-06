package com.travel.travelapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false, length = 80)
    private String destination;

    @Column(nullable = false)
    private Integer distanceKm;

    @Column(nullable = false)
    private Boolean active = true;

    @Column
    private Boolean tourismRoute = false;

    @Column(length = 320)
    private String description;

    @Column(length = 600)
    private String travelHighlights;

    @Column(length = 600)
    private String travelTips;

    @OneToMany(mappedBy = "route")
    @JsonIgnore
    private List<TripSchedule> schedules;
}
