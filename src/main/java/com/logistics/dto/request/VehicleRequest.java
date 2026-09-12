package com.logistics.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record VehicleRequest(
        @NotBlank String registrationNumber,
        @NotBlank String vehicleCode,
        @NotBlank String vehicleType,

        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal capacityKg,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal capacityVolumeM3,

        @NotBlank String fuelType,

        @NotNull Boolean refrigerated,
        BigDecimal minTemperatureC, // required only if refrigerated=true — validated in service
        BigDecimal maxTemperatureC,

        @NotBlank String status,

        Long currentLocationId,
        Long currentDriverId
) {}