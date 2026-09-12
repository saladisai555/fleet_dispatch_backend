package com.logistics.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

// alertType and severity are plain Strings here (no DB enum constraint found
// on weather_alerts, per Step 5 note) — validated as non-blank only, not
// against a fixed set, since the DB itself doesn't restrict them either.
public record WeatherAlertRequest(
        @NotNull Long locationId,

        @NotBlank String alertType,
        @NotBlank String severity,

        @NotBlank String description,

        BigDecimal temperatureC, // nullable
        BigDecimal rainfallMm    // nullable
) {}