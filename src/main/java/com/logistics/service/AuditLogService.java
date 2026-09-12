package com.logistics.service;

import com.logistics.dto.response.AuditLogResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditLogService {

    /**
     * Internal-only write path - never exposed via a client-facing request DTO.
     * Called by DispatchService (and other services) whenever an operational
     * state change or human decision needs to be recorded.
     */
    AuditLogResponse record(Long userId, String action, String entityType, Long entityId,
                            String oldValues, String newValues, String description);

    List<AuditLogResponse> getByEntity(String entityType, Long entityId);
    List<AuditLogResponse> getByUser(Long userId, Pageable pageable);
}