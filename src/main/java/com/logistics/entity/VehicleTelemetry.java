package com.logistics.entity;

import com.logistics.entity.enums.EngineStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_telemetry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "speed_kmh", nullable = false, precision = 6, scale = 2)
    private BigDecimal speedKmh;

    @Column(name = "fuel_level_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal fuelLevelPercent;

    @Column(name = "engine_temperature_c", precision = 5, scale = 2)
    private BigDecimal engineTemperatureC;

    @Column(name = "cargo_temperature_c", precision = 5, scale = 2)
    private BigDecimal cargoTemperatureC;

    @Column(name = "odometer_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal odometerKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_status", nullable = false)
    private EngineStatus engineStatus;

    @Column(name = "temperature_alert", nullable = false)
    private Boolean temperatureAlert;

    @Column(name = "engine_fault_code")
    private String engineFaultCode;
}