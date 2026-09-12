package com.logistics.repository;

import com.logistics.entity.ComplianceCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplianceCheckRepository extends JpaRepository<ComplianceCheck, Long> {

    // Treated as 1:1 with DispatchChangeRequest per the Step 5 mapping decision
    Optional<ComplianceCheck> findByChangeRequestId(Long changeRequestId);

    boolean existsByChangeRequestId(Long changeRequestId);
}