package com.logistics.repository;

import com.logistics.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Needed for "show history of this record" views — e.g. audit trail for
    // a specific DispatchChangeRequest or DeliveryOrder
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId);

    // Needed for "what has this user done" views, paginated since this table
    // logs every mutating action and grows unbounded
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}