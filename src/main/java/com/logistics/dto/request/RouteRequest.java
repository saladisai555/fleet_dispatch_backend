package com.logistics.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

// route_geometry, distance_km, and estimated_duration_minutes are intentionally
// absent from the request — those come from the RoutingService (OSRM) computation,
// not from a human-supplied payload. This DTO represents "plan a route for this
// manifest" (origin/destination/timing intent), not the raw route row.
public record RouteRequest(
        @NotNull Long manifestId,
        @NotNull Long originLocationId,
        @NotNull Long destinationLocationId,

        @NotNull LocalDateTime plannedStartTime,
        @NotNull LocalDateTime plannedEndTime
) {}