package com.logistics.service;

import com.logistics.dto.request.WeatherAlertRequest;
import com.logistics.dto.response.WeatherAlertResponse;

import java.util.List;

public interface WeatherAlertService {
    WeatherAlertResponse create(WeatherAlertRequest request);
    WeatherAlertResponse getById(Long id);
    List<WeatherAlertResponse> getByLocation(Long locationId);
    List<WeatherAlertResponse> getBySeverity(String severity);
    // Added to WeatherAlertService interface
    List<WeatherAlertResponse> search(Long locationId, String severity, String alertType);
}