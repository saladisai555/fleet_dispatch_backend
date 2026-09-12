package com.logistics.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

// Mirrors chk_manifests_planned_time (end > start) — enforced again in service
// since Bean Validation can't express cross-field ordering directly here.
public record ManifestRequest(
        @NotNull Long vehicleId,
        @NotNull Long driverId,
        @NotNull Long startLocationId,

        @NotNull LocalDateTime plannedStartTime,
        @NotNull LocalDateTime plannedEndTime
) {}