package com.logistics.dto.response;

import com.logistics.entity.enums.EngineStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VehicleTelemetryResponse(
        Long id,
        Long vehicleId,
        String vehicleCode,
        LocalDateTime recordedAt,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speedKmh,
        BigDecimal fuelLevelPercent,
        BigDecimal engineTemperatureC,
        BigDecimal cargoTemperatureC,
        BigDecimal odometerKm,
        EngineStatus engineStatus,
        Boolean temperatureAlert,
        String engineFaultCode
) {}