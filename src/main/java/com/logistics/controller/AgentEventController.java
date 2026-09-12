package com.logistics.controller;

import com.logistics.dto.response.AgentEventResponse;
import com.logistics.entity.enums.AgentEntityType;
import com.logistics.entity.enums.AgentEventStatus;
import com.logistics.entity.enums.AgentType;
import com.logistics.entity.enums.EventSeverity;
import com.logistics.service.AgentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agent-events")
@RequiredArgsConstructor
public class AgentEventController {

    private final AgentEventService agentEventService;

    @GetMapping
    public ResponseEntity<List<AgentEventResponse>> list(
            @RequestParam(required = false) AgentType agentType,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) EventSeverity severity,
            @RequestParam(required = false) AgentEntityType entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) AgentEventStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        return ResponseEntity.ok(agentEventService.search(
                agentType, eventType, severity, entityType, entityId, status, from, to));
    }
}