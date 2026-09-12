package com.logistics.dto.request;

import jakarta.validation.constraints.NotNull;

// Incident resolution/cancellation is a distinct workflow action (sets
// resolvedAt + status together) — kept separate from the creation DTO,
// consistent with the status-update pattern from batch 2.
public record IncidentStatusUpdateRequest(
        @NotNull com.logistics.entity.enums.IncidentStatus status
) {}