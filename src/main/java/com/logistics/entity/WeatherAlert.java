package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "weather_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "rainfall_mm", precision = 6, scale = 2)
    private BigDecimal rainfallMm;

    // No created_at column in this table — confirmed against schema
}