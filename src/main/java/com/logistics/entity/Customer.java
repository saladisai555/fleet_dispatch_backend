package com.logistics.entity;

import com.logistics.entity.enums.CustomerPriority;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", nullable = false, unique = true)
    private String customerCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email")
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false)
    private CustomerPriority priorityLevel;

    @Column(name = "requires_temperature_control", nullable = false)
    private Boolean requiresTemperatureControl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}