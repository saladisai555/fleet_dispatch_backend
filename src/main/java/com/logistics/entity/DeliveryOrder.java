package com.logistics.entity;

import com.logistics.entity.enums.OrderPriority;
import com.logistics.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_location_id", nullable = false)
    private Location pickupLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_location_id", nullable = false)
    private Location deliveryLocation;

    @Column(name = "cargo_description", nullable = false)
    private String cargoDescription;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumeM3;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private OrderPriority priority;

    @Column(name = "requires_refrigeration", nullable = false)
    private Boolean requiresRefrigeration;

    @Column(name = "required_temperature_min_c", precision = 5, scale = 2)
    private BigDecimal requiredTemperatureMinC;

    @Column(name = "required_temperature_max_c", precision = 5, scale = 2)
    private BigDecimal requiredTemperatureMaxC;

    @Column(name = "delivery_window_start", nullable = false)
    private LocalDateTime deliveryWindowStart;

    @Column(name = "delivery_window_end", nullable = false)
    private LocalDateTime deliveryWindowEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}