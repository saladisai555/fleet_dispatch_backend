package com.logistics.dto.response;

import com.logistics.entity.enums.IncidentSeverity;
import com.logistics.entity.enums.IncidentStatus;
import com.logistics.entity.enums.IncidentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        IncidentType incidentType,
        IncidentSeverity severity,
        String title,
        String description,
        Long locationId,
        String locationName,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime startedAt,
        LocalDateTime expectedEndAt,
        LocalDateTime resolvedAt,
        IncidentStatus status,
        String source,
        LocalDateTime createdAt
) {}