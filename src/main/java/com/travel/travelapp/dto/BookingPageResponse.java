package com.travel.travelapp.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BookingPageResponse {
    List<BookingHistoryResponse> items;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;
}
