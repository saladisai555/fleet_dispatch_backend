package com.logistics.service.impl;

import com.logistics.dto.request.ManifestRequest;
import com.logistics.dto.response.ManifestResponse;
import com.logistics.entity.Driver;
import com.logistics.entity.Location;
import com.logistics.entity.Manifest;
import com.logistics.entity.Vehicle;
import com.logistics.entity.enums.ManifestStatus;
import com.logistics.exception.BusinessRuleViolationException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.ManifestRepository;
import com.logistics.service.DriverService;
import com.logistics.service.LocationService;
import com.logistics.service.ManifestItemService;
import com.logistics.service.ManifestService;
import com.logistics.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManifestServiceImpl implements ManifestService {

    private static final List<ManifestStatus> ACTIVE_STATUSES =
            List.of(ManifestStatus.PLANNED, ManifestStatus.DISPATCHED, ManifestStatus.IN_TRANSIT);

    private final ManifestRepository manifestRepository;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final LocationService locationService;
    private final ManifestItemService manifestItemService;

    @Override
    @Transactional
    public ManifestResponse create(ManifestRequest request) {

        if (!request.plannedEndTime().isAfter(request.plannedStartTime())) {
            throw new BusinessRuleViolationException("plannedEndTime must be after plannedStartTime");
        }

        Vehicle vehicle = vehicleService.findEntity(request.vehicleId());
        Driver driver = driverService.findEntity(request.driverId());
        Location startLocation = locationService.findEntity(request.startLocationId());

        if (!manifestRepository.findByVehicleIdAndStatusIn(vehicle.getId(), ACTIVE_STATUSES).isEmpty()) {
            throw new BusinessRuleViolationException("Vehicle is already committed to an active manifest: " + vehicle.getVehicleCode());
        }
        if (!manifestRepository.findByDriverIdAndStatusIn(driver.getId(), ACTIVE_STATUSES).isEmpty()) {
            throw new BusinessRuleViolationException("Driver is already committed to an active manifest: " + driver.getName());
        }

        Manifest manifest = Manifest.builder()
                .manifestNumber(generateManifestNumber())
                .vehicle(vehicle)
                .driver(driver)
                .startLocation(startLocation)
                .plannedStartTime(request.plannedStartTime())
                .plannedEndTime(request.plannedEndTime())
                .status(ManifestStatus.PLANNED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(manifestRepository.save(manifest), false);
    }
    // Added to ManifestServiceImpl
    @Override
    @Transactional
    public ManifestResponse update(Long id, ManifestRequest request) {
        Manifest manifest = findEntity(id);

        if (!request.plannedEndTime().isAfter(request.plannedStartTime())) {
            throw new BusinessRuleViolationException("plannedEndTime must be after plannedStartTime");
        }

        Vehicle vehicle = vehicleService.findEntity(request.vehicleId());
        Driver driver = driverService.findEntity(request.driverId());
        Location startLocation = locationService.findEntity(request.startLocationId());

        // Double-booking guard applies here too, excluding this manifest itself.
        boolean vehicleConflict = manifestRepository.findByVehicleIdAndStatusIn(vehicle.getId(), ACTIVE_STATUSES)
                .stream().anyMatch(m -> !m.getId().equals(id));
        boolean driverConflict = manifestRepository.findByDriverIdAndStatusIn(driver.getId(), ACTIVE_STATUSES)
                .stream().anyMatch(m -> !m.getId().equals(id));
        if (vehicleConflict) {
            throw new BusinessRuleViolationException("Vehicle is already committed to another active manifest: " + vehicle.getVehicleCode());
        }
        if (driverConflict) {
            throw new BusinessRuleViolationException("Driver is already committed to another active manifest: " + driver.getName());
        }

        manifest.setVehicle(vehicle);
        manifest.setDriver(driver);
        manifest.setStartLocation(startLocation);
        manifest.setPlannedStartTime(request.plannedStartTime());
        manifest.setPlannedEndTime(request.plannedEndTime());
        manifest.setUpdatedAt(LocalDateTime.now());

        return toResponse(manifest, false);
    }

    @Override
    public List<ManifestResponse> search(ManifestStatus status, Long vehicleId, Long driverId) {
        List<Manifest> base = status != null ? manifestRepository.findByStatus(status) : manifestRepository.findAll();
        return base.stream()
                .filter(m -> vehicleId == null || m.getVehicle().getId().equals(vehicleId))
                .filter(m -> driverId == null || m.getDriver().getId().equals(driverId))
                .map(m -> toResponse(m, false))
                .toList();
    }
    @Override
    public ManifestResponse getById(Long id) {
        return toResponse(findEntity(id), true);
    }

    @Override
    public List<ManifestResponse> getByStatus(ManifestStatus status) {
        return manifestRepository.findByStatus(status).stream()
                .map(m -> toResponse(m, false))
                .toList();
    }

    @Override
    @Transactional
    public ManifestResponse updateStatus(Long id, ManifestStatus newStatus) {
        Manifest manifest = findEntity(id);
        manifest.setStatus(newStatus);
        manifest.setUpdatedAt(LocalDateTime.now());
        if (newStatus == ManifestStatus.DISPATCHED && manifest.getActualStartTime() == null) {
            manifest.setActualStartTime(LocalDateTime.now());
        }
        if (newStatus == ManifestStatus.COMPLETED && manifest.getActualEndTime() == null) {
            manifest.setActualEndTime(LocalDateTime.now());
        }
        return toResponse(manifest, false);
    }

    @Override
    public Manifest findEntity(Long id) {
        return manifestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manifest not found: " + id));
    }

    private String generateManifestNumber() {
        return "MFT-" + System.currentTimeMillis();
    }

    private ManifestResponse toResponse(Manifest m, boolean includeItems) {
        return new ManifestResponse(
                m.getId(), m.getManifestNumber(),
                m.getVehicle().getId(), m.getVehicle().getVehicleCode(),
                m.getDriver().getId(), m.getDriver().getName(),
                m.getStartLocation().getId(), m.getStartLocation().getName(),
                m.getPlannedStartTime(), m.getPlannedEndTime(),
                m.getActualStartTime(), m.getActualEndTime(), m.getStatus(),
                includeItems ? manifestItemService.getByManifestId(m.getId()) : null,
                m.getCreatedAt(), m.getUpdatedAt()
        );
    }
}