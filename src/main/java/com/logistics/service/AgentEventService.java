package com.logistics.service;

import com.logistics.dto.AgentEventCreateRequest;
import com.logistics.dto.response.AgentEventResponse;
import com.logistics.entity.enums.AgentEntityType;
import com.logistics.entity.enums.AgentEventStatus;
import com.logistics.entity.enums.AgentType;
import com.logistics.entity.enums.EventSeverity;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AgentEventService {
    AgentEventResponse log(AgentEventCreateRequest request);
    List<AgentEventResponse> getByEntity(AgentEntityType entityType, Long entityId);
    List<AgentEventResponse> getByAgentType(AgentType agentType, Pageable pageable);
    // Added to AgentEventService interface
    List<AgentEventResponse> search(AgentType agentType, String eventType, EventSeverity severity,
                                    AgentEntityType entityType, Long entityId, AgentEventStatus status,
                                    LocalDateTime from, LocalDateTime to);
}