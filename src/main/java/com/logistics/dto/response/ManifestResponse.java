package com.logistics.dto.response;

import com.logistics.entity.enums.ManifestStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ManifestResponse(
        Long id,
        String manifestNumber,
        Long vehicleId,
        String vehicleCode,
        Long driverId,
        String driverName,
        Long startLocationId,
        String startLocationName,
        LocalDateTime plannedStartTime,
        LocalDateTime plannedEndTime,
        LocalDateTime actualStartTime,
        LocalDateTime actualEndTime,
        ManifestStatus status,
        List<ManifestItemResponse> items, // populated only on the detail endpoint, not list views
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}