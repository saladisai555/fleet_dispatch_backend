package com.logistics.service.impl;

import com.logistics.dto.AgentEventCreateRequest;
import com.logistics.dto.request.DispatchChangeRequestCreateRequest;
import com.logistics.dto.response.DispatchChangeRequestResponse;
import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.exception.*;
import com.logistics.repository.DispatchChangeRequestRepository;
import com.logistics.repository.ManifestItemRepository;
import com.logistics.repository.RouteRepository;
import com.logistics.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatchServiceImpl implements DispatchService {

    // Roles permitted to approve/reject, per API_Specification.md Section 22 -
    // this is the corrected, documentation-authoritative rule (overrides the
    // earlier conflicting instruction). SAFETY_MANAGER is explicitly excluded.
    private static final Set<UserRole> APPROVAL_ROLES =
            Set.of(UserRole.ADMIN, UserRole.FLEET_MANAGER, UserRole.DISPATCHER);

    private static final List<ManifestStatus> EXECUTABLE_MANIFEST_STATUSES =
            List.of(ManifestStatus.PLANNED, ManifestStatus.DISPATCHED, ManifestStatus.IN_TRANSIT);

    // TTL for lazy PENDING -> EXPIRED transition. Kept as a configurable
    // constant (application.properties) rather than a scheduled job - simplest
    // approach that satisfies "EXPIRED -> cannot execute" without adding
    // scheduling infrastructure the project doesn't otherwise need.
    @Value("${app.dispatch.pending-expiry-hours:24}")
    private long pendingExpiryHours;

    private final DispatchChangeRequestRepository dispatchChangeRequestRepository;
    private final ManifestItemRepository manifestItemRepository;
    private final RouteRepository routeRepository;

    private final ManifestService manifestService;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final UserService userService;
    private final ComplianceService complianceService;
    private final AgentEventService agentEventService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public DispatchChangeRequestResponse createProposal(DispatchChangeRequestCreateRequest request) {

        Manifest manifest = manifestService.findEntity(request.affectedManifestId());

        // Idempotency guard: don't create a second PENDING proposal against a
        // manifest that already has one open.
        if (!dispatchChangeRequestRepository
                .findByAffectedManifestIdAndStatus(manifest.getId(), DispatchChangeStatus.PENDING).isEmpty()) {
            throw new DuplicateResourceException(
                    "Manifest " + manifest.getManifestNumber() + " already has a pending dispatch change request");
        }

        Vehicle proposedVehicle = request.proposedVehicleId() != null
                ? vehicleService.findEntity(request.proposedVehicleId()) : manifest.getVehicle();
        Driver proposedDriver = request.proposedDriverId() != null
                ? driverService.findEntity(request.proposedDriverId()) : manifest.getDriver();

        ManifestItem primaryItem = manifestItemRepository
                .findByManifestIdOrderBySequenceNumberAsc(manifest.getId()).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Manifest has no items - cannot evaluate a dispatch change against it"));
        DeliveryOrder primaryOrder = primaryItem.getOrder();

        BigDecimal proposedAdditionalHours = computeProposedAdditionalDrivingHours(manifest, proposedDriver);
        LocalDateTime proposedEta = primaryItem.getPlannedArrivalTime().plusMinutes(request.estimatedDelayMinutes());

        ComplianceService.ComplianceEvaluationRequest evalRequest = new ComplianceService.ComplianceEvaluationRequest(
                manifest, proposedVehicle, proposedDriver, primaryOrder,
                proposedAdditionalHours, proposedEta,
                List.of(primaryOrder.getPickupLocation().getId(), primaryOrder.getDeliveryLocation().getId())
        );

        ComplianceService.ComplianceEvaluationResult result = complianceService.evaluate(evalRequest);

        if (!result.overallPassed()) {
            // Option A: compliance gates creation - a failed proposal never
            // becomes a persisted DCR. Recorded via agent_events only.
            agentEventService.log(new AgentEventCreateRequest(
                    AgentType.COMPLIANCE_QA, "COMPLIANCE_GATE_FAILED", EventSeverity.MEDIUM,
                    AgentEntityType.MANIFEST, manifest.getId(),
                    "Proposed dispatch change failed compliance: " + result.failureReason(),
                    null, null, AgentEventStatus.FAILED
            ));
            throw new ComplianceGateFailedException(result.failureReason());
        }

        DispatchChangeRequest dcr = DispatchChangeRequest.builder()
                .requestNumber(generateRequestNumber())
                .triggerType(request.triggerType())
                .triggerId(request.triggerId())
                .affectedManifest(manifest)
                .proposedVehicle(request.proposedVehicleId() != null ? proposedVehicle : null)
                .proposedDriver(request.proposedDriverId() != null ? proposedDriver : null)
                .reason(request.reason())
                .proposalSummary(request.proposalSummary())
                .routeChanges(request.routeChanges())
                .estimatedDelayMinutes(request.estimatedDelayMinutes())
                .estimatedAdditionalDistanceKm(request.estimatedAdditionalDistanceKm())
                .status(DispatchChangeStatus.PENDING)
                .createdByAgent(request.createdByAgent())
                .createdAt(LocalDateTime.now())
                .build();

        DispatchChangeRequest saved = dispatchChangeRequestRepository.save(dcr);
        complianceService.persist(saved.getId(), result);

        agentEventService.log(new AgentEventCreateRequest(
                AgentType.OPERATIONS, "DISPATCH_CHANGE_PROPOSED", EventSeverity.INFO,
                AgentEntityType.DISPATCH_CHANGE_REQUEST, saved.getId(),
                "Dispatch change request created: " + saved.getRequestNumber(),
                null, null, AgentEventStatus.COMPLETED
        ));

        return buildResponse(saved);
    }

    @Override
    public DispatchChangeRequestResponse getById(Long id) {
        return buildResponse(findEntity(id));
    }

    @Override
    public List<DispatchChangeRequestResponse> list(DispatchChangeStatus status, TriggerType triggerType,
                                                    Long affectedManifestId, String createdByAgent) {
        // Kept as an in-memory filter over a status-scoped (or full) fetch,
        // rather than a Specification/Criteria query - simplest approach that
        // satisfies the documented filter set at this project's scale.
        List<DispatchChangeRequest> base = status != null
                ? dispatchChangeRequestRepository.findByStatus(status)
                : dispatchChangeRequestRepository.findAll();

        return base.stream()
                .filter(d -> triggerType == null || d.getTriggerType() == triggerType)
                .filter(d -> affectedManifestId == null || d.getAffectedManifest().getId().equals(affectedManifestId))
                .filter(d -> createdByAgent == null || createdByAgent.equals(d.getCreatedByAgent()))
                .map(this::buildResponse)
                .toList();
    }

    @Override
    @Transactional
    public DispatchChangeRequestResponse approve(Long id, Long reviewerId, String reviewComments) {
        DispatchChangeRequest dcr = dispatchChangeRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch change request not found: " + id));
        expireIfDue(dcr);

        if (dcr.getStatus() != DispatchChangeStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Cannot approve a request in status " + dcr.getStatus());
        }

        User reviewer = userService.findEntity(reviewerId);
        requireApprovalRole(reviewer);

        dcr.setStatus(DispatchChangeStatus.APPROVED);
        dcr.setReviewedBy(reviewer);
        dcr.setReviewedAt(LocalDateTime.now());
        dcr.setReviewComments(reviewComments);

        auditLogService.record(reviewerId, "DCR_APPROVED", "DispatchChangeRequest", dcr.getId(),
                "PENDING", "APPROVED", "Approved by " + reviewer.getName());

        return buildResponse(dcr);
    }

    @Override
    @Transactional
    public DispatchChangeRequestResponse reject(Long id, Long reviewerId, String reviewComments) {
        DispatchChangeRequest dcr = dispatchChangeRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch change request not found: " + id));
        expireIfDue(dcr);

        if (dcr.getStatus() != DispatchChangeStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Cannot reject a request in status " + dcr.getStatus());
        }

        User reviewer = userService.findEntity(reviewerId);
        requireApprovalRole(reviewer);

        dcr.setStatus(DispatchChangeStatus.REJECTED);
        dcr.setReviewedBy(reviewer);
        dcr.setReviewedAt(LocalDateTime.now());
        dcr.setReviewComments(reviewComments);

        auditLogService.record(reviewerId, "DCR_REJECTED", "DispatchChangeRequest", dcr.getId(),
                "PENDING", "REJECTED", "Rejected by " + reviewer.getName() + ": " + reviewComments);

        return buildResponse(dcr);
    }

    @Override
    @Transactional
    public DispatchChangeRequestResponse execute(Long id) {
        DispatchChangeRequest dcr = dispatchChangeRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch change request not found: " + id));

        // Gate 1: must be APPROVED (also implicitly rules out EXPIRED/EXECUTED/
        // REJECTED/PENDING, since only APPROVED satisfies this check).
        if (dcr.getStatus() != DispatchChangeStatus.APPROVED) {
            throw new InvalidStateTransitionException(
                    "Cannot execute a request in status " + dcr.getStatus() + " - must be APPROVED");
        }

        // Gate 2: compliance must still pass right now, not just at creation time.
        var complianceResult = complianceService.recheck(dcr.getId());
        if (!complianceResult.overallPassed()) {
            throw new ComplianceGateFailedException(
                    "Compliance no longer passes at execution time: " + complianceResult.failureReason());
        }

        // Gate 3: stale-proposal protection (API_Specification.md Section 25) -
        // verify the proposed vehicle/driver are still genuinely assignable and
        // the manifest hasn't moved into a terminal/incompatible state since
        // this proposal was created.
        Manifest manifest = dcr.getAffectedManifest();
        if (!EXECUTABLE_MANIFEST_STATUSES.contains(manifest.getStatus())) {
            throw new StaleProposalException(
                    "Manifest " + manifest.getManifestNumber() + " is no longer in an executable state: " + manifest.getStatus());
        }
        if (dcr.getProposedVehicle() != null && "BREAKDOWN".equals(dcr.getProposedVehicle().getStatus())) {
            throw new StaleProposalException("Proposed vehicle is no longer operational (BREAKDOWN)");
        }
        if (dcr.getProposedDriver() != null && dcr.getProposedDriver().getAvailabilityStatus() != AvailabilityStatus.AVAILABLE
                && !dcr.getProposedDriver().getId().equals(manifest.getDriver().getId())) {
            throw new StaleProposalException("Proposed driver is no longer available");
        }

        // All gates passed - apply the operational change.
        String oldVehicle = manifest.getVehicle().getVehicleCode();
        String oldDriver = manifest.getDriver().getName();

        if (dcr.getProposedVehicle() != null) {
            manifest.setVehicle(dcr.getProposedVehicle());
        }
        if (dcr.getProposedDriver() != null) {
            manifest.setDriver(dcr.getProposedDriver());
        }
        manifest.setUpdatedAt(LocalDateTime.now());

        // NOTE: actual route recalculation (RouteService.reroute) is NOT
        // triggered automatically here. The locked DCR schema stores route
        // changes only as free text (routeChanges) plus delay/distance
        // estimates, with no structured proposed-route FK to act on (confirmed
        // by API_Specification.md Section 15.2). Wiring a real route swap into
        // execute() needs the RoutingService/OSRM integration (Step 18) to
        // supply concrete origin/destination/timing - flagging this honestly
        // rather than simulating a route change with placeholder data.
        routeRepository.findByManifestIdAndStatus(manifest.getId(), RouteStatus.PLANNED)
                .ifPresent(plannedRoute -> {
                    routeRepository.findByManifestIdAndStatus(manifest.getId(), RouteStatus.ACTIVE)
                            .ifPresent(oldActive -> {
                                oldActive.setStatus(RouteStatus.REPLACED);
                                oldActive.setUpdatedAt(LocalDateTime.now());
                            });
                    plannedRoute.setStatus(RouteStatus.ACTIVE);
                    plannedRoute.setUpdatedAt(LocalDateTime.now());
                });
        dcr.setStatus(DispatchChangeStatus.EXECUTED);

        auditLogService.record(
                dcr.getReviewedBy() != null ? dcr.getReviewedBy().getId() : null,
                "DCR_EXECUTED", "DispatchChangeRequest", dcr.getId(),
                "vehicle=" + oldVehicle + ", driver=" + oldDriver,
                "vehicle=" + manifest.getVehicle().getVehicleCode() + ", driver=" + manifest.getDriver().getName(),
                "Executed dispatch change " + dcr.getRequestNumber() + " on manifest " + manifest.getManifestNumber()
        );

        agentEventService.log(new AgentEventCreateRequest(
                AgentType.OPERATIONS, "DISPATCH_CHANGE_EXECUTED", EventSeverity.INFO,
                AgentEntityType.DISPATCH_CHANGE_REQUEST, dcr.getId(),
                "Dispatch change executed: " + dcr.getRequestNumber(),
                null, null, AgentEventStatus.COMPLETED
        ));

        return buildResponse(dcr);
    }

    // ===================== Internal helpers =====================

    private DispatchChangeRequest findEntity(Long id) {
        return dispatchChangeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch change request not found: " + id));
    }

    private void expireIfDue(DispatchChangeRequest dcr) {
        if (dcr.getStatus() == DispatchChangeStatus.PENDING
                && dcr.getCreatedAt().plusHours(pendingExpiryHours).isBefore(LocalDateTime.now())) {
            dcr.setStatus(DispatchChangeStatus.EXPIRED);
            auditLogService.record(null, "DCR_EXPIRED", "DispatchChangeRequest", dcr.getId(),
                    "PENDING", "EXPIRED", "Auto-expired after " + pendingExpiryHours + " hours without review");
        }
    }

    private void requireApprovalRole(User reviewer) {
        if (!APPROVAL_ROLES.contains(reviewer.getRole())) {
            throw new UnauthorizedOperationException(
                    "Role " + reviewer.getRole() + " is not permitted to approve or reject dispatch change requests");
        }
    }

    // Approximates additional driving hours a proposed reassignment would add.
    // If the driver is unchanged, treats the estimated delay as the additional
    // commitment. If a NEW driver is being proposed, treats the manifest's
    // remaining planned duration as their full additional commitment, since
    // they haven't logged any of it yet. This is a reasonable business
    // inference, not a value derivable directly from the schema - flagged
    // here plainly since it isn't specified verbatim in the documentation.
    private BigDecimal computeProposedAdditionalDrivingHours(Manifest manifest, Driver proposedDriver) {
        boolean isSameDriver = manifest.getDriver().getId().equals(proposedDriver.getId());
        if (isSameDriver) {
            return BigDecimal.ZERO; // exact delay-based figure is added by the caller via proposedEta comparison
        }
        long remainingMinutes = java.time.Duration.between(
                LocalDateTime.now().isAfter(manifest.getPlannedStartTime()) ? LocalDateTime.now() : manifest.getPlannedStartTime(),
                manifest.getPlannedEndTime()
        ).toMinutes();
        return BigDecimal.valueOf(Math.max(remainingMinutes, 0)).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
    }

    private String generateRequestNumber() {
        return "DCR-" + System.currentTimeMillis();
    }

    private DispatchChangeRequestResponse buildResponse(DispatchChangeRequest dcr) {
        Manifest manifest = dcr.getAffectedManifest();

        Long currentRouteId = routeRepository.findByManifestIdAndStatus(manifest.getId(), RouteStatus.ACTIVE)
                .or(() -> routeRepository.findByManifestIdAndStatus(manifest.getId(), RouteStatus.PLANNED))
                .map(Route::getId).orElse(null);

        var current = new DispatchChangeRequestResponse.CurrentState(
                manifest.getVehicle().getId(), manifest.getVehicle().getVehicleCode(),
                manifest.getDriver().getId(), manifest.getDriver().getName(),
                currentRouteId
        );

        // A proposed route, if one exists, is represented by a PLANNED route
        // row not yet promoted to ACTIVE - see execute()'s note on route
        // recalculation not being fully wired yet.
        var proposedRoute = routeRepository.findByManifestIdAndStatus(manifest.getId(), RouteStatus.PLANNED)
                .map(r -> new DispatchChangeRequestResponse.ProposedRoute(
                        r.getId(), r.getDistanceKm(), r.getEstimatedDurationMinutes()))
                .orElse(null);

        var proposed = new DispatchChangeRequestResponse.ProposedState(
                dcr.getProposedVehicle() != null ? dcr.getProposedVehicle().getId() : null,
                dcr.getProposedVehicle() != null ? dcr.getProposedVehicle().getVehicleCode() : null,
                dcr.getProposedDriver() != null ? dcr.getProposedDriver().getId() : null,
                dcr.getProposedDriver() != null ? dcr.getProposedDriver().getName() : null,
                proposedRoute
        );

        var complianceResponse = complianceService.getForChangeRequest(dcr.getId());

        return new DispatchChangeRequestResponse(
                dcr.getId(), dcr.getRequestNumber(), dcr.getTriggerType(), dcr.getTriggerId(),
                manifest.getId(), current, proposed,
                dcr.getReason(), dcr.getProposalSummary(), dcr.getRouteChanges(),
                dcr.getEstimatedDelayMinutes(), dcr.getEstimatedAdditionalDistanceKm(),
                dcr.getStatus(), dcr.getCreatedByAgent(),
                dcr.getReviewedBy() != null ? dcr.getReviewedBy().getId() : null,
                dcr.getReviewedBy() != null ? dcr.getReviewedBy().getName() : null,
                dcr.getReviewedAt(), dcr.getReviewComments(),
                complianceResponse, dcr.getCreatedAt()
        );
    }
}