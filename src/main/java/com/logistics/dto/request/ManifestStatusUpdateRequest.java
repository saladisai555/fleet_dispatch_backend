package com.logistics.dto.request;

import com.logistics.entity.enums.ManifestStatus;
import jakarta.validation.constraints.NotNull;

public record ManifestStatusUpdateRequest(
        @NotNull ManifestStatus status
) {}