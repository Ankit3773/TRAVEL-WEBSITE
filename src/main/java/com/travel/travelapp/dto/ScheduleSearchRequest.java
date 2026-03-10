package com.travel.travelapp.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleSearchRequest {
    private LocalDate date;
    private String source;
    private String destination;
}
