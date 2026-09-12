package com.logistics.entity;

import com.logistics.entity.enums.ManifestStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manifests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manifest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manifest_number", nullable = false, unique = true)
    private String manifestNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_location_id", nullable = false)
    private Location startLocation;

    @Column(name = "planned_start_time", nullable = false)
    private LocalDateTime plannedStartTime;

    @Column(name = "planned_end_time", nullable = false)
    private LocalDateTime plannedEndTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ManifestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}