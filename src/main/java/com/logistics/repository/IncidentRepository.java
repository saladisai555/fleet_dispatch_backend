package com.logistics.repository;

import com.logistics.entity.Incident;
import com.logistics.entity.enums.IncidentStatus;
import com.logistics.entity.enums.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Core to the Listener Agent: currently active incidents to monitor
    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByLocationIdAndStatus(Long locationId, IncidentStatus status);

    // Needed for the Diagnostic Agent when classifying a disruption type
    List<Incident> findByIncidentTypeAndStatus(IncidentType incidentType, IncidentStatus status);
    // Added to IncidentRepository
    List<Incident> findByIncidentTypeAndStartedAt(IncidentType incidentType, LocalDateTime startedAt);
}