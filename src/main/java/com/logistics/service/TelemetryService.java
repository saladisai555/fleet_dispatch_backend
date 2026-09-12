package com.logistics.service;

import com.logistics.dto.request.VehicleTelemetryRequest;
import com.logistics.dto.response.VehicleTelemetryResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TelemetryService {
    VehicleTelemetryResponse ingest(VehicleTelemetryRequest request);
    Optional<VehicleTelemetryResponse> getLatestForVehicle(Long vehicleId);
    List<VehicleTelemetryResponse> getHistory(Long vehicleId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    List<VehicleTelemetryResponse> getRecentTemperatureAlerts(Long vehicleId, Pageable pageable);
}