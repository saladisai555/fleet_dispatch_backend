package com.logistics.service;

import com.logistics.dto.request.DispatchChangeRequestCreateRequest;
import com.logistics.dto.response.DispatchChangeRequestResponse;
import com.logistics.entity.enums.DispatchChangeStatus;
import com.logistics.entity.enums.TriggerType;

import java.util.List;

public interface DispatchService {

    /**
     * Compliance-gated creation (Option A): evaluates the proposal first;
     * only persists a DispatchChangeRequest (status PENDING) if compliance
     * passes overall. Throws if compliance fails - no DCR row is ever
     * created for a failed proposal.
     */
    DispatchChangeRequestResponse createProposal(DispatchChangeRequestCreateRequest request);

    DispatchChangeRequestResponse getById(Long id);

    List<DispatchChangeRequestResponse> list(DispatchChangeStatus status, TriggerType triggerType,
                                             Long affectedManifestId, String createdByAgent);

    /**
     * PENDING -> APPROVED only. Does NOT execute the operational change -
     * see class-level note on why approve/execute are separate transactions.
     * reviewerId must belong to a role permitted to approve (ADMIN,
     * FLEET_MANAGER, DISPATCHER per API_Specification.md Section 22).
     */
    DispatchChangeRequestResponse approve(Long id, Long reviewerId, String reviewComments);

    /**
     * PENDING -> REJECTED. No operational state mutation. Same role rule as approve().
     */
    DispatchChangeRequestResponse reject(Long id, Long reviewerId, String reviewComments);

    /**
     * APPROVED -> EXECUTED. Re-validates all five gates (approved / compliance
     * still passing / not expired / not already executed / current state still
     * matches the proposal - the stale-proposal check) before mutating
     * operational data. Its own atomic transaction, independent of approve().
     */
    DispatchChangeRequestResponse execute(Long id);
}