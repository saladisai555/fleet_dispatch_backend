package com.logistics.controller;

import com.logistics.dto.*;
import com.logistics.dto.request.ComplianceEvaluateRequest;
import com.logistics.dto.request.DispatchChangeRequestCreateRequest;
import com.logistics.dto.response.ComplianceQaResponse;
import com.logistics.dto.response.DispatchChangeRequestResponse;
import com.logistics.service.ComplianceService;
import com.logistics.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Distinct from DispatchChangeRequestController deliberately - this is the
// controlled AI/agent-tool boundary (Agent_Architecture.md Section 20/22),
// not the human-facing dispatch API. A human user never POSTs here directly;
// only an internal agent pipeline or trusted external AI service does.
//
// SECURITY NOTE: like /api/v1/events/**, this boundary needs API-key or
// service-account authentication distinct from user JWT roles - deferred to
// Step 18 alongside the other external-integration hardening, consistent
// with how /events/** was already flagged. NOT yet locked down beyond
// standard JWT auth, which is a known, explicitly-flagged gap until then.
@RestController
@RequestMapping("/api/v1/agent-tools")
@RequiredArgsConstructor
public class AgentToolsController {

    private final ComplianceService complianceService;
    private final DispatchService dispatchService;

    @PostMapping("/compliance/evaluate")
    public ResponseEntity<ComplianceQaResponse> evaluateCandidate(
            @Valid @RequestBody ComplianceEvaluateRequest request) {
        return ResponseEntity.ok(complianceService.evaluateCandidate(request));
    }

    @PostMapping("/dispatch-proposals")
    public ResponseEntity<DispatchChangeRequestResponse> createProposal(
            @Valid @RequestBody DispatchChangeRequestCreateRequest request) {
        // Reuses the EXACT SAME compliance-gated createProposal() path as
        // everything else - this is the structural proof that an AI-originated
        // proposal cannot bypass compliance. There is no separate "trusted AI"
        // code path anywhere in DispatchService.
        return ResponseEntity.status(HttpStatus.CREATED).body(dispatchService.createProposal(request));
    }
}