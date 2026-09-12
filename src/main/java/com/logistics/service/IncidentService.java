package com.logistics.service;

import com.logistics.dto.request.IncidentRequest;
import com.logistics.dto.response.IncidentResponse;
import com.logistics.dto.request.IncidentStatusUpdateRequest;
import com.logistics.entity.Incident;
import com.logistics.entity.enums.IncidentSeverity;
import com.logistics.entity.enums.IncidentStatus;
import com.logistics.entity.enums.IncidentType;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentService {
    IncidentResponse create(IncidentRequest request);
    IncidentResponse getById(Long id);
    List<IncidentResponse> getActive();
    List<IncidentResponse> getByTypeAndStatus(IncidentType type, IncidentStatus status);
    IncidentResponse updateStatus(Long id, IncidentStatusUpdateRequest request);
    Incident findEntity(Long id);

    // Used by ComplianceService's route-safety check — returns true if any
    // ACTIVE HIGH/CRITICAL incident affects the given location.
    boolean hasCriticalActiveIncidentAtLocation(Long locationId);
    // Added to IncidentService interface
    IncidentResponse resolve(Long id);
    // backs PATCH /incidents/{id}/resolve (no request body per docs)
    // Added to IncidentService interface
    List<IncidentResponse> search(IncidentStatus status, IncidentSeverity severity, IncidentType type,
                                  LocalDateTime from, LocalDateTime to);
}