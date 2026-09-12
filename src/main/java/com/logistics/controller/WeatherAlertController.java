package com.logistics.controller;

import com.logistics.dto.response.WeatherAlertResponse;
import com.logistics.service.WeatherAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weather-alerts")
@RequiredArgsConstructor
public class WeatherAlertController {

    private final WeatherAlertService weatherAlertService;

    @GetMapping
    public ResponseEntity<List<WeatherAlertResponse>> list(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String alertType) {
        return ResponseEntity.ok(weatherAlertService.search(locationId, severity, alertType));
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<WeatherAlertResponse> getById(@PathVariable Long alertId) {
        return ResponseEntity.ok(weatherAlertService.getById(alertId));
    }
}