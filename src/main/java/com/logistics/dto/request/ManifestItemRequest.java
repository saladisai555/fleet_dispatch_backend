package com.logistics.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record ManifestItemRequest(
        @NotNull Long orderId,

        @NotNull @Positive Integer sequenceNumber,

        @NotNull LocalDateTime plannedArrivalTime
) {}