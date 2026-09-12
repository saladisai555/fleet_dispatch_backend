package com.logistics.dto.response;

import com.logistics.entity.enums.AgentEntityType;
import com.logistics.entity.enums.AgentType;
import com.logistics.entity.enums.AgentEventStatus;
import com.logistics.entity.enums.EventSeverity;
import java.time.LocalDateTime;

public record AgentEventResponse(
        Long id,
        AgentType agentType,
        String eventType,
        EventSeverity severity,
        AgentEntityType entityType,
        Long entityId,
        String message,
        String inputData,
        String outputData,
        AgentEventStatus status,
        LocalDateTime createdAt
) {}