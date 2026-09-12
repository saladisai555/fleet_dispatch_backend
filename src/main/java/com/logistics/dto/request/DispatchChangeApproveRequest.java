package com.logistics.dto.request;

// Body for POST /dispatch-requests/{id}/approve
// reviewComments is optional here - an approval doesn't strictly need justification,
// though the service may still record it if provided.
public record DispatchChangeApproveRequest(
        String reviewComments
) {}