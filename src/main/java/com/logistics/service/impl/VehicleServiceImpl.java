package com.logistics.service.impl;

import com.logistics.dto.request.VehicleRequest;
import com.logistics.dto.response.*;
import com.logistics.entity.Driver;
import com.logistics.entity.Location;
import com.logistics.entity.Manifest;
import com.logistics.entity.Route;
import com.logistics.entity.Vehicle;
import com.logistics.entity.enums.ManifestStatus;
import com.logistics.entity.enums.RouteStatus;
import com.logistics.exception.BusinessRuleViolationException;
import com.logistics.exception.DuplicateResourceException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.ManifestRepository;
import com.logistics.repository.RouteRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final LocationService locationService;
    private final DriverService driverService;
    private final TelemetryService telemetryService;
    private final ManifestRepository manifestRepository;
    private final RouteRepository routeRepository;
    // ManifestItemService and RouteService REMOVED - both transitively depend
    // on ManifestService, which depends on VehicleService. Injecting either
    // one here recreates the exact cycle we're trying to eliminate. Reading
    // directly from ManifestRepository/RouteRepository below breaks it, since
    // repositories never depend on services.

    @Override
    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        if (vehicleRepository.existsByVehicleCode(request.vehicleCode())) {
            throw new DuplicateResourceException("Vehicle code already exists: " + request.vehicleCode());
        }
        if (vehicleRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new DuplicateResourceException("Registration number already exists: " + request.registrationNumber());
        }
        if (Boolean.TRUE.equals(request.refrigerated())
                && (request.minTemperatureC() == null || request.maxTemperatureC() == null)) {
            throw new BusinessRuleViolationException(
                    "Refrigerated vehicles must specify both minTemperatureC and maxTemperatureC");
        }

        Location location = request.currentLocationId() != null
                ? locationService.findEntity(request.currentLocationId()) : null;
        Driver driver = request.currentDriverId() != null
                ? driverService.findEntity(request.currentDriverId()) : null;

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(request.registrationNumber())
                .vehicleCode(request.vehicleCode())
                .vehicleType(request.vehicleType())
                .capacityKg(request.capacityKg())
                .capacityVolumeM3(request.capacityVolumeM3())
                .fuelType(request.fuelType())
                .refrigerated(request.refrigerated())
                .minTemperatureC(request.minTemperatureC())
                .maxTemperatureC(request.maxTemperatureC())
                .status(request.status())
                .currentLocation(location)
                .currentDriver(driver)
                .odometerKm(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public VehicleResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<VehicleResponse> getByStatus(String status) {
        return vehicleRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Override
    public List<VehicleResponse> findAvailableCandidates(
            String status, BigDecimal minCapacityKg, BigDecimal minCapacityVolumeM3, boolean requiresRefrigeration) {
        return vehicleRepository.findAvailableCandidates(status, minCapacityKg, minCapacityVolumeM3, requiresRefrigeration)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public Vehicle findEntity(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }

    @Override
    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehicle = findEntity(id);

        if (!vehicle.getVehicleCode().equals(request.vehicleCode())
                && vehicleRepository.existsByVehicleCode(request.vehicleCode())) {
            throw new DuplicateResourceException("Vehicle code already exists: " + request.vehicleCode());
        }
        if (!vehicle.getRegistrationNumber().equals(request.registrationNumber())
                && vehicleRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new DuplicateResourceException("Registration number already exists: " + request.registrationNumber());
        }
        if (Boolean.TRUE.equals(request.refrigerated())
                && (request.minTemperatureC() == null || request.maxTemperatureC() == null)) {
            throw new BusinessRuleViolationException(
                    "Refrigerated vehicles must specify both minTemperatureC and maxTemperatureC");
        }

        Location location = request.currentLocationId() != null
                ? locationService.findEntity(request.currentLocationId()) : null;
        Driver driver = request.currentDriverId() != null
                ? driverService.findEntity(request.currentDriverId()) : null;

        vehicle.setRegistrationNumber(request.registrationNumber());
        vehicle.setVehicleCode(request.vehicleCode());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setCapacityKg(request.capacityKg());
        vehicle.setCapacityVolumeM3(request.capacityVolumeM3());
        vehicle.setFuelType(request.fuelType());
        vehicle.setRefrigerated(request.refrigerated());
        vehicle.setMinTemperatureC(request.minTemperatureC());
        vehicle.setMaxTemperatureC(request.maxTemperatureC());
        vehicle.setStatus(request.status());
        vehicle.setCurrentLocation(location);
        vehicle.setCurrentDriver(driver);
        vehicle.setUpdatedAt(LocalDateTime.now());

        return toResponse(vehicle);
    }

    @Override
    public List<VehicleResponse> search(String status, String vehicleType, Boolean refrigerated, Long locationId) {
        return vehicleRepository.findAll().stream()
                .filter(v -> status == null || status.equals(v.getStatus()))
                .filter(v -> vehicleType == null || vehicleType.equals(v.getVehicleType()))
                .filter(v -> refrigerated == null || refrigerated.equals(v.getRefrigerated()))
                .filter(v -> locationId == null || (v.getCurrentLocation() != null && v.getCurrentLocation().getId().equals(locationId)))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public VehicleDetailsResponse getDetails(Long id) {
        Vehicle vehicle = findEntity(id);

        VehicleTelemetryResponse latestTelemetry = telemetryService.getLatestForVehicle(id).orElse(null);

        List<ManifestStatus> activeStatuses = List.of(
                ManifestStatus.PLANNED, ManifestStatus.DISPATCHED, ManifestStatus.IN_TRANSIT);

        // Read directly from ManifestRepository, NOT ManifestService/ManifestItemService.
        Manifest activeManifestEntity = manifestRepository
                .findByVehicleIdAndStatusIn(id, activeStatuses)
                .stream().findFirst().orElse(null);

        ManifestResponse activeManifest = activeManifestEntity != null
                ? toManifestSummary(activeManifestEntity)
                : null;

        // Read directly from RouteRepository, NOT RouteService.
        RouteResponse activeRoute = activeManifestEntity != null
                ? routeRepository.findByManifestIdAndStatus(activeManifestEntity.getId(), RouteStatus.ACTIVE)
                .or(() -> routeRepository.findByManifestIdAndStatus(activeManifestEntity.getId(), RouteStatus.PLANNED))
                .map(this::toRouteSummary)
                .orElse(null)
                : null;

        return new VehicleDetailsResponse(toResponse(vehicle), latestTelemetry, activeManifest, activeRoute);
    }

    // Lightweight local mapper - deliberately duplicates a small amount of
    // mapping logic instead of calling ManifestService/ManifestItemService.
    // 'items' is intentionally null: a vehicle-details summary doesn't need
    // the full nested manifest-items list, and including it is exactly what
    // would force the ManifestItemService dependency back in.
    private ManifestResponse toManifestSummary(Manifest m) {
        return new ManifestResponse(
                m.getId(), m.getManifestNumber(),
                m.getVehicle().getId(), m.getVehicle().getVehicleCode(),
                m.getDriver().getId(), m.getDriver().getName(),
                m.getStartLocation().getId(), m.getStartLocation().getName(),
                m.getPlannedStartTime(), m.getPlannedEndTime(),
                m.getActualStartTime(), m.getActualEndTime(), m.getStatus(),
                null, // items omitted deliberately - see note above
                m.getCreatedAt(), m.getUpdatedAt()
        );
    }

    private RouteResponse toRouteSummary(Route r) {
        return new RouteResponse(
                r.getId(), r.getManifest().getId(),
                r.getOriginLocation().getId(), r.getOriginLocation().getName(),
                r.getDestinationLocation().getId(), r.getDestinationLocation().getName(),
                r.getRouteGeometry(), r.getDistanceKm(), r.getEstimatedDurationMinutes(),
                r.getPlannedStartTime(), r.getPlannedEndTime(), r.getTrafficDelayMinutes(),
                r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }

    private VehicleResponse toResponse(Vehicle v) {
        return new VehicleResponse(
                v.getId(), v.getRegistrationNumber(), v.getVehicleCode(), v.getVehicleType(),
                v.getCapacityKg(), v.getCapacityVolumeM3(), v.getFuelType(), v.getRefrigerated(),
                v.getMinTemperatureC(), v.getMaxTemperatureC(), v.getStatus(),
                v.getCurrentLocation() != null ? v.getCurrentLocation().getId() : null,
                v.getCurrentLocation() != null ? v.getCurrentLocation().getName() : null,
                v.getCurrentDriver() != null ? v.getCurrentDriver().getId() : null,
                v.getCurrentDriver() != null ? v.getCurrentDriver().getName() : null,
                v.getOdometerKm(), v.getCreatedAt(), v.getUpdatedAt()
        );
    }
}