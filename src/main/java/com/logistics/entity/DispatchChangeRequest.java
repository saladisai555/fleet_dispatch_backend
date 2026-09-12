package com.logistics.entity;

import com.logistics.entity.enums.DispatchChangeStatus;
import com.logistics.entity.enums.TriggerType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true)
    private String requestNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    // Polymorphic pointer — no FK in DB. Resolves against incidents/weather_alerts/
    // vehicle_telemetry.id depending on triggerType. Do NOT map as @ManyToOne.
    @Column(name = "trigger_id")
    private Long triggerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affected_manifest_id", nullable = false)
    private Manifest affectedManifest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_vehicle_id")
    private Vehicle proposedVehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_driver_id")
    private Driver proposedDriver;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "proposal_summary", nullable = false, columnDefinition = "TEXT")
    private String proposalSummary;

    @Column(name = "route_changes", columnDefinition = "TEXT")
    private String routeChanges;

    @Column(name = "estimated_delay_minutes", nullable = false)
    private Integer estimatedDelayMinutes;

    @Column(name = "estimated_additional_distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedAdditionalDistanceKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DispatchChangeStatus status;

    @Column(name = "created_by_agent", nullable = false)
    private String createdByAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}