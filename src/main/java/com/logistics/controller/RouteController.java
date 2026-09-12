package com.logistics.controller;

import com.logistics.dto.*;
import com.logistics.dto.request.RouteCalculateRequest;
import com.logistics.dto.response.RouteCalculateResponse;
import com.logistics.dto.response.RouteResponse;
import com.logistics.entity.enums.RouteStatus;
import com.logistics.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteResponse>> list(
            @RequestParam(required = false) Long manifestId,
            @RequestParam(required = false) RouteStatus status) {
        // manifestId is effectively required for a meaningful result given
        // current repository methods - a global cross-manifest route list
        // wasn't part of the documented use case set, so this keeps to what's
        // actually needed: history for one manifest, optionally status-filtered.
        if (manifestId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<RouteResponse> history = routeService.getHistoryForManifest(manifestId);
        return ResponseEntity.ok(status == null ? history
                : history.stream().filter(r -> r.status() == status).toList());
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<RouteResponse> getById(@PathVariable Long routeId) {
        return ResponseEntity.ok(routeService.getRouteResponseById(routeId)); // see note below
    }

    @PostMapping("/calculate")
    public ResponseEntity<RouteCalculateResponse> calculate(@Valid @RequestBody RouteCalculateRequest request) {
        return ResponseEntity.ok(routeService.calculatePreview(request));
    }
}