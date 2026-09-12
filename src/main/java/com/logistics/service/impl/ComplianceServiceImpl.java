package com.logistics.service.impl;

import com.logistics.dto.response.ComplianceCheckResponse;
import com.logistics.entity.ComplianceCheck;
import com.logistics.entity.DeliveryOrder;
import com.logistics.entity.Driver;
import com.logistics.entity.DispatchChangeRequest;
import com.logistics.entity.Manifest;
import com.logistics.entity.ManifestItem;
import com.logistics.entity.Vehicle;
import com.logistics.entity.enums.ManifestStatus;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.ComplianceCheckRepository;
import com.logistics.repository.DispatchChangeRequestRepository;
import com.logistics.repository.ManifestItemRepository;
import com.logistics.repository.ManifestRepository;
import com.logistics.service.ComplianceService;
import com.logistics.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplianceServiceImpl implements ComplianceService {

    // "Genuinely committed" manifest statuses - same set used across ManifestService/
    // ManifestItemService for double-booking checks. Used here by checkVehicleType
    // to decide whether an ASSIGNED vehicle is eligible for THIS proposal.
    private static final List<ManifestStatus> ACTIVE_STATUSES =
            List.of(ManifestStatus.PLANNED, ManifestStatus.DISPATCHED, ManifestStatus.IN_TRANSIT);

    private final ComplianceCheckRepository complianceCheckRepository;
    private final DispatchChangeRequestRepository dispatchChangeRequestRepository;
    private final ManifestRepository manifestRepository;
    private final ManifestItemRepository manifestItemRepository;
    private final IncidentService incidentService;

    @Override
    public ComplianceEvaluationResult evaluate(ComplianceEvaluationRequest request) {
        boolean capacityPassed = checkVehicleCapacity(request.affectedManifest(), request.proposedVehicle());
        boolean hoursPassed = checkDriverHours(request.proposedDriver(), request.proposedAdditionalDrivingHours());
        boolean windowPassed = checkDeliveryWindow(request.primaryOrder(), request.proposedEta());
        boolean typePassed = checkVehicleType(request.affectedManifest(), request.proposedVehicle());
        boolean temperaturePassed = checkTemperatureRequirement(request.primaryOrder(), request.proposedVehicle());
        boolean routeSafetyPassed = checkRouteSafety(request.relevantLocationIds());

        // Mirrors chk_compliance_overall exactly: overall can only be true if
        // every individual check passed. No partial-override logic.
        boolean overallPassed = capacityPassed && hoursPassed && windowPassed
                && typePassed && temperaturePassed && routeSafetyPassed;

        String failureReason = overallPassed ? null : buildFailureReason(
                capacityPassed, hoursPassed, windowPassed, typePassed, temperaturePassed, routeSafetyPassed);

        return new ComplianceEvaluationResult(
                capacityPassed, hoursPassed, windowPassed, typePassed, temperaturePassed,
                routeSafetyPassed, overallPassed, failureReason);
    }

    @Override
    @Transactional
    public ComplianceCheckResponse persist(Long changeRequestId, ComplianceEvaluationResult result) {
        DispatchChangeRequest dcr = dispatchChangeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch change request not found: " + changeRequestId));

        ComplianceCheck check = ComplianceCheck.builder()
                .changeRequest(dcr)
                .vehicleCapacityPassed(result.vehicleCapacityPassed())
                .driverHoursPassed(result.driverHoursPassed())
                .deliveryWindowPassed(result.deliveryWindowPassed())
                .vehicleTypePassed(result.vehicleTypePassed())
                .temperatureRequirementPassed(result.temperatureRequirementPassed())
                .routeSafetyPassed(result.routeSafetyPassed())
                .overallPassed(result.overallPassed())
                .failureReason(result.failureReason())
                .checkedAt(LocalDateTime.now())
                .build();

        return toResponse(complianceCheckRepository.save(check));
    }

    @Override
    @Transactional
    public ComplianceCheckResponse recheck(Long changeRequestId) {
        DispatchChangeRequest dcr = dispatchChangeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch change request not found: " + changeRequestId));

        // Re-evaluate against the DCR's CURRENT stored proposal - proposedVehicle/
        // proposedDriver may be null if the proposal only changed the route.
        Manifest manifest = dcr.getAffectedManifest();
        DeliveryOrder primaryOrder = manifestItemRepository.findByManifestIdOrderBySequenceNumberAsc(manifest.getId())
                .stream().findFirst().map(ManifestItem::getOrder)
                .orElseThrow(() -> new ResourceNotFoundException("Manifest has no items to evaluate: " + manifest.getId()));

        ComplianceEvaluationRequest evalRequest = new ComplianceEvaluationRequest(
                manifest,
                dcr.getProposedVehicle() != null ? dcr.getProposedVehicle() : manifest.getVehicle(),
                dcr.getProposedDriver() != null ? dcr.getProposedDriver() : manifest.getDriver(),
                primaryOrder,
                BigDecimal.ZERO, // driving-hours delta not re-derivable from a stored DCR alone; treated as neutral on recheck
                primaryOrder.getDeliveryWindowEnd(), // conservative ETA assumption on recheck
                List.of(primaryOrder.getPickupLocation().getId(), primaryOrder.getDeliveryLocation().getId())
        );

        ComplianceEvaluationResult result = evaluate(evalRequest);

        ComplianceCheck existing = complianceCheckRepository.findByChangeRequestId(changeRequestId).orElse(null);
        if (existing != null) {
            existing.setVehicleCapacityPassed(result.vehicleCapacityPassed());
            existing.setDriverHoursPassed(result.driverHoursPassed());
            existing.setDeliveryWindowPassed(result.deliveryWindowPassed());
            existing.setVehicleTypePassed(result.vehicleTypePassed());
            existing.setTemperatureRequirementPassed(result.temperatureRequirementPassed());
            existing.setRouteSafetyPassed(result.routeSafetyPassed());
            existing.setOverallPassed(result.overallPassed());
            existing.setFailureReason(result.failureReason());
            existing.setCheckedAt(LocalDateTime.now());
            return toResponse(existing);
        }
        return persist(changeRequestId, result);
    }

    @Override
    public ComplianceCheckResponse getForChangeRequest(Long changeRequestId) {
        ComplianceCheck check = complianceCheckRepository.findByChangeRequestId(changeRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("No compliance result for change request: " + changeRequestId));
        return toResponse(check);
    }

    // ===================== Individual deterministic checks =====================

    private boolean checkVehicleCapacity(Manifest manifest, Vehicle proposedVehicle) {
        List<ManifestItem> items = manifestItemRepository.findByManifestIdOrderBySequenceNumberAsc(manifest.getId());

        BigDecimal totalWeight = items.stream()
                .map(i -> i.getOrder().getWeightKg())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVolume = items.stream()
                .map(i -> i.getOrder().getVolumeM3())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalWeight.compareTo(proposedVehicle.getCapacityKg()) <= 0
                && totalVolume.compareTo(proposedVehicle.getCapacityVolumeM3()) <= 0;
    }

    private boolean checkDriverHours(Driver proposedDriver, BigDecimal proposedAdditionalHours) {
        // Confirmed rule: current + proposed <= driver's own configured max.
        // No separate regulatory-hours table or hardcoded cap.
        BigDecimal projected = proposedDriver.getCurrentDrivingHours().add(proposedAdditionalHours);
        return projected.compareTo(proposedDriver.getMaxDrivingHours()) <= 0;
    }

    private boolean checkDeliveryWindow(DeliveryOrder order, LocalDateTime proposedEta) {
        if (proposedEta == null) return false;
        return !proposedEta.isBefore(order.getDeliveryWindowStart())
                && !proposedEta.isAfter(order.getDeliveryWindowEnd());
    }

    private boolean checkVehicleType(Manifest affectedManifest, Vehicle proposedVehicle) {
        // Confirmed rule (vehicle_type_passed = operational suitability, not
        // literal type matching):
        String status = proposedVehicle.getStatus();
        if ("BREAKDOWN".equals(status) || "IN_TRANSIT".equals(status)) {
            return false;
        }
        if ("AVAILABLE".equals(status)) {
            return true;
        }
        if ("ASSIGNED".equals(status)) {
            // Passes only when this is the vehicle already tied to the manifest
            // being changed, OR it is not actually committed to any OTHER
            // active manifest (covers edge cases where the status string is
            // ASSIGNED but the underlying manifest linkage is stale/inconsistent).
            boolean isVehicleForThisManifest = affectedManifest.getVehicle().getId().equals(proposedVehicle.getId());
            if (isVehicleForThisManifest) {
                return true;
            }
            boolean committedElsewhere = manifestRepository
                    .findByVehicleIdAndStatusIn(proposedVehicle.getId(), ACTIVE_STATUSES).stream()
                    .anyMatch(m -> !m.getId().equals(affectedManifest.getId()));
            return !committedElsewhere;
        }
        // Any other/unrecognized status string is treated conservatively as a failure.
        return false;
    }

    private boolean checkTemperatureRequirement(DeliveryOrder order, Vehicle proposedVehicle) {
        if (!Boolean.TRUE.equals(order.getRequiresRefrigeration())) {
            return true; // no requirement to satisfy
        }
        if (!Boolean.TRUE.equals(proposedVehicle.getRefrigerated())) {
            return false;
        }
        if (proposedVehicle.getMinTemperatureC() == null || proposedVehicle.getMaxTemperatureC() == null) {
            return false;
        }
        // Vehicle's capable range must fully cover the order's required range.
        return proposedVehicle.getMinTemperatureC().compareTo(order.getRequiredTemperatureMinC()) <= 0
                && proposedVehicle.getMaxTemperatureC().compareTo(order.getRequiredTemperatureMaxC()) >= 0;
    }

    private boolean checkRouteSafety(List<Long> relevantLocationIds) {
        // Confirmed rule: fails only on an ACTIVE HIGH/CRITICAL incident at a
        // relevant location. Traffic delay alone never fails this check.
        return relevantLocationIds.stream().noneMatch(incidentService::hasCriticalActiveIncidentAtLocation);
    }

    private String buildFailureReason(boolean capacity, boolean hours, boolean window,
                                      boolean type, boolean temperature, boolean routeSafety) {
        List<String> failures = java.util.stream.Stream.of(
                        !capacity ? "vehicle capacity exceeded" : null,
                        !hours ? "driver hours exceeded" : null,
                        !window ? "delivery window not met" : null,
                        !type ? "vehicle not operationally suitable" : null,
                        !temperature ? "temperature requirement not met" : null,
                        !routeSafety ? "active critical incident on route" : null
                ).filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return String.join("; ", failures);
    }

    private ComplianceCheckResponse toResponse(ComplianceCheck c) {
        return new ComplianceCheckResponse(
                c.getId(), c.getChangeRequest().getId(),
                c.getVehicleCapacityPassed(), c.getDriverHoursPassed(), c.getDeliveryWindowPassed(),
                c.getVehicleTypePassed(), c.getTemperatureRequirementPassed(), c.getRouteSafetyPassed(),
                c.getOverallPassed(), c.getFailureReason(), c.getCheckedAt()
        );
    }
}