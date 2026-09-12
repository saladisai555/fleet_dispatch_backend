package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Column(name = "vehicle_code", nullable = false, unique = true)
    private String vehicleCode;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    @Column(name = "capacity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityKg;

    @Column(name = "capacity_volume_m3", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityVolumeM3;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType;

    @Column(name = "refrigerated", nullable = false)
    private Boolean refrigerated;

    @Column(name = "min_temperature_c", precision = 5, scale = 2)
    private BigDecimal minTemperatureC;

    @Column(name = "max_temperature_c", precision = 5, scale = 2)
    private BigDecimal maxTemperatureC;

    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_location_id")
    private Location currentLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_driver_id")
    private Driver currentDriver;

    @Column(name = "odometer_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal odometerKm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}