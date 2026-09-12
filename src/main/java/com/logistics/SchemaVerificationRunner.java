package com.logistics;

import com.logistics.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SchemaVerificationRunner implements CommandLineRunner {

    private final LocationRepository locationRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final ManifestRepository manifestRepository;

    public SchemaVerificationRunner(LocationRepository locationRepository,
                                    VehicleRepository vehicleRepository,
                                    DriverRepository driverRepository,
                                    DeliveryOrderRepository deliveryOrderRepository,
                                    ManifestRepository manifestRepository) {
        this.locationRepository = locationRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.manifestRepository = manifestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) {

        System.out.println("=== SCHEMA VERIFICATION START ===");

        // ---------------------------------------------------------
        // 1. Verify entity -> table mapping and row availability
        // ---------------------------------------------------------
        System.out.println("Locations: " + locationRepository.count());
        System.out.println("Vehicles: " + vehicleRepository.count());
        System.out.println("Drivers: " + driverRepository.count());
        System.out.println("Delivery Orders: " + deliveryOrderRepository.count());
        System.out.println("Manifests: " + manifestRepository.count());


        // ---------------------------------------------------------
        // 2. Verify Vehicle -> Location relationship
        // ---------------------------------------------------------
        vehicleRepository.findAll()
                .stream()
                .findFirst()
                .ifPresent(v -> {

                    System.out.println(
                            "Sample vehicle: " + v.getVehicleCode()
                                    + " | capacity=" + v.getCapacityKg()
                                    + " | location="
                                    + (v.getCurrentLocation() != null
                                    ? v.getCurrentLocation().getName()
                                    : "null")
                    );
                });


        System.out.println("=== SCHEMA VERIFICATION END ===");
    }
}