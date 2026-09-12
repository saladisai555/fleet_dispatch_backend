package com.logistics.dto.response;

import com.logistics.entity.enums.AvailabilityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DriverResponse(
        Long id,
        Long userId,
        String employeeCode,
        String name,
        String phone,
        String licenseNumber,
        String licenseType,
        LocalDate licenseExpiry,
        BigDecimal maxDrivingHours,
        BigDecimal currentDrivingHours,
        AvailabilityStatus availabilityStatus,
        Long currentLocationId,
        String currentLocationName, // flattened, avoids exposing full Location object
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}