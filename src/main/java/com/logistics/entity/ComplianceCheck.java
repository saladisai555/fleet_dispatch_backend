package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Treated as 1:1 by business logic (DB permits multiple; workflow implies one).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private DispatchChangeRequest changeRequest;

    @Column(name = "vehicle_capacity_passed", nullable = false)
    private Boolean vehicleCapacityPassed;

    @Column(name = "driver_hours_passed", nullable = false)
    private Boolean driverHoursPassed;

    @Column(name = "delivery_window_passed", nullable = false)
    private Boolean deliveryWindowPassed;

    @Column(name = "vehicle_type_passed", nullable = false)
    private Boolean vehicleTypePassed;

    @Column(name = "temperature_requirement_passed", nullable = false)
    private Boolean temperatureRequirementPassed;

    @Column(name = "route_safety_passed", nullable = false)
    private Boolean routeSafetyPassed;

    @Column(name = "overall_passed", nullable = false)
    private Boolean overallPassed;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;
}