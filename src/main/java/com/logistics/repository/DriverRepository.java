package com.logistics.repository;

import com.logistics.entity.Driver;
import com.logistics.entity.enums.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    // Needed for driver login/profile-linking lookups
    Optional<Driver> findByEmployeeCode(String employeeCode);

    Optional<Driver> findByUserId(Long userId);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByPhone(String phone);

    boolean existsByLicenseNumber(String licenseNumber);

    // Core to the Dispatch Planner: find drivers who could realistically be reassigned
    List<Driver> findByAvailabilityStatus(AvailabilityStatus availabilityStatus);
}