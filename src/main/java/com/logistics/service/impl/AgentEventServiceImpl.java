package com.logistics.service.impl;

import com.logistics.dto.AgentEventCreateRequest;
import com.logistics.dto.response.AgentEventResponse;
import com.logistics.entity.AgentEvent;
import com.logistics.entity.enums.AgentEntityType;
import com.logistics.entity.enums.AgentEventStatus;
import com.logistics.entity.enums.AgentType;
import com.logistics.entity.enums.EventSeverity;
import com.logistics.repository.AgentEventRepository;
import com.logistics.service.AgentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentEventServiceImpl implements AgentEventService {

    private final AgentEventRepository agentEventRepository;

    @Override
    @Transactional
    public AgentEventResponse log(AgentEventCreateRequest request) {
        AgentEvent event = AgentEvent.builder()
                .agentType(request.agentType())
                .eventType(request.eventType())
                .severity(request.severity())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .message(request.message())
                .inputData(request.inputData())
                .outputData(request.outputData())
                .status(request.status())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(agentEventRepository.save(event));
    }

    @Override
    public List<AgentEventResponse> getByEntity(AgentEntityType entityType, Long entityId) {
        return agentEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<AgentEventResponse> getByAgentType(AgentType agentType, Pageable pageable) {
        return agentEventRepository.findByAgentTypeOrderByCreatedAtDesc(agentType, pageable)
                .stream().map(this::toResponse).toList();
    }
    // Added to AgentEventServiceImpl
    @Override
    public List<AgentEventResponse> search(AgentType agentType, String eventType, EventSeverity severity,
                                           AgentEntityType entityType, Long entityId, AgentEventStatus status,
                                           LocalDateTime from, LocalDateTime to) {
        return agentEventRepository.findAll().stream()
                .filter(e -> agentType == null || e.getAgentType() == agentType)
                .filter(e -> eventType == null || eventType.equalsIgnoreCase(e.getEventType()))
                .filter(e -> severity == null || e.getSeverity() == severity)
                .filter(e -> entityType == null || e.getEntityType() == entityType)
                .filter(e -> entityId == null || entityId.equals(e.getEntityId()))
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> from == null || !e.getCreatedAt().isBefore(from))
                .filter(e -> to == null || !e.getCreatedAt().isAfter(to))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }
    private AgentEventResponse toResponse(AgentEvent e) {
        return new AgentEventResponse(
                e.getId(), e.getAgentType(), e.getEventType(), e.getSeverity(),
                e.getEntityType(), e.getEntityId(), e.getMessage(), e.getInputData(), e.getOutputData(),
                e.getStatus(), e.getCreatedAt()
        );
    }
}