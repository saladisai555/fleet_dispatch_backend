package com.logistics.dto.response;

import com.logistics.service.ComplianceService;

import java.util.List;
import java.util.Map;

// Matches Agent_Architecture.md Section 18 exactly - status is a string
// ("PASS"/"FAIL"), checks is a per-name PASS/FAIL map, not raw booleans.
// This is the agent-consumption shape; ComplianceCheckResponse (boolean
// fields) remains the human/frontend-facing shape used elsewhere.
public record ComplianceQaResponse(
        String status,
        List<ViolationDto> violations,
        Map<String, String> checks
) {
    public record ViolationDto(String code, String message) {}

    public static ComplianceQaResponse from(ComplianceService.ComplianceEvaluationResult result) {
        Map<String, String> checks = Map.of(
                "vehicleCapacity", result.vehicleCapacityPassed() ? "PASS" : "FAIL",
                "driverHours", result.driverHoursPassed() ? "PASS" : "FAIL",
                "deliveryWindow", result.deliveryWindowPassed() ? "PASS" : "FAIL",
                "vehicleType", result.vehicleTypePassed() ? "PASS" : "FAIL",
                "temperature", result.temperatureRequirementPassed() ? "PASS" : "FAIL",
                "routeSafety", result.routeSafetyPassed() ? "PASS" : "FAIL"
        );
        List<ViolationDto> violations = result.violations().stream()
                .map(v -> new ViolationDto(v.code(), v.message()))
                .toList();
        return new ComplianceQaResponse(result.overallPassed() ? "PASS" : "FAIL", violations, checks);
    }
}