package com.logistics.dto.request;

import com.logistics.entity.enums.LocationType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LocationRequest(
        @NotBlank String name,
        @NotNull LocationType locationType,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String district,
        @NotBlank String state,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "pincode must be 6 digits") String pincode,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude
) {}