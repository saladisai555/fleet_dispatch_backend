package com.logistics.repository;

import com.logistics.entity.Location;
import com.logistics.entity.enums.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    // Needed for lookups like "all warehouses" or "all customer sites"
    List<Location> findByLocationType(LocationType locationType);

    // Needed for search/autocomplete-style lookups in the UI
    List<Location> findByCityIgnoreCase(String city);
}