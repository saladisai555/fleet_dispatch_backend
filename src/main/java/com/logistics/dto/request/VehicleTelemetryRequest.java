package com.logistics.dto.request;

import com.logistics.entity.enums.EngineStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VehicleTelemetryRequest(
        @NotNull Long vehicleId,

        @NotNull LocalDateTime recordedAt, // client-supplied per API_Specification.md Section 14.1

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,

        @NotNull @DecimalMin("0.0") BigDecimal speedKmh,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal fuelLevelPercent,

        BigDecimal engineTemperatureC,
        BigDecimal cargoTemperatureC,

        @NotNull @DecimalMin("0.0") BigDecimal odometerKm,

        @NotNull EngineStatus engineStatus,

        // Documented request shape includes a client-supplied temperatureAlert
        // field. It is accepted here for schema compatibility but deliberately
        // NOT trusted as authoritative - see TelemetryServiceImpl. Cold-chain
        // violations are safety-relevant, so the backend recomputes this
        // value deterministically rather than accepting an external claim,
        // consistent with the project's rule that external/AI-supplied data
        // never bypasses a deterministic backend check.
        Boolean temperatureAlert,

        String engineFaultCode
) {}