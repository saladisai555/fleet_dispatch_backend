package com.logistics.entity;

import com.logistics.entity.enums.ManifestItemStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manifest_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manifest_id", nullable = false)
    private Manifest manifest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private DeliveryOrder order;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "planned_arrival_time", nullable = false)
    private LocalDateTime plannedArrivalTime;

    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ManifestItemStatus status;
}