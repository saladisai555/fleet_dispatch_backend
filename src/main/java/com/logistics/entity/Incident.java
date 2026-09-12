package com.logistics.entity;

import com.logistics.entity.enums.IncidentSeverity;
import com.logistics.entity.enums.IncidentStatus;
import com.logistics.entity.enums.IncidentType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false)
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private IncidentSeverity severity;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expected_end_at")
    private LocalDateTime expectedEndAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IncidentStatus status;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}