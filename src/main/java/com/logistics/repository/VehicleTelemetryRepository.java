package com.logistics.repository;

import com.logistics.entity.VehicleTelemetry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface VehicleTelemetryRepository extends JpaRepository<VehicleTelemetry, Long> {

    // Core to the Listener Agent: "where is this vehicle right now" — per the
    // GPS-distinction rule, this is the authoritative live-position lookup,
    // never vehicles.current_location_id.
    Optional<VehicleTelemetry> findFirstByVehicleIdOrderByRecordedAtDesc(Long vehicleId);

    // Needed for telemetry history views / charts, with pagination since this
    // table grows unbounded
    List<VehicleTelemetry> findByVehicleIdAndRecordedAtBetween(
            Long vehicleId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Needed for cold-chain monitoring: any recent temperature-alert readings
    List<VehicleTelemetry> findByVehicleIdAndTemperatureAlertTrueOrderByRecordedAtDesc(
            Long vehicleId, Pageable pageable);
    // Added to VehicleTelemetryRepository
    List<VehicleTelemetry> findByVehicleIdAndRecordedAtAfterOrderByRecordedAtDesc(Long vehicleId, LocalDateTime after);
}