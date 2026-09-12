package com.logistics.dto.request;

import com.logistics.entity.enums.UserRole;
import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
        @NotNull UserRole role
) {}