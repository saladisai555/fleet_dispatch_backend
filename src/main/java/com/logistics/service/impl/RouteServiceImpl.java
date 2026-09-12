package com.logistics.service.impl;

import com.logistics.dto.request.RouteCalculateRequest;
import com.logistics.dto.request.RouteRequest;
import com.logistics.dto.response.RouteCalculateResponse;
import com.logistics.dto.response.RouteResponse;
import com.logistics.entity.Location;
import com.logistics.entity.Manifest;
import com.logistics.entity.Route;
import com.logistics.entity.Vehicle;
import com.logistics.entity.enums.RouteStatus;
import com.logistics.exception.BusinessRuleViolationException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.RouteRepository;
import com.logistics.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final ManifestService manifestService;
    private final LocationService locationService;
    private final RoutingService routingService;
    private final VehicleService vehicleService;
    // RoutingService (OSRM abstraction, Step 18) not implemented yet — see
    // placeholder distance/duration values below.

    // RouteServiceImpl - constructor now also takes RoutingService; planRoute()
// updated to call it instead of hardcoding BigDecimal.ONE / 1
    @Override
    @Transactional
    public RouteResponse planRoute(RouteRequest request) {
        if (request.originLocationId().equals(request.destinationLocationId())) {
            throw new BusinessRuleViolationException("Origin and destination locations must differ");
        }
        if (!request.plannedEndTime().isAfter(request.plannedStartTime())) {
            throw new BusinessRuleViolationException("plannedEndTime must be after plannedStartTime");
        }

        Manifest manifest = manifestService.findEntity(request.manifestId());
        Location origin = locationService.findEntity(request.originLocationId());
        Location destination = locationService.findEntity(request.destinationLocationId());

        RoutingService.RouteCalculationResult calc = routingService.calculateRoute(origin, destination, manifest.getVehicle());

        Route route = Route.builder()
                .manifest(manifest)
                .originLocation(origin)
                .destinationLocation(destination)
                .routeGeometry(calc.routeGeometry())
                .distanceKm(calc.distanceKm())
                .estimatedDurationMinutes(calc.estimatedDurationMinutes())
                .plannedStartTime(request.plannedStartTime())
                .plannedEndTime(request.plannedEndTime())
                .trafficDelayMinutes(calc.trafficDelayMinutes())
                .status(RouteStatus.PLANNED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(routeRepository.save(route));
    }

    // Added to RouteServiceImpl
    @Override
    public RouteCalculateResponse calculatePreview(RouteCalculateRequest request) {
        Location origin = locationService.findEntity(request.originLocationId());
        Location destination = locationService.findEntity(request.destinationLocationId());

        Vehicle vehicle = vehicleService.findEntity(request.vehicleId()); // requires VehicleService dependency

        var calc = routingService.calculateRoute(origin, destination, vehicle);
        return new RouteCalculateResponse(calc.distanceKm(), calc.estimatedDurationMinutes(),
                calc.trafficDelayMinutes(), calc.routeGeometry());
    }
    @Override
    @Transactional
    public RouteResponse reroute(Long manifestId, RouteRequest request) {
        routeRepository.findByManifestIdAndStatus(manifestId, RouteStatus.ACTIVE)
                .or(() -> routeRepository.findByManifestIdAndStatus(manifestId, RouteStatus.PLANNED))
                .ifPresent(old -> {
                    old.setStatus(RouteStatus.REPLACED);
                    old.setUpdatedAt(LocalDateTime.now());
                });

        return planRoute(request);
    }

    // RouteServiceImpl - updated to return Optional instead of throwing
    @Override
    public Optional<RouteResponse> getActiveForManifest(Long manifestId) {
        return routeRepository.findByManifestIdAndStatus(manifestId, RouteStatus.ACTIVE)
                .or(() -> routeRepository.findByManifestIdAndStatus(manifestId, RouteStatus.PLANNED))
                .map(this::toResponse);
    }

    @Override
    public List<RouteResponse> getHistoryForManifest(Long manifestId) {
        return routeRepository.findByManifestIdOrderByCreatedAtDesc(manifestId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public Route findEntity(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + id));
    }
    // RouteServiceImpl - new method
    @Override
    @Transactional
    public RouteResponse activate(Long routeId) {
        Route route = findEntity(routeId);

        if (route.getStatus() != RouteStatus.PLANNED) {
            throw new BusinessRuleViolationException(
                    "Only a PLANNED route can be activated - current status: " + route.getStatus());
        }

        // Defensive exclusivity: ensure no other ACTIVE route exists for the same
        // manifest before promoting this one (should already be guaranteed by
        // reroute()'s REPLACED-flip, but checked here since activate() can also
        // be called independently).
        routeRepository.findByManifestIdAndStatus(route.getManifest().getId(), RouteStatus.ACTIVE)
                .ifPresent(existingActive -> {
                    existingActive.setStatus(RouteStatus.REPLACED);
                    existingActive.setUpdatedAt(LocalDateTime.now());
                });

        route.setStatus(RouteStatus.ACTIVE);
        route.setUpdatedAt(LocalDateTime.now());

        return toResponse(route);
    }
    // Added to RouteServiceImpl
    @Override
    public RouteResponse getRouteResponseById(Long id) {
        return toResponse(findEntity(id));
    }
    private RouteResponse toResponse(Route r) {
        return new RouteResponse(
                r.getId(), r.getManifest().getId(),
                r.getOriginLocation().getId(), r.getOriginLocation().getName(),
                r.getDestinationLocation().getId(), r.getDestinationLocation().getName(),
                r.getRouteGeometry(), r.getDistanceKm(), r.getEstimatedDurationMinutes(),
                r.getPlannedStartTime(), r.getPlannedEndTime(), r.getTrafficDelayMinutes(),
                r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}