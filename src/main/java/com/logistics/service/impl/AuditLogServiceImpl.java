package com.logistics.service.impl;

import com.logistics.dto.response.AuditLogResponse;
import com.logistics.entity.AuditLog;
import com.logistics.entity.User;
import com.logistics.repository.AuditLogRepository;
import com.logistics.service.AuditLogService;
import com.logistics.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserService userService;

    @Override
    @Transactional
    public AuditLogResponse record(Long userId, String action, String entityType, Long entityId,
                                   String oldValues, String newValues, String description) {
        User user = userId != null ? userService.findEntity(userId) : null;

        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(auditLogRepository.save(log));
    }

    @Override
    public List<AuditLogResponse> getByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<AuditLogResponse> getByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return new AuditLogResponse(
                a.getId(), a.getUser() != null ? a.getUser().getId() : null,
                a.getUser() != null ? a.getUser().getName() : null,
                a.getAction(), a.getEntityType(), a.getEntityId(),
                a.getOldValues(), a.getNewValues(), a.getDescription(), a.getCreatedAt()
        );
    }
}