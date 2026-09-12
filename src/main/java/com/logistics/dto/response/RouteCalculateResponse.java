package com.logistics.dto.response;

import java.math.BigDecimal;

public record RouteCalculateResponse(
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        Integer trafficDelayMinutes,
        String routeGeometry
) {}