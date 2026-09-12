package com.logistics.service;

import com.logistics.dto.request.ManifestRequest;
import com.logistics.dto.response.ManifestResponse;
import com.logistics.entity.Manifest;
import com.logistics.entity.enums.ManifestStatus;

import java.util.List;

public interface ManifestService {
    ManifestResponse create(ManifestRequest request);
    ManifestResponse getById(Long id);
    List<ManifestResponse> getByStatus(ManifestStatus status);
    ManifestResponse updateStatus(Long id, ManifestStatus newStatus);
    Manifest findEntity(Long id);
    // Added to ManifestService interface
    ManifestResponse update(Long id, ManifestRequest request);
    List<ManifestResponse> search(ManifestStatus status, Long vehicleId, Long driverId);
}