package com.logistics.dto.request;

import jakarta.validation.constraints.NotNull;

public record RouteCalculateRequest(
        @NotNull Long originLocationId,
        @NotNull Long destinationLocationId,
        @NotNull Long vehicleId
) {}