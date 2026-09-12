package com.logistics.dto.request;

import com.logistics.entity.enums.TriggerType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

// This is an INTERNAL creation DTO — used by DispatchService/agents when
// constructing a proposal, NOT a public controller endpoint. A DCR is never
// created directly from a raw client POST; it's always the output of the
// detect->diagnose->plan pipeline (manual or AI-driven). Kept as a proper
// DTO anyway so the creation path has one clean, validated entry point
// regardless of which internal caller builds it.
public record DispatchChangeRequestCreateRequest(
        @NotNull TriggerType triggerType,

        Long triggerId, // nullable — polymorphic pointer to incident/weather_alert/telemetry

        @NotNull Long affectedManifestId,

        Long proposedVehicleId,  // nullable — a proposal might only change route, not vehicle
        Long proposedDriverId,   // nullable

        @NotBlank String reason,
        @NotBlank String proposalSummary,

        String routeChanges, // nullable free text

        @NotNull @Min(0) Integer estimatedDelayMinutes,

        @NotNull @DecimalMin("0.0") BigDecimal estimatedAdditionalDistanceKm,

        @NotBlank String createdByAgent
) {}