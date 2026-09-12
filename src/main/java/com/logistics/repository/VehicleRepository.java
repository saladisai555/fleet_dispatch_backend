package com.logistics.repository;

import com.logistics.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVehicleCode(String vehicleCode);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    boolean existsByVehicleCode(String vehicleCode);

    boolean existsByRegistrationNumber(String registrationNumber);

    // status kept as String per schema (no DB-level enum constraint found)
    List<Vehicle> findByStatus(String status);

    // Core to Dispatch Planner: find candidate vehicles for reassignment that
    // meet minimum capacity, are refrigerated if required, and are in a usable status.
    @Query("""
           SELECT v FROM Vehicle v
           WHERE v.status = :status
           AND v.capacityKg >= :minCapacityKg
           AND v.capacityVolumeM3 >= :minCapacityVolumeM3
           AND (:requiresRefrigeration = false OR v.refrigerated = true)
           """)
    List<Vehicle> findAvailableCandidates(
            @Param("status") String status,
            @Param("minCapacityKg") BigDecimal minCapacityKg,
            @Param("minCapacityVolumeM3") BigDecimal minCapacityVolumeM3,
            @Param("requiresRefrigeration") boolean requiresRefrigeration
    );
}