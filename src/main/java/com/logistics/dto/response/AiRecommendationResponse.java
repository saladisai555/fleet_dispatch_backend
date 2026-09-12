package com.logistics.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// Received FROM the AI service (Spring AI or external Python service — same
// shape either way, per Rule 8). This is untrusted input structurally: it is
// NEVER persisted or acted on directly. It must pass through ComplianceService
// and become a DispatchChangeRequestCreateRequest only after validation.
public record AiRecommendationResponse(
        @NotNull Long orderId,

        @NotBlank String action, // e.g. REROUTE, REASSIGN_VEHICLE, REASSIGN_DRIVER, RESEQUENCE

        Long proposedVehicleId,
        Long proposedDriverId,
        Long proposedRouteId,

        @NotBlank String reason,

        LocalDateTime estimatedArrival,

        Double confidenceScore // nullable — optional, informational only,
        // never used to bypass or weight compliance checks
) {}