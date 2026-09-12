package com.logistics.repository;

import com.logistics.entity.Route;
import com.logistics.entity.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    // Needed since a manifest may accumulate multiple route rows over time
    // (PLANNED -> ACTIVE -> REPLACED history, per the route-versioning question
    // flagged earlier) — this fetches the one currently in effect.
    Optional<Route> findByManifestIdAndStatus(Long manifestId, RouteStatus status);

    List<Route> findByManifestIdOrderByCreatedAtDesc(Long manifestId);
}