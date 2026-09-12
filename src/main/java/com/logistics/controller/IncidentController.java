package com.logistics.controller;

import com.logistics.dto.request.IncidentRequest;
import com.logistics.dto.response.IncidentResponse;
import com.logistics.entity.enums.IncidentSeverity;
import com.logistics.entity.enums.IncidentStatus;
import com.logistics.entity.enums.IncidentType;
import com.logistics.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) IncidentType incidentType,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        return ResponseEntity.ok(incidentService.search(status, severity, incidentType, from, to));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> getById(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentService.getById(incidentId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','FLEET_MANAGER','SAFETY_MANAGER')")
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody IncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(request));
    }

    @PatchMapping("/{incidentId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','FLEET_MANAGER','SAFETY_MANAGER')")
    public ResponseEntity<IncidentResponse> resolve(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentService.resolve(incidentId));
    }
}