package com.logistics.dto.request;

import jakarta.validation.constraints.NotBlank;

// Body for POST /dispatch-requests/{id}/reject
// reviewComments is required here - every rejection needs a recorded reason,
// consistent with every documented rejection example carrying an explanation.
public record DispatchChangeRejectRequest(
        @NotBlank String reviewComments
) {}