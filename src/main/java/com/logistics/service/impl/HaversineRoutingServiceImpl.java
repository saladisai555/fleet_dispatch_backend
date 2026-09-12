package com.logistics.service.impl;

import com.logistics.entity.Location;
import com.logistics.entity.Vehicle;
import com.logistics.service.RoutingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class HaversineRoutingServiceImpl implements RoutingService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    // Straight-line distance underestimates real road distance - this factor
    // is a rough, commonly-used correction, NOT a substitute for real routing.
    private static final BigDecimal ROAD_DISTANCE_FACTOR = BigDecimal.valueOf(1.3);
    // Assumed average speed for duration estimation. Real speed varies by
    // vehicle type/road/traffic - this is an interim placeholder only.
    private static final double ASSUMED_AVERAGE_SPEED_KMH = 45.0;

    @Override
    public RouteCalculationResult calculateRoute(Location origin, Location destination, Vehicle vehicle) {
        double straightLineKm = haversineKm(
                origin.getLatitude().doubleValue(), origin.getLongitude().doubleValue(),
                destination.getLatitude().doubleValue(), destination.getLongitude().doubleValue());

        BigDecimal distanceKm = BigDecimal.valueOf(straightLineKm)
                .multiply(ROAD_DISTANCE_FACTOR)
                .setScale(2, RoundingMode.HALF_UP);

        int estimatedMinutes = (int) Math.ceil((distanceKm.doubleValue() / ASSUMED_AVERAGE_SPEED_KMH) * 60);

        return new RouteCalculationResult(distanceKm, estimatedMinutes, 0, null);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}