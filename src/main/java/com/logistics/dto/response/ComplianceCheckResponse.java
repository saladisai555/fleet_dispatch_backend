package com.logistics.dto.response;

public record ComplianceCheckResponse(
        Long id,
        Long changeRequestId,
        Boolean vehicleCapacityPassed,
        Boolean driverHoursPassed,
        Boolean deliveryWindowPassed,
        Boolean vehicleTypePassed,
        Boolean temperatureRequirementPassed,
        Boolean routeSafetyPassed,
        Boolean overallPassed,
        String failureReason,
        java.time.LocalDateTime checkedAt
) {}