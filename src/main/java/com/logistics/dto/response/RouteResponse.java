package com.logistics.dto.response;

import com.logistics.entity.enums.RouteStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RouteResponse(
        Long id,
        Long manifestId,
        Long originLocationId,
        String originLocationName,
        Long destinationLocationId,
        String destinationLocationName,
        String routeGeometry,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        LocalDateTime plannedStartTime,
        LocalDateTime plannedEndTime,
        Integer trafficDelayMinutes,
        RouteStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}