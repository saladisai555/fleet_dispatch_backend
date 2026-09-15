package com.logistics.dto.request;

import com.logistics.dto.response.ComplianceQaResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ---- AI INTEGRATION BOUNDARY DTOs (Part I) ----

// Sent FROM Spring Boot TO the AI service. Deliberately structured and
// pre-filtered — the AI never receives raw entity dumps or open DB access.
public record OperationalContextRequest(
        Long manifestId,
        TriggerSummary trigger,
        AffectedOrderSummary affectedOrder,
        CurrentAssignmentSummary currentAssignment,
        List<CandidateVehicleSummary> candidateVehicles,
        List<CandidateDriverSummary> candidateDrivers,
        List<WeatherSummary> relevantWeather,               // new - Section 23
        ComplianceQaResponse preliminaryComplianceCheck
) {
    public record TriggerSummary(
            String triggerType,
            Long triggerId,
            String description,
            String severity
    ) {}

    public record AffectedOrderSummary(
            Long orderId,
            String orderNumber,
            BigDecimal weightKg,
            BigDecimal volumeM3,
            Boolean requiresRefrigeration,
            BigDecimal requiredTemperatureMinC,
            BigDecimal requiredTemperatureMaxC,
            LocalDateTime deliveryWindowStart,
            LocalDateTime deliveryWindowEnd
    ) {}

    public record CurrentAssignmentSummary(
            Long vehicleId,
            String vehicleCode,
            Long driverId,
            String driverName,
            Long routeId,
            BigDecimal currentDistanceKm
    ) {}

    public record CandidateVehicleSummary(
            Long vehicleId,
            String vehicleCode,
            BigDecimal availableCapacityKg,
            BigDecimal availableCapacityVolumeM3,
            Boolean refrigerated,
            String currentLocationName
    ) {}

    public record CandidateDriverSummary(
            Long driverId,
            String name,
            BigDecimal remainingDrivingHours,
            String currentLocationName
    ) {}
    public record WeatherSummary(String alertType, String severity, String description) {}
}