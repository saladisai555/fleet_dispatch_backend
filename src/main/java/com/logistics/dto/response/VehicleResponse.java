package com.logistics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VehicleResponse(
        Long id,
        String registrationNumber,
        String vehicleCode,
        String vehicleType,
        BigDecimal capacityKg,
        BigDecimal capacityVolumeM3,
        String fuelType,
        Boolean refrigerated,
        BigDecimal minTemperatureC,
        BigDecimal maxTemperatureC,
        String status,
        Long currentLocationId,
        String currentLocationName,
        Long currentDriverId,
        String currentDriverName,
        BigDecimal odometerKm,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}