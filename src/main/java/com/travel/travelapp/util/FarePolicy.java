package com.travel.travelapp.util;

import com.travel.travelapp.entity.Bus;
import com.travel.travelapp.entity.BusType;
import com.travel.travelapp.entity.Route;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FarePolicy {

    public static final BigDecimal REGULAR_RATE_PER_KM = new BigDecimal("2.50");
    public static final BigDecimal REGULAR_AC_RATE_PER_KM = new BigDecimal("3.00");
    public static final BigDecimal TOURISM_RATE_PER_KM = new BigDecimal("5.00");

    private FarePolicy() {}

    public static BigDecimal fareFor(Route route, Bus bus) {
        return fareFor(route.getDistanceKm(), Boolean.TRUE.equals(route.getTourismRoute()), bus.getBusType());
    }

    public static BigDecimal fareFor(int distanceKm, boolean tourismRoute, BusType busType) {
        BigDecimal ratePerKm = rateFor(tourismRoute, busType);
        return ratePerKm.multiply(BigDecimal.valueOf(distanceKm)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal rateFor(boolean tourismRoute, BusType busType) {
        if (tourismRoute) {
            return TOURISM_RATE_PER_KM;
        }
        if (busType == BusType.AC) {
            return REGULAR_AC_RATE_PER_KM;
        }
        return REGULAR_RATE_PER_KM;
    }
}
