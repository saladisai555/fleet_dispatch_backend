package com.logistics.service.impl;

import com.logistics.dto.*;
import com.logistics.dto.request.DispatchChangeRequestCreateRequest;
import com.logistics.dto.response.ComplianceCheckResponse;
import com.logistics.dto.response.DispatchChangeRequestResponse;
import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.exception.*;
import com.logistics.repository.DispatchChangeRequestRepository;
import com.logistics.repository.ManifestItemRepository;
import com.logistics.repository.RouteRepository;
import com.logistics.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchServiceImplTest {

    @Mock private DispatchChangeRequestRepository dispatchChangeRequestRepository;
    @Mock private ManifestItemRepository manifestItemRepository;
    @Mock private RouteRepository routeRepository;
    @Mock private ManifestService manifestService;
    @Mock private VehicleService vehicleService;
    @Mock private DriverService driverService;
    @Mock private UserService userService;
    @Mock private ComplianceService complianceService;
    @Mock private AgentEventService agentEventService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private DispatchServiceImpl dispatchService;

    private Manifest manifest;
    private Vehicle currentVehicle;
    private Vehicle proposedVehicle;
    private Driver currentDriver;
    private Driver proposedDriver;
    private DeliveryOrder order;
    private Location pickup;
    private Location delivery;
    private ManifestItem item;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dispatchService, "pendingExpiryHours", 24L);

        pickup = Location.builder().id(1L).name("Pickup Pt").build();
        delivery = Location.builder().id(2L).name("Delivery Pt").build();

        currentVehicle = Vehicle.builder().id(10L).vehicleCode("VH-010").status("ASSIGNED").build();
        proposedVehicle = Vehicle.builder().id(11L).vehicleCode("VH-011").status("AVAILABLE").build();

        currentDriver = Driver.builder().id(20L).name("Current Driver")
                .availabilityStatus(AvailabilityStatus.ON_DUTY).build();
        proposedDriver = Driver.builder().id(21L).name("Proposed Driver")
                .availabilityStatus(AvailabilityStatus.AVAILABLE).build();

        manifest = Manifest.builder()
                .id(30L).manifestNumber("MFT-1").vehicle(currentVehicle).driver(currentDriver)
                .status(ManifestStatus.DISPATCHED)
                .plannedStartTime(LocalDateTime.of(2026, 9, 15, 9, 0))
                .plannedEndTime(LocalDateTime.of(2026, 9, 15, 18, 0))
                .build();

        order = DeliveryOrder.builder()
                .id(40L).pickupLocation(pickup).deliveryLocation(delivery)
                .requiresRefrigeration(false)
                .deliveryWindowStart(LocalDateTime.of(2026, 9, 15, 14, 0))
                .deliveryWindowEnd(LocalDateTime.of(2026, 9, 15, 17, 0))
                .build();

        item = ManifestItem.builder().manifest(manifest).order(order).sequenceNumber(1)
                .plannedArrivalTime(LocalDateTime.of(2026, 9, 15, 15, 0)).build();
    }

    private DispatchChangeRequestCreateRequest createRequest() {
        return new DispatchChangeRequestCreateRequest(
                TriggerType.ROAD_CLOSURE, 100L, manifest.getId(),
                proposedVehicle.getId(), proposedDriver.getId(),
                "Road closure detected", "Reassign to VH-011", null,
                45, new BigDecimal("12.5"), "DISPATCH_PLANNER"
        );
    }

    private ComplianceService.ComplianceEvaluationResult passingResult() {
        return new ComplianceService.ComplianceEvaluationResult(
                true, true, true, true, true, true, true, null, List.of());
    }

    private ComplianceService.ComplianceEvaluationResult failingResult(String reason) {
        return new ComplianceService.ComplianceEvaluationResult(
                false, true, true, true, true, true, false, reason,
                List.of(new ComplianceService.ComplianceEvaluationResult.Violation("VEHICLE_CAPACITY_EXCEEDED", reason)));
    }

    private DispatchChangeRequest buildDcr(DispatchChangeStatus status) {
        return DispatchChangeRequest.builder()
                .id(50L).requestNumber("DCR-1").triggerType(TriggerType.ROAD_CLOSURE)
                .affectedManifest(manifest).proposedVehicle(proposedVehicle).proposedDriver(proposedDriver)
                .reason("Road closure").proposalSummary("Reassign")
                .estimatedDelayMinutes(45).estimatedAdditionalDistanceKm(new BigDecimal("12.5"))
                .status(status).createdByAgent("DISPATCH_PLANNER")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    private void stubResponseBuilding(DispatchChangeRequest dcr) {
        when(routeRepository.findByManifestIdAndStatus(eq(manifest.getId()), any())).thenReturn(Optional.empty());
        when(complianceService.getForChangeRequest(dcr.getId())).thenReturn(new ComplianceCheckResponse(
                1L, dcr.getId(), true, true, true, true, true, true,
                true, null, LocalDateTime.now()
        ));
    }

    // ===================== createProposal: compliance gate =====================

    @Test
    void createProposal_persistsDcr_whenComplianceGatePasses() {
        when(manifestService.findEntity(manifest.getId())).thenReturn(manifest);
        when(dispatchChangeRequestRepository.findByAffectedManifestIdAndStatus(manifest.getId(), DispatchChangeStatus.PENDING))
                .thenReturn(List.of());
        when(vehicleService.findEntity(proposedVehicle.getId())).thenReturn(proposedVehicle);
        when(driverService.findEntity(proposedDriver.getId())).thenReturn(proposedDriver);
        when(manifestItemRepository.findByManifestIdOrderBySequenceNumberAsc(manifest.getId())).thenReturn(List.of(item));
        when(complianceService.evaluate(any())).thenReturn(passingResult());

        DispatchChangeRequest saved = buildDcr(DispatchChangeStatus.PENDING);
        when(dispatchChangeRequestRepository.save(any())).thenReturn(saved);
        stubResponseBuilding(saved);

        DispatchChangeRequestResponse response = dispatchService.createProposal(createRequest());

        assertThat(response.status()).isEqualTo(DispatchChangeStatus.PENDING);
        verify(dispatchChangeRequestRepository).save(any());
        verify(complianceService).persist(eq(saved.getId()), any());
        // No compliance-failure agent event should be logged on the success path.
        verify(agentEventService, never()).log(argThat(req -> "COMPLIANCE_GATE_FAILED".equals(req.eventType())));
    }

    @Test
    void createProposal_throwsAndNeverPersists_whenComplianceGateFails() {
        when(manifestService.findEntity(manifest.getId())).thenReturn(manifest);
        when(dispatchChangeRequestRepository.findByAffectedManifestIdAndStatus(manifest.getId(), DispatchChangeStatus.PENDING))
                .thenReturn(List.of());
        when(vehicleService.findEntity(proposedVehicle.getId())).thenReturn(proposedVehicle);
        when(driverService.findEntity(proposedDriver.getId())).thenReturn(proposedDriver);
        when(manifestItemRepository.findByManifestIdOrderBySequenceNumberAsc(manifest.getId())).thenReturn(List.of(item));
        when(complianceService.evaluate(any())).thenReturn(failingResult("vehicle capacity exceeded"));

        assertThatThrownBy(() -> dispatchService.createProposal(createRequest()))
                .isInstanceOf(ComplianceGateFailedException.class)
                .hasMessageContaining("vehicle capacity exceeded");

        // This is the structural proof of Option A: a failed proposal NEVER
        // becomes a persisted row, under any circumstance.
        verify(dispatchChangeRequestRepository, never()).save(any());
        verify(complianceService, never()).persist(anyLong(), any());
        // The failure must still be discoverable via the agent event trail.
        verify(agentEventService).log(argThat(req ->
                req.agentType() == AgentType.COMPLIANCE_QA && req.status() == AgentEventStatus.FAILED));
    }

    @Test
    void createProposal_throwsDuplicate_whenManifestAlreadyHasPendingRequest() {
        when(manifestService.findEntity(manifest.getId())).thenReturn(manifest);
        when(dispatchChangeRequestRepository.findByAffectedManifestIdAndStatus(manifest.getId(), DispatchChangeStatus.PENDING))
                .thenReturn(List.of(buildDcr(DispatchChangeStatus.PENDING)));

        assertThatThrownBy(() -> dispatchService.createProposal(createRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(complianceService, never()).evaluate(any());
        verify(dispatchChangeRequestRepository, never()).save(any());
    }

    // ===================== approve: role enforcement =====================

    @Test
    void approve_succeeds_forFleetManager() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.PENDING);
        User reviewer = User.builder().id(1L).name("Manager").role(UserRole.FLEET_MANAGER).build();

        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));
        when(userService.findEntity(reviewer.getId())).thenReturn(reviewer);
        stubResponseBuilding(dcr);

        DispatchChangeRequestResponse response = dispatchService.approve(dcr.getId(), reviewer.getId(), "Looks good");

        assertThat(response.status()).isEqualTo(DispatchChangeStatus.APPROVED);
        assertThat(dcr.getReviewedBy()).isEqualTo(reviewer);
        verify(auditLogService).record(eq(reviewer.getId()), eq("DCR_APPROVED"), any(), any(), any(), any(), any());
    }

    @Test
    void approve_throwsUnauthorized_forSafetyManager() {
        // Corrected, documentation-authoritative rule: SAFETY_MANAGER cannot
        // approve, even though it was the original (superseded) instruction.
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.PENDING);
        User reviewer = User.builder().id(2L).name("Safety").role(UserRole.SAFETY_MANAGER).build();

        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));
        when(userService.findEntity(reviewer.getId())).thenReturn(reviewer);

        assertThatThrownBy(() -> dispatchService.approve(dcr.getId(), reviewer.getId(), "comment"))
                .isInstanceOf(UnauthorizedOperationException.class);

        assertThat(dcr.getStatus()).isEqualTo(DispatchChangeStatus.PENDING); // unchanged
    }

    @Test
    void approve_throwsInvalidStateTransition_whenNotPending() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.EXECUTED);
        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));

        assertThatThrownBy(() -> dispatchService.approve(dcr.getId(), 1L, "comment"))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(userService, never()).findEntity(anyLong()); // fails before even resolving the reviewer
    }

    @Test
    void approve_autoExpires_whenPendingPastTtl() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.PENDING);
        dcr.setCreatedAt(LocalDateTime.now().minusHours(25)); // TTL is 24h
        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));

        assertThatThrownBy(() -> dispatchService.approve(dcr.getId(), 1L, "comment"))
                .isInstanceOf(InvalidStateTransitionException.class);

        assertThat(dcr.getStatus()).isEqualTo(DispatchChangeStatus.EXPIRED);
        verify(auditLogService).record(isNull(), eq("DCR_EXPIRED"), any(), any(), any(), any(), any());
    }

    // ===================== reject =====================

    @Test
    void reject_succeeds_forDispatcher_andMakesNoOperationalChange() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.PENDING);
        User reviewer = User.builder().id(3L).name("Dispatcher").role(UserRole.DISPATCHER).build();

        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));
        when(userService.findEntity(reviewer.getId())).thenReturn(reviewer);
        stubResponseBuilding(dcr);

        DispatchChangeRequestResponse response = dispatchService.reject(dcr.getId(), reviewer.getId(), "Not feasible");

        assertThat(response.status()).isEqualTo(DispatchChangeStatus.REJECTED);
        assertThat(manifest.getVehicle()).isEqualTo(currentVehicle); // untouched
        assertThat(manifest.getDriver()).isEqualTo(currentDriver);   // untouched
    }

    // ===================== execute: five gates =====================

    @Test
    void execute_throws_whenNotApproved() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.PENDING);
        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));

        assertThatThrownBy(() -> dispatchService.execute(dcr.getId()))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("must be APPROVED");
    }

    @Test
    void execute_throws_whenComplianceNoLongerPasses() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.APPROVED);
        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));
        when(complianceService.recheck(dcr.getId())).thenReturn(
                new ComplianceCheckResponse(1L, dcr.getId(), false, true, true, true,true, true, false, "capacity now exceeded", LocalDateTime.now()));

        assertThatThrownBy(() -> dispatchService.execute(dcr.getId()))
                .isInstanceOf(ComplianceGateFailedException.class);

        assertThat(dcr.getStatus()).isEqualTo(DispatchChangeStatus.APPROVED); // left retryable, not corrupted
    }

    @Test
    void execute_throwsStaleProposal_whenManifestNoLongerExecutable() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.APPROVED);
        manifest.setStatus(ManifestStatus.CANCELLED); // changed since proposal was made
        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));
        when(complianceService.recheck(dcr.getId())).thenReturn(passingCompliance(dcr));

        assertThatThrownBy(() -> dispatchService.execute(dcr.getId()))
                .isInstanceOf(StaleProposalException.class);
    }

    @Test
    void execute_throwsStaleProposal_whenProposedVehicleNowInBreakdown() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.APPROVED);
        proposedVehicle.setStatus("BREAKDOWN"); // changed since proposal was made
        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));
        when(complianceService.recheck(dcr.getId())).thenReturn(passingCompliance(dcr));

        assertThatThrownBy(() -> dispatchService.execute(dcr.getId()))
                .isInstanceOf(StaleProposalException.class)
                .hasMessageContaining("BREAKDOWN");
    }

    @Test
    void execute_throwsStaleProposal_whenProposedDriverNoLongerAvailable() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.APPROVED);
        proposedDriver.setAvailabilityStatus(AvailabilityStatus.ON_LEAVE); // changed since proposal
        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));
        when(complianceService.recheck(dcr.getId())).thenReturn(passingCompliance(dcr));

        assertThatThrownBy(() -> dispatchService.execute(dcr.getId()))
                .isInstanceOf(StaleProposalException.class);
    }

    @Test
    void execute_appliesOperationalChange_andMarksExecuted_whenAllGatesPass() {
        DispatchChangeRequest dcr = buildDcr(DispatchChangeStatus.APPROVED);
        when(dispatchChangeRequestRepository.findByIdForUpdate(dcr.getId())).thenReturn(Optional.of(dcr));
        when(complianceService.recheck(dcr.getId())).thenReturn(passingCompliance(dcr));
        stubResponseBuilding(dcr);

        DispatchChangeRequestResponse response = dispatchService.execute(dcr.getId());

        assertThat(response.status()).isEqualTo(DispatchChangeStatus.EXECUTED);
        assertThat(manifest.getVehicle()).isEqualTo(proposedVehicle); // actually mutated
        assertThat(manifest.getDriver()).isEqualTo(proposedDriver);
        verify(auditLogService).record(any(), eq("DCR_EXECUTED"), any(), any(), any(), any(), any());
        verify(agentEventService).log(argThat(req -> "DISPATCH_CHANGE_EXECUTED".equals(req.eventType())));
    }

    private ComplianceCheckResponse passingCompliance(DispatchChangeRequest dcr) {return new ComplianceCheckResponse(
            1L, dcr.getId(), true, true, true, true, true, true,
            true, null, LocalDateTime.now()
    );
    }
}