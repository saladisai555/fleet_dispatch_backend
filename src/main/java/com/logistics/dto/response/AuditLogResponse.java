package com.logistics.dto.response;

import java.time.LocalDateTime;

// Read-only — audit logs are written internally by an AuditService hook,
// never created via a client-facing request DTO. Response-only.
public record AuditLogResponse(
        Long id,
        Long userId,
        String userName,
        String action,
        String entityType,
        Long entityId,
        String oldValues,
        String newValues,
        String description,
        LocalDateTime createdAt
) {}