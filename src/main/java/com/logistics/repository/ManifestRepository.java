package com.logistics.repository;

import com.logistics.entity.Manifest;
import com.logistics.entity.enums.ManifestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ManifestRepository extends JpaRepository<Manifest, Long> {

    Optional<Manifest> findByManifestNumber(String manifestNumber);

    boolean existsByManifestNumber(String manifestNumber);

    List<Manifest> findByStatus(ManifestStatus status);

    // Needed to check driver/vehicle conflicts before creating a new manifest
    List<Manifest> findByDriverIdAndStatusIn(Long driverId, List<ManifestStatus> statuses);

    List<Manifest> findByVehicleIdAndStatusIn(Long vehicleId, List<ManifestStatus> statuses);
}