package com.logistics.dto.response;

import com.logistics.entity.enums.CustomerPriority;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String customerCode,
        String name,
        String contactName,
        String phone,
        String email,
        Long locationId,
        String locationName, // flattened
        CustomerPriority priorityLevel,
        Boolean requiresTemperatureControl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}