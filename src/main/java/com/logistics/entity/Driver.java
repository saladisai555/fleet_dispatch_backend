package com.logistics.entity;

import com.logistics.entity.enums.AvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;

    @Column(name = "license_type", nullable = false)
    private String licenseType;

    @Column(name = "license_expiry", nullable = false)
    private LocalDate licenseExpiry;

    @Column(name = "max_driving_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxDrivingHours;

    @Column(name = "current_driving_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal currentDrivingHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    private AvailabilityStatus availabilityStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_location_id")
    private Location currentLocation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}