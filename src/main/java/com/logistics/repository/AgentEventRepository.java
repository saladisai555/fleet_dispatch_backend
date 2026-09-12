package com.logistics.repository;

import com.logistics.entity.AgentEvent;
import com.logistics.entity.enums.AgentEntityType;
import com.logistics.entity.enums.AgentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentEventRepository extends JpaRepository<AgentEvent, Long> {

    // Needed for the agent activity trail on a specific entity (e.g. all agent
    // events tied to a given DispatchChangeRequest, for audit/debugging views)
    List<AgentEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            AgentEntityType entityType, Long entityId);

    // Needed for a per-agent activity feed, paginated since this table logs
    // every agent run and grows unbounded
    List<AgentEvent> findByAgentTypeOrderByCreatedAtDesc(AgentType agentType, Pageable pageable);
}