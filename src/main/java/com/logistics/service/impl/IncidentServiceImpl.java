package com.logistics.service.impl;

import com.logistics.dto.request.IncidentRequest;
import com.logistics.dto.response.IncidentResponse;
import com.logistics.dto.request.IncidentStatusUpdateRequest;
import com.logistics.entity.Incident;
import com.logistics.entity.Location;
import com.logistics.entity.enums.IncidentSeverity;
import com.logistics.entity.enums.IncidentStatus;
import com.logistics.entity.enums.IncidentType;
import com.logistics.exception.BusinessRuleViolationException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.IncidentRepository;
import com.logistics.service.IncidentService;
import com.logistics.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentServiceImpl implements IncidentService {

    // The severities that make an incident "critical" for compliance purposes,
    // per the confirmed Route Safety rule (Batch 4, decision #5).
    private static final Set<IncidentSeverity> CRITICAL_SEVERITIES =
            Set.of(IncidentSeverity.HIGH, IncidentSeverity.CRITICAL);

    private final IncidentRepository incidentRepository;
    private final LocationService locationService;


    // IncidentServiceImpl.create - idempotency check added before the existing validation
    @Override
    @Transactional
    public IncidentResponse create(IncidentRequest request) {

        boolean hasLocation = request.locationId() != null;
        boolean hasCoordinates = request.latitude() != null && request.longitude() != null;
        if (!hasLocation && !hasCoordinates) {
            throw new BusinessRuleViolationException(
                    "Incident must specify either locationId or both latitude and longitude");
        }
        if (request.expectedEndAt() != null && request.expectedEndAt().isBefore(request.startedAt())) {
            throw new BusinessRuleViolationException("expectedEndAt must not be before startedAt");
        }

        // Idempotency guard: a webhook retry for the same physical event will
        // carry the same type + startedAt + location. No dedicated idempotency
        // key exists in the schema, so this content-based match is the practical
        // substitute - if found, return the existing incident instead of
        // creating a duplicate.
        List<Incident> possibleDuplicates =
                incidentRepository.findByIncidentTypeAndStartedAt(request.incidentType(), request.startedAt());
        for (Incident existing : possibleDuplicates) {
            boolean sameLocation = hasLocation
                    ? existing.getLocation() != null && existing.getLocation().getId().equals(request.locationId())
                    : existing.getLatitude() != null && existing.getLatitude().compareTo(request.latitude()) == 0
                    && existing.getLongitude() != null && existing.getLongitude().compareTo(request.longitude()) == 0;
            if (sameLocation) {
                return toResponse(existing); // duplicate detected - return existing, do not insert
            }
        }

        Location location = hasLocation ? locationService.findEntity(request.locationId()) : null;

        Incident incident = Incident.builder()
                .incidentType(request.incidentType())
                .severity(request.severity())
                .title(request.title())
                .description(request.description())
                .location(location)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .startedAt(request.startedAt())
                .expectedEndAt(request.expectedEndAt())
                .status(IncidentStatus.ACTIVE)
                .source(request.source())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(incidentRepository.save(incident));
    }
    // Added to IncidentServiceImpl
    @Override
    public List<IncidentResponse> search(IncidentStatus status, IncidentSeverity severity, IncidentType type,
                                         LocalDateTime from, LocalDateTime to) {
        List<Incident> base = status != null ? incidentRepository.findByStatus(status) : incidentRepository.findAll();
        return base.stream()
                .filter(i -> severity == null || i.getSeverity() == severity)
                .filter(i -> type == null || i.getIncidentType() == type)
                .filter(i -> from == null || !i.getStartedAt().isBefore(from))
                .filter(i -> to == null || !i.getStartedAt().isAfter(to))
                .map(this::toResponse)
                .toList();
    }
    // Added to IncidentServiceImpl
    @Override
    @Transactional
    public IncidentResponse resolve(Long id) {
        Incident incident = findEntity(id);
        incident.setStatus(IncidentStatus.RESOLVED);
        if (incident.getResolvedAt() == null) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        return toResponse(incident);
    }
    @Override
    public IncidentResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<IncidentResponse> getActive() {
        return incidentRepository.findByStatus(IncidentStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    @Override
    public List<IncidentResponse> getByTypeAndStatus(IncidentType type, IncidentStatus status) {
        return incidentRepository.findByIncidentTypeAndStatus(type, status).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public IncidentResponse updateStatus(Long id, IncidentStatusUpdateRequest request) {
        Incident incident = findEntity(id);
        incident.setStatus(request.status());
        if (request.status() == IncidentStatus.RESOLVED && incident.getResolvedAt() == null) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        return toResponse(incident);
    }

    @Override
    public Incident findEntity(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    @Override
    public boolean hasCriticalActiveIncidentAtLocation(Long locationId) {
        if (locationId == null) {
            return false;
        }
        return incidentRepository.findByLocationIdAndStatus(locationId, IncidentStatus.ACTIVE).stream()
                .anyMatch(incident -> CRITICAL_SEVERITIES.contains(incident.getSeverity()));
    }

    private IncidentResponse toResponse(Incident i) {
        return new IncidentResponse(
                i.getId(), i.getIncidentType(), i.getSeverity(), i.getTitle(), i.getDescription(),
                i.getLocation() != null ? i.getLocation().getId() : null,
                i.getLocation() != null ? i.getLocation().getName() : null,
                i.getLatitude(), i.getLongitude(), i.getStartedAt(), i.getExpectedEndAt(),
                i.getResolvedAt(), i.getStatus(), i.getSource(), i.getCreatedAt()
        );
    }
}