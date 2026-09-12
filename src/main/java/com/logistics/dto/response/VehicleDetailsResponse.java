package com.logistics.dto.response;

public record VehicleDetailsResponse(
        VehicleResponse vehicle,
        VehicleTelemetryResponse latestTelemetry, // nullable - vehicle may have no readings yet
        ManifestResponse activeManifest,           // nullable - vehicle may be idle
        RouteResponse activeRoute                  // nullable
) {}