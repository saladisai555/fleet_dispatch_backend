package com.logistics.entity;

import com.logistics.entity.enums.LocationType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private LocationType locationType;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "district", nullable = false)
    private String district;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "pincode", nullable = false)
    private String pincode;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}