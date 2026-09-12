package com.logistics.service;

import com.logistics.dto.request.DriverAvailabilityUpdateRequest;
import com.logistics.dto.request.DriverRequest;
import com.logistics.dto.response.DriverResponse;
import com.logistics.entity.Driver;
import com.logistics.entity.enums.AvailabilityStatus;

import java.util.List;

public interface DriverService {
    DriverResponse create(DriverRequest request);
    DriverResponse getById(Long id);
    List<DriverResponse> getByAvailability(AvailabilityStatus status);
    DriverResponse updateAvailability(Long id, DriverAvailabilityUpdateRequest request);
    Driver findEntity(Long id);
    // Added to DriverService interface
    DriverResponse update(Long id, DriverRequest request);
    List<DriverResponse> search(String availabilityStatus, Long locationId);
}