package com.logistics.entity;

import com.logistics.entity.enums.AgentEntityType;
import com.logistics.entity.enums.AgentEventStatus;
import com.logistics.entity.enums.AgentType;
import com.logistics.entity.enums.EventSeverity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AgentType agentType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private EventSeverity severity;

    // Polymorphic pointer, no FK — resolved via entityType + entityId at query time.
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private AgentEntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AgentEventStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}