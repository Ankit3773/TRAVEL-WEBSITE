package com.travel.travelapp.entity;

public enum BookingStatus {
    LOCKED,
    BOOKED,
    @Deprecated
    CONFIRMED,
    CANCELLED;

    public boolean isBookedState() {
        return this == BOOKED || this == CONFIRMED;
    }

    public static BookingStatus normalizeFilter(BookingStatus status) {
        if (status == CONFIRMED) {
            return BOOKED;
        }
        return status;
    }
}
