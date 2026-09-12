package com.logistics.dto.request;

import com.logistics.entity.enums.IncidentSeverity;
import com.logistics.entity.enums.IncidentType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Mirrors the POST /api/webhooks/traffic example from Section 12, generalized
// to cover all incident types your schema supports, not just ROAD_CLOSURE.
// locationId and lat/long are both optional-but-one-should-exist — enforced
// in service, since an incident may reference a known location OR a raw point.
public record IncidentRequest(
        @NotNull IncidentType incidentType,
        @NotNull IncidentSeverity severity,

        @NotBlank String title,
        @NotBlank String description,

        Long locationId,

        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,

        @NotNull LocalDateTime startedAt,
        LocalDateTime expectedEndAt,

        @NotBlank String source
) {}