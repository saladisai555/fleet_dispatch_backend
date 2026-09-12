package com.logistics.service;

import com.logistics.dto.request.RouteCalculateRequest;
import com.logistics.dto.request.RouteRequest;
import com.logistics.dto.response.RouteCalculateResponse;
import com.logistics.dto.response.RouteResponse;
import com.logistics.entity.Route;

import java.util.List;
import java.util.Optional;

public interface RouteService {
    RouteResponse planRoute(RouteRequest request);
    RouteResponse reroute(Long manifestId, RouteRequest request);
    List<RouteResponse> getHistoryForManifest(Long manifestId);
    Route findEntity(Long id);
    // Added to RouteService interface
    RouteResponse activate(Long routeId);
    // RouteService interface - signature changed
    Optional<RouteResponse> getActiveForManifest(Long manifestId);
    // Added to RouteService interface
    RouteCalculateResponse calculatePreview(RouteCalculateRequest request);
    // Added to RouteService interface
    RouteResponse getRouteResponseById(Long id);// non-persisting preview
}