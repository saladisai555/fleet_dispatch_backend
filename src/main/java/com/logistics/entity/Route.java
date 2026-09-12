package com.logistics.entity;

import com.logistics.entity.enums.RouteStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manifest_id", nullable = false)
    private Manifest manifest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_location_id", nullable = false)
    private Location originLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id", nullable = false)
    private Location destinationLocation;

    @Column(name = "route_geometry", columnDefinition = "TEXT")
    private String routeGeometry;

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "planned_start_time", nullable = false)
    private LocalDateTime plannedStartTime;

    @Column(name = "planned_end_time", nullable = false)
    private LocalDateTime plannedEndTime;

    @Column(name = "traffic_delay_minutes", nullable = false)
    private Integer trafficDelayMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RouteStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}