package com.logistics.service.impl;

import com.logistics.dto.request.WeatherAlertRequest;
import com.logistics.dto.response.WeatherAlertResponse;
import com.logistics.entity.Location;
import com.logistics.entity.WeatherAlert;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.WeatherAlertRepository;
import com.logistics.service.LocationService;
import com.logistics.service.WeatherAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherAlertServiceImpl implements WeatherAlertService {

    private final WeatherAlertRepository weatherAlertRepository;
    private final LocationService locationService;

    @Override
    @Transactional
    public WeatherAlertResponse create(WeatherAlertRequest request) {
        Location location = locationService.findEntity(request.locationId());

        WeatherAlert alert = WeatherAlert.builder()
                .location(location)
                .alertType(request.alertType())
                .severity(request.severity())
                .description(request.description())
                .temperatureC(request.temperatureC())
                .rainfallMm(request.rainfallMm())
                .build();

        return toResponse(weatherAlertRepository.save(alert));
    }

    @Override
    public WeatherAlertResponse getById(Long id) {
        WeatherAlert alert = weatherAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weather alert not found: " + id));
        return toResponse(alert);
    }

    @Override
    public List<WeatherAlertResponse> getByLocation(Long locationId) {
        return weatherAlertRepository.findByLocationId(locationId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<WeatherAlertResponse> getBySeverity(String severity) {
        return weatherAlertRepository.findBySeverity(severity).stream().map(this::toResponse).toList();
    }
    // Added to WeatherAlertServiceImpl
    @Override
    public List<WeatherAlertResponse> search(Long locationId, String severity, String alertType) {
        List<WeatherAlert> base = locationId != null
                ? weatherAlertRepository.findByLocationId(locationId) : weatherAlertRepository.findAll();
        return base.stream()
                .filter(w -> severity == null || severity.equalsIgnoreCase(w.getSeverity()))
                .filter(w -> alertType == null || alertType.equalsIgnoreCase(w.getAlertType()))
                .map(this::toResponse)
                .toList();
    }
    private WeatherAlertResponse toResponse(WeatherAlert w) {
        return new WeatherAlertResponse(
                w.getId(), w.getLocation().getId(), w.getLocation().getName(),
                w.getAlertType(), w.getSeverity(), w.getDescription(),
                w.getTemperatureC(), w.getRainfallMm()
        );
    }
}