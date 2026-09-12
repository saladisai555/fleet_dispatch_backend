package com.logistics.dto.request;

import com.logistics.entity.enums.ManifestItemStatus;
import jakarta.validation.constraints.NotNull;

// Delivery progress (picked up / delivered / failed) updates independently
// of the manifest's own lifecycle, so it's a separate narrow DTO — same
// pattern as DriverAvailabilityUpdateRequest and DeliveryOrderStatusUpdateRequest.
public record ManifestItemStatusUpdateRequest(
        @NotNull ManifestItemStatus status
) {}