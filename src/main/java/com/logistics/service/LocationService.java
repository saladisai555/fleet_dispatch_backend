package com.logistics.service;

import com.logistics.dto.request.LocationRequest;
import com.logistics.dto.response.LocationResponse;
import com.logistics.entity.Location;
import com.logistics.entity.enums.LocationType;

import java.util.List;

public interface LocationService {
    LocationResponse create(LocationRequest request);
    LocationResponse getById(Long id);
    List<LocationResponse> getAll();
    List<LocationResponse> getByType(LocationType type);
    Location findEntity(Long id); // internal accessor for other services
}