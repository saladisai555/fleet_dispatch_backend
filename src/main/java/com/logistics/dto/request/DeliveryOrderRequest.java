package com.logistics.dto.request;

import com.logistics.entity.enums.OrderPriority;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Mirrors chk_orders_temperature, chk_orders_different_locations, chk_orders_delivery_window
// as much as Bean Validation can express — the cross-field parts (temperature pairing,
// window ordering, pickup != delivery) still need service-layer validation, noted below.
public record DeliveryOrderRequest(
        @NotNull Long customerId,
        @NotNull Long pickupLocationId,
        @NotNull Long deliveryLocationId,

        @NotBlank String cargoDescription,

        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal volumeM3,

        @NotNull OrderPriority priority,

        @NotNull Boolean requiresRefrigeration,
        BigDecimal requiredTemperatureMinC, // required together iff requiresRefrigeration=true
        BigDecimal requiredTemperatureMaxC,

        @NotNull @Future LocalDateTime deliveryWindowStart,
        @NotNull @Future LocalDateTime deliveryWindowEnd
) {}