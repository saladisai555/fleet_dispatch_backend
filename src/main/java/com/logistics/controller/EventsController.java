package com.logistics.controller;

import com.logistics.dto.*;
import com.logistics.dto.request.IncidentRequest;
import com.logistics.dto.request.VehicleTelemetryRequest;
import com.logistics.dto.request.WeatherAlertRequest;
import com.logistics.dto.response.IncidentResponse;
import com.logistics.dto.response.VehicleTelemetryResponse;
import com.logistics.dto.response.WeatherAlertResponse;
import com.logistics.service.IncidentService;
import com.logistics.service.TelemetryService;
import com.logistics.service.WeatherAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventsController {

    private final TelemetryService telemetryService;
    private final IncidentService incidentService;
    private final WeatherAlertService weatherAlertService;

    // No @PreAuthorize here deliberately - these are external/webhook-style
    // ingestion boundaries (API_Specification.md Section 14: "External
    // integrations may use these endpoints through controlled webhooks").
    // Real protection for these will be signature/API-key-based (Step 12/18),
    // not user-role-based like the rest of the API.

    @PostMapping("/telemetry")
    public ResponseEntity<VehicleTelemetryResponse> telemetry(@Valid @RequestBody VehicleTelemetryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(telemetryService.ingest(request));
    }

    // Request body shape not explicitly documented for traffic/road-closure/
    // weather events beyond telemetry - reusing the existing, already-correct
    // IncidentRequest/WeatherAlertRequest DTOs rather than inventing new ones
    // with unverified shapes.

    @PostMapping("/traffic")
    public ResponseEntity<IncidentResponse> traffic(@Valid @RequestBody IncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(request));
    }

    @PostMapping("/road-closures")
    public ResponseEntity<IncidentResponse> roadClosure(@Valid @RequestBody IncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(request));
    }

    @PostMapping("/weather")
    public ResponseEntity<WeatherAlertResponse> weather(@Valid @RequestBody WeatherAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(weatherAlertService.create(request));
    }
}