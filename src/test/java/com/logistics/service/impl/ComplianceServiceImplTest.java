package com.logistics.service.impl;

import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.repository.ManifestItemRepository;
import com.logistics.repository.ManifestRepository;
import com.logistics.service.ComplianceService;
import com.logistics.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceImplTest {

    @Mock private com.logistics.repository.ComplianceCheckRepository complianceCheckRepository;
    @Mock private com.logistics.repository.DispatchChangeRequestRepository dispatchChangeRequestRepository;
    @Mock private ManifestRepository manifestRepository;
    @Mock private ManifestItemRepository manifestItemRepository;
    @Mock private IncidentService incidentService;

    @InjectMocks
    private ComplianceServiceImpl complianceService;

    private Manifest manifest;
    private Vehicle vehicle;
    private Driver driver;
    private DeliveryOrder order;
    private Location pickup;
    private Location delivery;

    @BeforeEach
    void setUp() {
        pickup = Location.builder().id(1L).name("Vijayawada Warehouse").build();
        delivery = Location.builder().id(2L).name("Guntur Hub").build();

        vehicle = Vehicle.builder()
                .id(10L).vehicleCode("VH-010").status("AVAILABLE")
                .capacityKg(new BigDecimal("1000")).capacityVolumeM3(new BigDecimal("20"))
                .refrigerated(false)
                .build();

        driver = Driver.builder()
                .id(20L).name("Ravi Kumar")
                .maxDrivingHours(new BigDecimal("11")).currentDrivingHours(new BigDecimal("5"))
                .build();

        Manifest m = Manifest.builder().id(30L).vehicle(vehicle).status(ManifestStatus.PLANNED).build();
        manifest = m;

        order = DeliveryOrder.builder()
                .id(40L)
                .weightKg(new BigDecimal("500")).volumeM3(new BigDecimal("10"))
                .pickupLocation(pickup).deliveryLocation(delivery)
                .requiresRefrigeration(false)
                .deliveryWindowStart(LocalDateTime.of(2026, 9, 15, 14, 0))
                .deliveryWindowEnd(LocalDateTime.of(2026, 9, 15, 17, 0))
                .build();

        ManifestItem item = ManifestItem.builder().manifest(manifest).order(order).sequenceNumber(1).build();
        when(manifestItemRepository.findByManifestIdOrderBySequenceNumberAsc(30L)).thenReturn(List.of(item));
        when(incidentService.hasCriticalActiveIncidentAtLocation(org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);
    }

    private ComplianceService.ComplianceEvaluationRequest baseRequest(LocalDateTime eta, BigDecimal additionalHours) {
        return new ComplianceService.ComplianceEvaluationRequest(
                manifest, vehicle, driver, order, additionalHours, eta,
                List.of(pickup.getId(), delivery.getId()));
    }

    // ===================== Vehicle capacity =====================

    @Test
    void allChecksPass_whenEverythingWithinLimits() {
        var request = baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE);
        var result = complianceService.evaluate(request);

        assertThat(result.overallPassed()).isTrue();
        assertThat(result.vehicleCapacityPassed()).isTrue();
        assertThat(result.driverHoursPassed()).isTrue();
        assertThat(result.deliveryWindowPassed()).isTrue();
        assertThat(result.vehicleTypePassed()).isTrue();
        assertThat(result.temperatureRequirementPassed()).isTrue();
        assertThat(result.routeSafetyPassed()).isTrue();
        assertThat(result.violations()).isEmpty();
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void capacityFails_whenManifestWeightExceedsVehicleCapacity() {
        order.setWeightKg(new BigDecimal("1500")); // exceeds vehicle's 1000kg

        var request = baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE);
        var result = complianceService.evaluate(request);

        assertThat(result.vehicleCapacityPassed()).isFalse();
        assertThat(result.overallPassed()).isFalse();
        assertThat(result.violations()).extracting("code").contains("VEHICLE_CAPACITY_EXCEEDED");
    }

    @Test
    void capacityFails_whenManifestVolumeExceedsVehicleCapacity() {
        order.setVolumeM3(new BigDecimal("25")); // exceeds vehicle's 20m3

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.vehicleCapacityPassed()).isFalse();
    }

    // ===================== Driver hours =====================

    @Test
    void driverHoursFails_whenProjectedExceedsMax() {
        // current 5h + proposed 7h = 12h > max 11h
        var result = complianceService.evaluate(
                baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), new BigDecimal("7")));

        assertThat(result.driverHoursPassed()).isFalse();
        assertThat(result.overallPassed()).isFalse();
        assertThat(result.violations()).extracting("code").contains("DRIVER_HOURS_EXCEEDED");
    }

    @Test
    void driverHoursPasses_whenExactlyAtMax() {
        // current 5h + proposed 6h = 11h == max 11h (boundary: <=, not <)
        var result = complianceService.evaluate(
                baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), new BigDecimal("6")));

        assertThat(result.driverHoursPassed()).isTrue();
    }

    // ===================== Delivery window =====================

    @Test
    void deliveryWindowFails_whenEtaAfterWindowEnd() {
        var result = complianceService.evaluate(
                baseRequest(LocalDateTime.of(2026, 9, 15, 18, 30), BigDecimal.ONE)); // window ends 17:00

        assertThat(result.deliveryWindowPassed()).isFalse();
        assertThat(result.violations()).extracting("code").contains("DELIVERY_WINDOW_VIOLATION");
    }

    @Test
    void deliveryWindowFails_whenEtaBeforeWindowStart() {
        var result = complianceService.evaluate(
                baseRequest(LocalDateTime.of(2026, 9, 15, 10, 0), BigDecimal.ONE)); // window starts 14:00

        assertThat(result.deliveryWindowPassed()).isFalse();
    }

    // ===================== Vehicle type / operational suitability =====================

    @Test
    void vehicleTypeFails_whenVehicleInBreakdown() {
        vehicle.setStatus("BREAKDOWN");

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.vehicleTypePassed()).isFalse();
        assertThat(result.violations()).extracting("code").contains("VEHICLE_TYPE_UNSUITABLE");
    }

    @Test
    void vehicleTypeFails_whenVehicleInTransit() {
        vehicle.setStatus("IN_TRANSIT");

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.vehicleTypePassed()).isFalse();
    }

    @Test
    void vehicleTypePasses_whenAssignedToTheSameAffectedManifest() {
        vehicle.setStatus("ASSIGNED");
        manifest.setVehicle(vehicle); // vehicle already belongs to this exact manifest

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.vehicleTypePassed()).isTrue();
    }

    @Test
    void vehicleTypeFails_whenAssignedToADifferentActiveManifest() {
        vehicle.setStatus("ASSIGNED");
        Manifest otherManifest = Manifest.builder().id(999L).vehicle(vehicle).status(ManifestStatus.PLANNED).build();
        manifest.setVehicle(Vehicle.builder().id(11L).build()); // affected manifest has a DIFFERENT vehicle
        when(manifestRepository.findByVehicleIdAndStatusIn(vehicle.getId(),
                List.of(ManifestStatus.PLANNED, ManifestStatus.DISPATCHED, ManifestStatus.IN_TRANSIT)))
                .thenReturn(List.of(otherManifest));

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.vehicleTypePassed()).isFalse();
    }

    // ===================== Temperature / cold-chain =====================

    @Test
    void temperaturePasses_whenOrderDoesNotRequireRefrigeration() {
        order.setRequiresRefrigeration(false);
        vehicle.setRefrigerated(false);

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.temperatureRequirementPassed()).isTrue();
    }

    @Test
    void temperatureFails_whenOrderNeedsRefrigerationButVehicleIsNotRefrigerated() {
        order.setRequiresRefrigeration(true);
        order.setRequiredTemperatureMinC(new BigDecimal("2"));
        order.setRequiredTemperatureMaxC(new BigDecimal("8"));
        vehicle.setRefrigerated(false);

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.temperatureRequirementPassed()).isFalse();
        assertThat(result.violations()).extracting("code").contains("TEMPERATURE_REQUIREMENT_NOT_MET");
    }

    @Test
    void temperatureFails_whenVehicleRangeDoesNotFullyCoverRequiredRange() {
        order.setRequiresRefrigeration(true);
        order.setRequiredTemperatureMinC(new BigDecimal("2"));
        order.setRequiredTemperatureMaxC(new BigDecimal("8"));
        vehicle.setRefrigerated(true);
        vehicle.setMinTemperatureC(new BigDecimal("4")); // narrower than required min of 2
        vehicle.setMaxTemperatureC(new BigDecimal("8"));

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.temperatureRequirementPassed()).isFalse();
    }

    @Test
    void temperaturePasses_whenVehicleRangeFullyCoversRequiredRange() {
        order.setRequiresRefrigeration(true);
        order.setRequiredTemperatureMinC(new BigDecimal("2"));
        order.setRequiredTemperatureMaxC(new BigDecimal("8"));
        vehicle.setRefrigerated(true);
        vehicle.setMinTemperatureC(new BigDecimal("0")); // wider than required - correctly covers it
        vehicle.setMaxTemperatureC(new BigDecimal("10"));

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.temperatureRequirementPassed()).isTrue();
    }

    // ===================== Route safety =====================

    @Test
    void routeSafetyFails_whenActiveCriticalIncidentAtRelevantLocation() {
        when(incidentService.hasCriticalActiveIncidentAtLocation(delivery.getId())).thenReturn(true);

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.routeSafetyPassed()).isFalse();
        assertThat(result.violations()).extracting("code").contains("ROUTE_SAFETY_VIOLATION");
    }

    // ===================== Overall AND logic (mirrors chk_compliance_overall) =====================

    @Test
    void overallFails_ifAnySingleCheckFails_evenIfAllOthersPass() {
        // Only driver hours fails; everything else passes.
        var result = complianceService.evaluate(
                baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), new BigDecimal("10")));

        assertThat(result.driverHoursPassed()).isFalse();
        assertThat(result.vehicleCapacityPassed()).isTrue();
        assertThat(result.deliveryWindowPassed()).isTrue();
        assertThat(result.vehicleTypePassed()).isTrue();
        assertThat(result.temperatureRequirementPassed()).isTrue();
        assertThat(result.routeSafetyPassed()).isTrue();
        assertThat(result.overallPassed()).isFalse(); // one failure is enough to fail overall
    }

    @Test
    void multipleFailures_areAllCapturedInViolationsList() {
        order.setWeightKg(new BigDecimal("2000")); // capacity fail
        vehicle.setStatus("BREAKDOWN");             // vehicle type fail

        var result = complianceService.evaluate(baseRequest(LocalDateTime.of(2026, 9, 15, 15, 0), BigDecimal.ONE));

        assertThat(result.violations()).hasSize(2);
        assertThat(result.violations()).extracting("code")
                .containsExactlyInAnyOrder("VEHICLE_CAPACITY_EXCEEDED", "VEHICLE_TYPE_UNSUITABLE");
    }
}