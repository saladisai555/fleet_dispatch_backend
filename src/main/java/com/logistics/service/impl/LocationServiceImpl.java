package com.logistics.service.impl;

import com.logistics.dto.request.LocationRequest;
import com.logistics.dto.response.LocationResponse;
import com.logistics.entity.Location;
import com.logistics.entity.enums.LocationType;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.LocationRepository;
import com.logistics.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public LocationResponse create(LocationRequest request) {
        Location location = Location.builder()
                .name(request.name())
                .locationType(request.locationType())
                .address(request.address())
                .city(request.city())
                .district(request.district())
                .state(request.state())
                .pincode(request.pincode())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(locationRepository.save(location));
    }

    @Override
    public LocationResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<LocationResponse> getAll() {
        return locationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<LocationResponse> getByType(LocationType type) {
        return locationRepository.findByLocationType(type).stream().map(this::toResponse).toList();
    }

    @Override
    public Location findEntity(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
    }

    private LocationResponse toResponse(Location l) {
        return new LocationResponse(
                l.getId(), l.getName(), l.getLocationType(), l.getAddress(),
                l.getCity(), l.getDistrict(), l.getState(), l.getPincode(),
                l.getLatitude(), l.getLongitude(), l.getCreatedAt()
        );
    }
}