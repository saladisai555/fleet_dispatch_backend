package com.logistics.dto.request;

import com.logistics.entity.enums.CustomerPriority;
import jakarta.validation.constraints.*;

public record CustomerRequest(
        @NotBlank String customerCode,
        @NotBlank String name,
        @NotBlank String contactName,
        @NotBlank String phone,

        @Email String email, // nullable per schema

        @NotNull Long locationId,

        @NotNull CustomerPriority priorityLevel,

        @NotNull Boolean requiresTemperatureControl
) {}