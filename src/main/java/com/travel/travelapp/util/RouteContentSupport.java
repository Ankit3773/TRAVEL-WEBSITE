package com.travel.travelapp.util;

import com.travel.travelapp.entity.Route;

public final class RouteContentSupport {

    private RouteContentSupport() {
    }

    public static String defaultDescription(Route route) {
        return defaultDescription(route.getSource(), route.getDestination(), route.getDistanceKm(), route.getTourismRoute());
    }

    public static String defaultDescription(String source, String destination, Integer distanceKm, Boolean tourismRoute) {
        String routeLabel = source + " to " + destination;
        if (Boolean.TRUE.equals(tourismRoute)) {
            return routeLabel + " is a premium Bihar tourism corridor built for planned sightseeing, family itineraries, and comfortable AC travel over approximately "
                    + distanceKm + " km.";
        }
        return routeLabel + " is a dependable intercity corridor built for repeat travel, commuter demand, and simple seat booking over approximately "
                + distanceKm + " km.";
    }

    public static String defaultHighlights(Route route) {
        return defaultHighlights(route.getSource(), route.getDestination(), route.getDistanceKm(), route.getTourismRoute());
    }

    public static String defaultHighlights(String source, String destination, Integer distanceKm, Boolean tourismRoute) {
        return String.join("\n",
                "Distance: " + distanceKm + " km",
                Boolean.TRUE.equals(tourismRoute)
                        ? "Designed for premium Bihar sightseeing demand and planned leisure travel"
                        : "Designed for dependable intercity movement and repeat travel demand",
                "Popular search corridor: " + source + " to " + destination);
    }

    public static String defaultTravelTips(Route route) {
        return defaultTravelTips(route.getSource(), route.getDestination(), route.getTourismRoute());
    }

    public static String defaultTravelTips(String source, String destination, Boolean tourismRoute) {
        return String.join("\n",
                "Reach the boarding point at least 20 minutes before departure.",
                "Compare fare and departure windows before locking your seats.",
                Boolean.TRUE.equals(tourismRoute)
                        ? "Tourism trips are better planned with morning departures and AC options."
                        : "Peak commute demand is usually easier to manage with earlier departures.");
    }
}
