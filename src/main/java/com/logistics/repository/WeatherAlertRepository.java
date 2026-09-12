package com.logistics.repository;

import com.logistics.entity.WeatherAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherAlertRepository extends JpaRepository<WeatherAlert, Long> {

    // Needed for the Listener Agent: current weather conditions at a given location
    List<WeatherAlert> findByLocationId(Long locationId);

    // severity kept as String per schema (no DB-level enum constraint found)
    List<WeatherAlert> findBySeverity(String severity);
}