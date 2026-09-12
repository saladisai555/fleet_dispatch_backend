package com.logistics.dto;

import com.logistics.entity.enums.AgentEntityType;
import com.logistics.entity.enums.AgentType;
import com.logistics.entity.enums.EventSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Internal DTO used by services to log agent activity — not a public
// controller endpoint. Kept small deliberately: input_data/output_data
// are optional so we don't force every log call to serialize a payload.
public record AgentEventCreateRequest(
        @NotNull AgentType agentType,
        @NotBlank String eventType,
        @NotNull EventSeverity severity,

        AgentEntityType entityType, // nullable
        Long entityId,               // nullable

        @NotBlank String message,

        String inputData,  // nullable, kept short per Rule 16 (don't store
        String outputData, // huge AI conversation data without clear reason)

        @NotNull com.logistics.entity.enums.AgentEventStatus status
) {}