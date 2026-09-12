package com.logistics.dto.response;

import com.logistics.entity.enums.OrderPriority;
import com.logistics.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryOrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        String customerName,
        Long pickupLocationId,
        String pickupLocationName,
        Long deliveryLocationId,
        String deliveryLocationName,
        String cargoDescription,
        BigDecimal weightKg,
        BigDecimal volumeM3,
        OrderPriority priority,
        Boolean requiresRefrigeration,
        BigDecimal requiredTemperatureMinC,
        BigDecimal requiredTemperatureMaxC,
        LocalDateTime deliveryWindowStart,
        LocalDateTime deliveryWindowEnd,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}