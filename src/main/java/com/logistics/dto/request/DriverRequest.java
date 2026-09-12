package com.logistics.dto.request;

import com.logistics.entity.enums.AvailabilityStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DriverRequest(
        Long userId, // nullable — driver may not have a linked login yet

        @NotBlank String employeeCode,
        @NotBlank String name,
        @NotBlank String phone,
        @NotBlank String licenseNumber,
        @NotBlank String licenseType,

        @NotNull @Future(message = "license expiry must be in the future") LocalDate licenseExpiry,

        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal maxDrivingHours,

        @NotNull AvailabilityStatus availabilityStatus,

        Long currentLocationId // nullable
) {}