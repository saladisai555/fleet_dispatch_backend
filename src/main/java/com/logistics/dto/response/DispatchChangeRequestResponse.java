package com.logistics.dto.response;

import com.logistics.entity.enums.DispatchChangeStatus;
import com.logistics.entity.enums.TriggerType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Shape matches API_Specification.md Section 15.2 exactly:
// current/proposed are nested, proposed.route is its own sub-object.
public record DispatchChangeRequestResponse(
        Long id,
        String requestNumber,
        TriggerType triggerType,
        Long triggerId,
        Long affectedManifestId,

        CurrentState current,
        ProposedState proposed,

        String reason,
        String proposalSummary,
        String routeChanges,

        Integer estimatedDelayMinutes,
        BigDecimal estimatedAdditionalDistanceKm,

        DispatchChangeStatus status,
        String createdByAgent,

        Long reviewedById,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewComments,

        ComplianceCheckResponse compliance,

        LocalDateTime createdAt
) {
    // Reflects the manifest's actual vehicle/driver/route at response time —
    // this is what "current" means: live operational state, not a stored snapshot.
    public record CurrentState(
            Long vehicleId,
            String vehicleCode,
            Long driverId,
            String driverName,
            Long routeId
    ) {}

    public record ProposedState(
            Long vehicleId,
            String vehicleCode,
            Long driverId,
            String driverName,
            ProposedRoute route // nullable — a proposal may not change the route
    ) {}

    public record ProposedRoute(
            Long routeId,
            BigDecimal distanceKm,
            Integer estimatedDurationMinutes
    ) {}
}