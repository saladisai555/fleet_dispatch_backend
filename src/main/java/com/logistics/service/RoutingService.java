package com.logistics.service;

import com.logistics.entity.Location;
import com.logistics.entity.Vehicle;

public interface RoutingService {

    /**
     * Interim implementation (haversine-based estimate) until a real
     * provider (OSRM, per Step 18 / Section 13 of the project brief) is
     * wired in. Callers must not treat distanceKm/estimatedDurationMinutes
     * as road-accurate - they are straight-line-derived approximations.
     */
    RouteCalculationResult calculateRoute(Location origin, Location destination, Vehicle vehicle);

    record RouteCalculationResult(
            java.math.BigDecimal distanceKm,
            Integer estimatedDurationMinutes,
            Integer trafficDelayMinutes,
            String routeGeometry
    ) {}
}