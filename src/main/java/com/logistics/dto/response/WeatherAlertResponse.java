package com.logistics.dto.response;

import java.math.BigDecimal;

// No createdAt field — confirmed in Step 5 that weather_alerts has none
public record WeatherAlertResponse(
        Long id,
        Long locationId,
        String locationName,
        String alertType,
        String severity,
        String description,
        BigDecimal temperatureC,
        BigDecimal rainfallMm
) {}