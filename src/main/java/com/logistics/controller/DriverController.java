package com.logistics.controller;

import com.logistics.dto.request.DriverAvailabilityUpdateRequest;
import com.logistics.dto.request.DriverRequest;
import com.logistics.dto.response.DriverResponse;
import com.logistics.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public ResponseEntity<List<DriverResponse>> list(
            @RequestParam(required = false) String availabilityStatus,
            @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(driverService.search(availabilityStatus, locationId));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getById(@PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getById(driverId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.create(request));
    }

    @PutMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<DriverResponse> update(@PathVariable Long driverId,
                                                 @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(driverService.update(driverId, request));
    }

    @PatchMapping("/{driverId}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<DriverResponse> updateAvailability(@PathVariable Long driverId,
                                                             @Valid @RequestBody DriverAvailabilityUpdateRequest request) {
        return ResponseEntity.ok(driverService.updateAvailability(driverId, request));
    }
}