package com.logistics.dto.request;

import com.logistics.entity.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

// Narrow, single-purpose update — availability changes frequently and
// independently of the driver's master data, so it gets its own endpoint/DTO
// rather than forcing a full DriverRequest just to flip a status.
public record DriverAvailabilityUpdateRequest(
        @NotNull AvailabilityStatus availabilityStatus,
        Long currentLocationId
) {}