package com.logistics.dto.request;

import jakarta.validation.constraints.NotNull;
import com.logistics.entity.enums.OrderStatus;

// Order status is driven by workflow events (assignment, pickup, delivery,
// disruption) rather than free-form editing, so it gets its own narrow DTO
// rather than being folded into a general update.
public record DeliveryOrderStatusUpdateRequest(
        @NotNull OrderStatus status
) {}