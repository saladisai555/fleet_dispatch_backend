package com.logistics.controller;

import com.logistics.dto.*;
import com.logistics.dto.request.DispatchChangeApproveRequest;
import com.logistics.dto.request.DispatchChangeRejectRequest;
import com.logistics.dto.response.ComplianceCheckResponse;
import com.logistics.dto.response.DispatchChangeRequestResponse;
import com.logistics.entity.enums.DispatchChangeStatus;
import com.logistics.entity.enums.TriggerType;
import com.logistics.service.ComplianceService;
import com.logistics.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch-requests")
@RequiredArgsConstructor
public class DispatchChangeRequestController {

    private final DispatchService dispatchService;
    private final ComplianceService complianceService;

    @GetMapping
    public ResponseEntity<List<DispatchChangeRequestResponse>> list(
            @RequestParam(required = false) DispatchChangeStatus status,
            @RequestParam(required = false) TriggerType triggerType,
            @RequestParam(required = false) Long affectedManifestId,
            @RequestParam(required = false) String createdByAgent) {
        return ResponseEntity.ok(dispatchService.list(status, triggerType, affectedManifestId, createdByAgent));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<DispatchChangeRequestResponse> getById(@PathVariable Long requestId) {
        return ResponseEntity.ok(dispatchService.getById(requestId));
    }

    // POST /dispatch-requests - deliberately NOT exposed as a public
    // client-facing endpoint. Per the project's own architecture (Section 5/6
    // of the original brief), a DCR is only ever produced by the internal
    // detect->diagnose->plan->compliance pipeline (DispatchService.createProposal),
    // triggered by an agent run or an internal dispatcher action - never by
    // an arbitrary external POST constructing a proposal from scratch. This
    // is intentional, not an oversight.

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','FLEET_MANAGER','DISPATCHER')")
    public ResponseEntity<DispatchChangeRequestResponse> approve(
            @PathVariable Long requestId,
            @Valid @RequestBody DispatchChangeApproveRequest request,
            Authentication authentication) {

        Long reviewerId = resolveCurrentUserId(authentication);

        // approve() and execute() are called as two separate calls here -
        // NOT via internal self-invocation - so each goes through the Spring
        // proxy and gets its own independent @Transactional boundary. This
        // is what makes "approval must trigger execution" (doc requirement)
        // compatible with "failed execution does not leave partial changes
        // and does not erase the recorded approval" (also a doc requirement).
        dispatchService.approve(requestId, reviewerId, request.reviewComments());
        return ResponseEntity.ok(dispatchService.execute(requestId));
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','FLEET_MANAGER','DISPATCHER')")
    public ResponseEntity<DispatchChangeRequestResponse> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody DispatchChangeRejectRequest request,
            Authentication authentication) {

        Long reviewerId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(dispatchService.reject(requestId, reviewerId, request.reviewComments()));
    }

    @PostMapping("/{requestId}/execute")
    // No role restriction per API_Specification.md Section 22 - execution is
    // "Backend" for every role, i.e. not a human-initiated action at all.
    // This endpoint exists for backend-controlled/retry scenarios (Section
    // 15.5), not for a human to call directly - it will be locked down to
    // an internal/service-account credential once Step 12's security model
    // is finalized, rather than left open to any authenticated human role.
    public ResponseEntity<DispatchChangeRequestResponse> execute(@PathVariable Long requestId) {
        return ResponseEntity.ok(dispatchService.execute(requestId));
    }

    @GetMapping("/{requestId}/compliance")
    public ResponseEntity<ComplianceCheckResponse> getCompliance(@PathVariable Long requestId) {
        return ResponseEntity.ok(complianceService.getForChangeRequest(requestId));
    }

    @PostMapping("/{requestId}/compliance/check")
    @PreAuthorize("hasAnyRole('ADMIN','FLEET_MANAGER','DISPATCHER','SAFETY_MANAGER')")
    public ResponseEntity<ComplianceCheckResponse> runComplianceCheck(@PathVariable Long requestId) {
        return ResponseEntity.ok(complianceService.recheck(requestId));
    }

    // PLACEHOLDER pending Step 12. Once a custom UserDetails implementation
    // wraps our User entity, this resolves to authentication.getPrincipal()'s
    // real user id. Throwing here (rather than trusting a client-supplied id)
    // is deliberate - accepting an unauthenticated claim of identity for an
    // approval/rejection action would be a genuine security hole.
    // Replaces the UnsupportedOperationException placeholder from Step 10
    private Long resolveCurrentUserId(Authentication authentication) {
        var userDetails = (com.logistics.security.CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }
}