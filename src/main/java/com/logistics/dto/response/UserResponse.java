package com.logistics.dto.response;

import com.logistics.entity.enums.UserRole;
import com.logistics.entity.enums.UserStatus;
import java.time.LocalDateTime;

// Never includes passwordHash — that field never leaves the backend
public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {}