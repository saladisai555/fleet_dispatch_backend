package com.logistics.service.impl;

import com.logistics.dto.AgentEventCreateRequest;
import com.logistics.dto.request.WeatherAlertRequest;
import com.logistics.dto.response.WeatherAlertResponse;
import com.logistics.entity.Location;
import com.logistics.entity.WeatherAlert;
import com.logistics.entity.enums.AgentEntityType;
import com.logistics.entity.enums.AgentEventStatus;
import com.logistics.entity.enums.AgentType;
import com.logistics.entity.enums.EventSeverity;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.WeatherAlertRepository;
import com.logistics.service.AgentEventService;
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
    private final AgentEventService agentEventService;
    // Added dependency: AgentEventService
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

        WeatherAlert saved = weatherAlertRepository.save(alert);

        // severity is a plain String on this entity (no DB enum constraint - see
        // Step 5 note), so it's mapped defensively rather than assumed to match
        // an EventSeverity constant exactly.
        agentEventService.log(new AgentEventCreateRequest(
                AgentType.LISTENER, "WEATHER_ALERT_DETECTED", mapSeverityString(saved.getSeverity()),
                AgentEntityType.INCIDENT, saved.getId(), // AgentEntityType has no WEATHER_ALERT constant - see note below
                "Weather alert at " + location.getName() + ": " + saved.getAlertType(),
                null, null, AgentEventStatus.COMPLETED
        ));

        return toResponse(saved);
    }

    private EventSeverity mapSeverityString(String severity) {
        try {
            return EventSeverity.valueOf(severity.toUpperCase());
        } catch (Exception e) {
            return EventSeverity.INFO; // unrecognized severity string - fail safe, not fail loud
        }
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