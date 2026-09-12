package com.logistics.service;

import com.logistics.dto.request.VehicleRequest;
import com.logistics.dto.response.VehicleDetailsResponse;
import com.logistics.dto.response.VehicleResponse;
import com.logistics.entity.Vehicle;

import java.math.BigDecimal;
import java.util.List;

public interface VehicleService {
    VehicleResponse create(VehicleRequest request);
    VehicleResponse getById(Long id);
    List<VehicleResponse> getByStatus(String status);
    List<VehicleResponse> findAvailableCandidates(
            String status, BigDecimal minCapacityKg, BigDecimal minCapacityVolumeM3, boolean requiresRefrigeration);
    Vehicle findEntity(Long id);
    // Added to VehicleService interface
    VehicleResponse update(Long id, VehicleRequest request);
    List<VehicleResponse> search(String status, String vehicleType, Boolean refrigerated, Long locationId);
    VehicleDetailsResponse getDetails(Long id); // composite, backs GET /vehicles/{id}/details
}