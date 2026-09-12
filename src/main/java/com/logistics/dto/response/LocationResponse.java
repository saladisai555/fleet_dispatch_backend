package com.logistics.dto.response;

import com.logistics.entity.enums.LocationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocationResponse(
        Long id,
        String name,
        LocationType locationType,
        String address,
        String city,
        String district,
        String state,
        String pincode,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime createdAt
) {}