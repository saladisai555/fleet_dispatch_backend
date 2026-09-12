package com.logistics.dto.response;

import com.logistics.entity.enums.ManifestItemStatus;
import java.time.LocalDateTime;

public record ManifestItemResponse(
        Long id,
        Long manifestId,
        Long orderId,
        String orderNumber, // flattened, avoids nested DeliveryOrderResponse here
        Integer sequenceNumber,
        LocalDateTime plannedArrivalTime,
        LocalDateTime actualArrivalTime,
        ManifestItemStatus status
) {}