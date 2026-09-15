package com.logistics.service;

import com.logistics.dto.request.ComplianceEvaluateRequest;
import com.logistics.dto.response.ComplianceCheckResponse;
import com.logistics.dto.response.ComplianceQaResponse;
import com.logistics.entity.DeliveryOrder;
import com.logistics.entity.Driver;
import com.logistics.entity.Manifest;
import com.logistics.entity.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ComplianceService {

    /**
     * Pure, non-persisting evaluation of a candidate dispatch change.
     * Used by DispatchService BEFORE a DispatchChangeRequest is created,
     * since compliance gates DCR creation (confirmed Batch-4 decision #1 /
     * Project Context doc Section 18: "Compliance acts as a gate before an
     * actionable DCR is created").
     */
    ComplianceEvaluationResult evaluate(ComplianceEvaluationRequest request);

    /**
     * Persists a ComplianceCheck row tied to an already-created DispatchChangeRequest.
     * Called once, immediately after DispatchService creates the DCR (using the
     * same result that gated its creation) - this is what satisfies the
     * compliance_checks.change_request_id FK and backs GET .../compliance.
     */
    ComplianceCheckResponse persist(Long changeRequestId, ComplianceEvaluationResult result);

    /**
     * Re-runs compliance against a DCR's current stored proposal and updates
     * its ComplianceCheck row. Backs POST /dispatch-requests/{id}/compliance/check
     * (API_Specification.md Section 16.2) - used for re-verification, including
     * as part of the execute() stale-proposal gate.
     */
    ComplianceCheckResponse recheck(Long changeRequestId);

    ComplianceCheckResponse getForChangeRequest(Long changeRequestId);
    // Added to ComplianceService interface
    ComplianceQaResponse evaluateCandidate(ComplianceEvaluateRequest request); // dry-run, non-persisting
    /**
     * Input carrier for evaluate(). Deliberately holds entities, not DTOs -
     * this is an internal service-to-service contract (DispatchService ->
     * ComplianceService), not an API boundary.
     */
    record ComplianceEvaluationRequest(
            Manifest affectedManifest,
            Vehicle proposedVehicle,
            Driver proposedDriver,
            DeliveryOrder primaryOrder,
            BigDecimal proposedAdditionalDrivingHours,
            LocalDateTime proposedEta,
            List<Long> relevantLocationIds // locations to check for active critical incidents
    ) {}

    // ComplianceService interface - ComplianceEvaluationResult upgraded
    record ComplianceEvaluationResult(
            boolean vehicleCapacityPassed,
            boolean driverHoursPassed,
            boolean deliveryWindowPassed,
            boolean vehicleTypePassed,
            boolean temperatureRequirementPassed,
            boolean routeSafetyPassed,
            boolean overallPassed,
            String failureReason,           // kept - still backs compliance_checks.failure_reason (TEXT column)
            List<Violation> violations      // new - structured, agent-consumption-ready
    ) {
        public record Violation(String code, String message) {}
    }
}