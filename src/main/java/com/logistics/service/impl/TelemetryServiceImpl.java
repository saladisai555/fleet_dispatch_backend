package com.logistics.service.impl;

import com.logistics.dto.AgentEventCreateRequest;
import com.logistics.dto.request.VehicleTelemetryRequest;
import com.logistics.dto.response.VehicleTelemetryResponse;
import com.logistics.entity.Vehicle;
import com.logistics.entity.VehicleTelemetry;
import com.logistics.entity.enums.*;
import com.logistics.repository.VehicleTelemetryRepository;
import com.logistics.service.AgentEventService;
import com.logistics.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelemetryServiceImpl implements TelemetryService {

    // Cold-chain alert threshold check: an order's temperature range is
    // compared elsewhere (ComplianceService); here we only decide whether
    // THIS reading itself should be flagged as a temperature_alert based on
    // the vehicle's own configured min/max — a reading outside the vehicle's
    // own refrigeration range is an operational fault regardless of any order.
    private final VehicleTelemetryRepository telemetryRepository;
    private final VehicleRepository vehicleRepository;
    private final AgentEventService agentEventService;

    // TelemetryServiceImpl - dedup window added
    @Value("${app.telemetry.dedup-window-seconds:30}")
    private long dedupWindowSeconds;

    // TelemetryServiceImpl.ingest - updated to use client-supplied recordedAt
// and to ignore the client-supplied temperatureAlert in favor of the
// server-computed value (request.temperatureAlert() is intentionally unused).
    @Override
    @Transactional
    public VehicleTelemetryResponse ingest(VehicleTelemetryRequest request) {

        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found: " + request.vehicleId()
                        ));

        LocalDateTime windowStart = request.recordedAt().minusSeconds(dedupWindowSeconds);
        boolean isDuplicate = telemetryRepository
                .findByVehicleIdAndRecordedAtAfterOrderByRecordedAtDesc(vehicle.getId(), windowStart).stream()
                .anyMatch(t -> t.getOdometerKm().compareTo(request.odometerKm()) == 0
                        && t.getLatitude().compareTo(request.latitude()) == 0
                        && t.getLongitude().compareTo(request.longitude()) == 0);

        if (isDuplicate) {
            return telemetryRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicle.getId())
                    .map(this::toResponse).orElseThrow();
        }

        boolean temperatureAlert = computeTemperatureAlert(vehicle, request.cargoTemperatureC());

        VehicleTelemetry telemetry = VehicleTelemetry.builder()
                .vehicle(vehicle)
                .recordedAt(request.recordedAt())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .speedKmh(request.speedKmh())
                .fuelLevelPercent(request.fuelLevelPercent())
                .engineTemperatureC(request.engineTemperatureC())
                .cargoTemperatureC(request.cargoTemperatureC())
                .odometerKm(request.odometerKm())
                .engineStatus(request.engineStatus())
                .temperatureAlert(temperatureAlert)
                .engineFaultCode(request.engineFaultCode())
                .build();

        VehicleTelemetry saved = telemetryRepository.save(telemetry);

        // Added dependency: AgentEventService
// Added right after "VehicleTelemetry saved = telemetryRepository.save(telemetry);"
        if (temperatureAlert) {
            agentEventService.log(new AgentEventCreateRequest(
                    AgentType.LISTENER, "TEMPERATURE_ALERT", EventSeverity.HIGH,
                    AgentEntityType.VEHICLE, vehicle.getId(),
                    "Cargo temperature out of range for vehicle " + vehicle.getVehicleCode()
                            + ": " + request.cargoTemperatureC() + "°C",
                    null, null, AgentEventStatus.COMPLETED
            ));
        }

        if (request.engineStatus() == EngineStatus.FAULT) {
            agentEventService.log(new AgentEventCreateRequest(
                    AgentType.LISTENER, "ENGINE_FAULT", EventSeverity.CRITICAL,
                    AgentEntityType.VEHICLE, vehicle.getId(),
                    "Engine fault on vehicle " + vehicle.getVehicleCode()
                            + (request.engineFaultCode() != null ? " (code: " + request.engineFaultCode() + ")" : ""),
                    null, null, AgentEventStatus.COMPLETED
            ));
        }

        if (request.odometerKm().compareTo(vehicle.getOdometerKm()) > 0) {
            vehicle.setOdometerKm(request.odometerKm());
            vehicle.setUpdatedAt(LocalDateTime.now());
        }

        return toResponse(saved);
    }

    @Override
    public Optional<VehicleTelemetryResponse> getLatestForVehicle(Long vehicleId) {
        return telemetryRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicleId)
                .map(this::toResponse);
    }

    @Override
    public List<VehicleTelemetryResponse> getHistory(
            Long vehicleId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return telemetryRepository.findByVehicleIdAndRecordedAtBetween(vehicleId, start, end, pageable)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<VehicleTelemetryResponse> getRecentTemperatureAlerts(Long vehicleId, Pageable pageable) {
        return telemetryRepository.findByVehicleIdAndTemperatureAlertTrueOrderByRecordedAtDesc(vehicleId, pageable)
                .stream().map(this::toResponse).toList();
    }

    private boolean computeTemperatureAlert(Vehicle vehicle, BigDecimal cargoTemperatureC) {
        if (!Boolean.TRUE.equals(vehicle.getRefrigerated()) || cargoTemperatureC == null) {
            return false;
        }
        if (vehicle.getMinTemperatureC() == null || vehicle.getMaxTemperatureC() == null) {
            return false; // no configured range to compare against
        }
        return cargoTemperatureC.compareTo(vehicle.getMinTemperatureC()) < 0
                || cargoTemperatureC.compareTo(vehicle.getMaxTemperatureC()) > 0;
    }

    private VehicleTelemetryResponse toResponse(VehicleTelemetry t) {
        return new VehicleTelemetryResponse(
                t.getId(), t.getVehicle().getId(), t.getVehicle().getVehicleCode(), t.getRecordedAt(),
                t.getLatitude(), t.getLongitude(), t.getSpeedKmh(), t.getFuelLevelPercent(),
                t.getEngineTemperatureC(), t.getCargoTemperatureC(), t.getOdometerKm(),
                t.getEngineStatus(), t.getTemperatureAlert(), t.getEngineFaultCode()
        );
    }
}