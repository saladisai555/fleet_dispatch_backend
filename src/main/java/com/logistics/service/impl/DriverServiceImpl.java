package com.logistics.service.impl;

import com.logistics.dto.request.DriverAvailabilityUpdateRequest;
import com.logistics.dto.request.DriverRequest;
import com.logistics.dto.response.DriverResponse;
import com.logistics.entity.Driver;
import com.logistics.entity.Location;
import com.logistics.entity.User;
import com.logistics.entity.enums.AvailabilityStatus;
import com.logistics.exception.DuplicateResourceException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.DriverRepository;
import com.logistics.service.DriverService;
import com.logistics.service.LocationService;
import com.logistics.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final LocationService locationService;
    private final UserService userService;

    @Override
    @Transactional
    public DriverResponse create(DriverRequest request) {
        if (driverRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateResourceException("Employee code already exists: " + request.employeeCode());
        }
        if (driverRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("Phone already registered: " + request.phone());
        }
        if (driverRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new DuplicateResourceException("License number already registered: " + request.licenseNumber());
        }

        User user = request.userId() != null ? userService.findEntity(request.userId()) : null;
        Location location = request.currentLocationId() != null
                ? locationService.findEntity(request.currentLocationId()) : null;

        Driver driver = Driver.builder()
                .user(user)
                .employeeCode(request.employeeCode())
                .name(request.name())
                .phone(request.phone())
                .licenseNumber(request.licenseNumber())
                .licenseType(request.licenseType())
                .licenseExpiry(request.licenseExpiry())
                .maxDrivingHours(request.maxDrivingHours())
                .currentDrivingHours(BigDecimal.ZERO)
                .availabilityStatus(request.availabilityStatus())
                .currentLocation(location)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(driverRepository.save(driver));
    }

    @Override
    public DriverResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<DriverResponse> getByAvailability(AvailabilityStatus status) {
        return driverRepository.findByAvailabilityStatus(status).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DriverResponse updateAvailability(Long id, DriverAvailabilityUpdateRequest request) {
        Driver driver = findEntity(id);
        driver.setAvailabilityStatus(request.availabilityStatus());
        if (request.currentLocationId() != null) {
            driver.setCurrentLocation(locationService.findEntity(request.currentLocationId()));
        }
        driver.setUpdatedAt(LocalDateTime.now());
        return toResponse(driver);
    }

    @Override
    public Driver findEntity(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
    }
    // Added to DriverServiceImpl
    @Override
    @Transactional
    public DriverResponse update(Long id, DriverRequest request) {
        Driver driver = findEntity(id);

        if (!driver.getEmployeeCode().equals(request.employeeCode())
                && driverRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateResourceException("Employee code already exists: " + request.employeeCode());
        }
        if (!driver.getPhone().equals(request.phone())
                && driverRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("Phone already registered: " + request.phone());
        }
        if (!driver.getLicenseNumber().equals(request.licenseNumber())
                && driverRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new DuplicateResourceException("License number already registered: " + request.licenseNumber());
        }

        User user = request.userId() != null ? userService.findEntity(request.userId()) : null;
        Location location = request.currentLocationId() != null
                ? locationService.findEntity(request.currentLocationId()) : null;

        driver.setUser(user);
        driver.setEmployeeCode(request.employeeCode());
        driver.setName(request.name());
        driver.setPhone(request.phone());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setLicenseType(request.licenseType());
        driver.setLicenseExpiry(request.licenseExpiry());
        driver.setMaxDrivingHours(request.maxDrivingHours());
        driver.setAvailabilityStatus(request.availabilityStatus());
        driver.setCurrentLocation(location);
        driver.setUpdatedAt(LocalDateTime.now());

        return toResponse(driver);
    }

    @Override
    public List<DriverResponse> search(String availabilityStatus, Long locationId) {
        List<Driver> base = availabilityStatus != null
                ? driverRepository.findByAvailabilityStatus(AvailabilityStatus.valueOf(availabilityStatus))
                : driverRepository.findAll();
        return base.stream()
                .filter(d -> locationId == null || (d.getCurrentLocation() != null && d.getCurrentLocation().getId().equals(locationId)))
                .map(this::toResponse)
                .toList();
    }
    private DriverResponse toResponse(Driver d) {
        return new DriverResponse(
                d.getId(),
                d.getUser() != null ? d.getUser().getId() : null,
                d.getEmployeeCode(), d.getName(), d.getPhone(), d.getLicenseNumber(), d.getLicenseType(),
                d.getLicenseExpiry(), d.getMaxDrivingHours(), d.getCurrentDrivingHours(), d.getAvailabilityStatus(),
                d.getCurrentLocation() != null ? d.getCurrentLocation().getId() : null,
                d.getCurrentLocation() != null ? d.getCurrentLocation().getName() : null,
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}