package com.logistics.repository;

import com.logistics.entity.DispatchChangeRequest;
import com.logistics.entity.enums.DispatchChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DispatchChangeRequestRepository extends JpaRepository<DispatchChangeRequest, Long> {

    Optional<DispatchChangeRequest> findByRequestNumber(String requestNumber);

    boolean existsByRequestNumber(String requestNumber);

    // Core to the Fleet Manager dashboard: the human approval queue
    List<DispatchChangeRequest> findByStatus(DispatchChangeStatus status);

    // Needed to check for an existing open request against a manifest before
    // creating a duplicate (idempotency at the workflow level)
    List<DispatchChangeRequest> findByAffectedManifestIdAndStatus(
            Long affectedManifestId, DispatchChangeStatus status);

    List<DispatchChangeRequest> findByReviewedById(Long reviewedById);
    // Added to DispatchChangeRequestRepository

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DispatchChangeRequest d WHERE d.id = :id")
    Optional<DispatchChangeRequest> findByIdForUpdate(@Param("id") Long id);
}