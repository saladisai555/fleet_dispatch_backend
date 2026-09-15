package com.logistics.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComplianceEvaluateRequest(
        @NotNull Long affectedManifestId,
        @NotNull Long proposedVehicleId,
        @NotNull Long proposedDriverId,
        @NotNull LocalDateTime proposedEta,
        BigDecimal proposedAdditionalDrivingHours // optional - defaults to 0 if omitted
) {}