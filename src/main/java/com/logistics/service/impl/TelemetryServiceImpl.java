package com.logistics.service.impl;

import com.logistics.dto.request.VehicleTelemetryRequest;
import com.logistics.dto.response.VehicleTelemetryResponse;
import com.logistics.entity.Vehicle;
import com.logistics.entity.VehicleTelemetry;
import com.logistics.entity.enums.EngineStatus;
import com.logistics.repository.VehicleTelemetryRepository;
import com.logistics.service.TelemetryService;
import com.logistics.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final VehicleService vehicleService;

    // TelemetryServiceImpl - dedup window added
    @Value("${app.telemetry.dedup-window-seconds:30}")
    private long dedupWindowSeconds;

    // TelemetryServiceImpl.ingest - updated to use client-supplied recordedAt
// and to ignore the client-supplied temperatureAlert in favor of the
// server-computed value (request.temperatureAlert() is intentionally unused).
    @Override
    @Transactional
    public VehicleTelemetryResponse ingest(VehicleTelemetryRequest request) {
        Vehicle vehicle = vehicleService.findEntity(request.vehicleId());

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