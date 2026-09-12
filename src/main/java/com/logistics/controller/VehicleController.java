package com.logistics.controller;

import com.logistics.dto.response.VehicleDetailsResponse;
import com.logistics.dto.request.VehicleRequest;
import com.logistics.dto.response.VehicleResponse;
import com.logistics.dto.response.VehicleTelemetryResponse;
import com.logistics.service.TelemetryService;
import com.logistics.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final TelemetryService telemetryService;

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) Boolean refrigerated,
            @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(vehicleService.search(status, vehicleType, refrigerated, locationId));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> getById(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(vehicleService.getById(vehicleId));
    }

    @GetMapping("/{vehicleId}/details")
    public ResponseEntity<VehicleDetailsResponse> getDetails(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(vehicleService.getDetails(vehicleId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(request));
    }

    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<VehicleResponse> update(@PathVariable Long vehicleId,
                                                  @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(vehicleId, request));
    }

    @GetMapping("/{vehicleId}/telemetry")
    public ResponseEntity<List<VehicleTelemetryResponse>> telemetryHistory(
            @PathVariable Long vehicleId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "50") int limit) {
        LocalDateTime start = from != null ? from : LocalDateTime.now().minusDays(7);
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(telemetryService.getHistory(vehicleId, start, end, PageRequest.of(0, limit)));
    }
}