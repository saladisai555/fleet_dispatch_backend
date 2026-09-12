package com.logistics.service.impl;

import com.logistics.dto.request.DeliveryOrderRequest;
import com.logistics.dto.response.DeliveryOrderResponse;
import com.logistics.dto.request.DeliveryOrderStatusUpdateRequest;
import com.logistics.entity.Customer;
import com.logistics.entity.DeliveryOrder;
import com.logistics.entity.Location;
import com.logistics.entity.enums.OrderPriority;
import com.logistics.entity.enums.OrderStatus;
import com.logistics.exception.BusinessRuleViolationException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.DeliveryOrderRepository;
import com.logistics.service.CustomerService;
import com.logistics.service.DeliveryOrderService;
import com.logistics.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final CustomerService customerService;
    private final LocationService locationService;

    @Override
    @Transactional
    public DeliveryOrderResponse create(DeliveryOrderRequest request) {

        if (request.pickupLocationId().equals(request.deliveryLocationId())) {
            throw new BusinessRuleViolationException("Pickup and delivery locations must differ");
        }

        if (!request.deliveryWindowEnd().isAfter(request.deliveryWindowStart())) {
            throw new BusinessRuleViolationException("deliveryWindowEnd must be after deliveryWindowStart");
        }

        boolean refrigerated = Boolean.TRUE.equals(request.requiresRefrigeration());
        boolean hasMin = request.requiredTemperatureMinC() != null;
        boolean hasMax = request.requiredTemperatureMaxC() != null;
        if (refrigerated) {
            if (!hasMin || !hasMax) {
                throw new BusinessRuleViolationException(
                        "requiresRefrigeration=true requires both requiredTemperatureMinC and requiredTemperatureMaxC");
            }
            if (request.requiredTemperatureMaxC().compareTo(request.requiredTemperatureMinC()) <= 0) {
                throw new BusinessRuleViolationException("requiredTemperatureMaxC must be greater than requiredTemperatureMinC");
            }
        } else if (hasMin || hasMax) {
            throw new BusinessRuleViolationException(
                    "Temperature fields must be null when requiresRefrigeration=false");
        }

        Customer customer = customerService.findEntity(request.customerId());
        Location pickup = locationService.findEntity(request.pickupLocationId());
        Location delivery = locationService.findEntity(request.deliveryLocationId());

        DeliveryOrder order = DeliveryOrder.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .pickupLocation(pickup)
                .deliveryLocation(delivery)
                .cargoDescription(request.cargoDescription())
                .weightKg(request.weightKg())
                .volumeM3(request.volumeM3())
                .priority(request.priority())
                .requiresRefrigeration(request.requiresRefrigeration())
                .requiredTemperatureMinC(request.requiredTemperatureMinC())
                .requiredTemperatureMaxC(request.requiredTemperatureMaxC())
                .deliveryWindowStart(request.deliveryWindowStart())
                .deliveryWindowEnd(request.deliveryWindowEnd())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(deliveryOrderRepository.save(order));
    }
    // Added to DeliveryOrderServiceImpl - reuses the same cross-field validation as create()
    @Override
    @Transactional
    public DeliveryOrderResponse update(Long id, DeliveryOrderRequest request) {
        DeliveryOrder order = findEntity(id);

        if (request.pickupLocationId().equals(request.deliveryLocationId())) {
            throw new BusinessRuleViolationException("Pickup and delivery locations must differ");
        }
        if (!request.deliveryWindowEnd().isAfter(request.deliveryWindowStart())) {
            throw new BusinessRuleViolationException("deliveryWindowEnd must be after deliveryWindowStart");
        }
        boolean refrigerated = Boolean.TRUE.equals(request.requiresRefrigeration());
        boolean hasMin = request.requiredTemperatureMinC() != null;
        boolean hasMax = request.requiredTemperatureMaxC() != null;
        if (refrigerated) {
            if (!hasMin || !hasMax) {
                throw new BusinessRuleViolationException(
                        "requiresRefrigeration=true requires both requiredTemperatureMinC and requiredTemperatureMaxC");
            }
            if (request.requiredTemperatureMaxC().compareTo(request.requiredTemperatureMinC()) <= 0) {
                throw new BusinessRuleViolationException("requiredTemperatureMaxC must be greater than requiredTemperatureMinC");
            }
        } else if (hasMin || hasMax) {
            throw new BusinessRuleViolationException("Temperature fields must be null when requiresRefrigeration=false");
        }

        Customer customer = customerService.findEntity(request.customerId());
        Location pickup = locationService.findEntity(request.pickupLocationId());
        Location delivery = locationService.findEntity(request.deliveryLocationId());

        order.setCustomer(customer);
        order.setPickupLocation(pickup);
        order.setDeliveryLocation(delivery);
        order.setCargoDescription(request.cargoDescription());
        order.setWeightKg(request.weightKg());
        order.setVolumeM3(request.volumeM3());
        order.setPriority(request.priority());
        order.setRequiresRefrigeration(request.requiresRefrigeration());
        order.setRequiredTemperatureMinC(request.requiredTemperatureMinC());
        order.setRequiredTemperatureMaxC(request.requiredTemperatureMaxC());
        order.setDeliveryWindowStart(request.deliveryWindowStart());
        order.setDeliveryWindowEnd(request.deliveryWindowEnd());
        order.setUpdatedAt(LocalDateTime.now());

        return toResponse(order);
    }

    @Override
    public List<DeliveryOrderResponse> search(OrderStatus status, OrderPriority priority, Long customerId,
                                              LocalDateTime windowStart, LocalDateTime windowEnd) {
        List<DeliveryOrder> base = status != null ? deliveryOrderRepository.findByStatus(status) : deliveryOrderRepository.findAll();
        return base.stream()
                .filter(o -> priority == null || o.getPriority() == priority)
                .filter(o -> customerId == null || o.getCustomer().getId().equals(customerId))
                .filter(o -> windowStart == null || !o.getDeliveryWindowStart().isBefore(windowStart))
                .filter(o -> windowEnd == null || !o.getDeliveryWindowEnd().isAfter(windowEnd))
                .map(this::toResponse)
                .toList();
    }
    @Override
    public DeliveryOrderResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<DeliveryOrderResponse> getByStatus(OrderStatus status) {
        return deliveryOrderRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Override
    public List<DeliveryOrderResponse> getOverdueOrders() {
        List<OrderStatus> nonTerminal = List.of(OrderStatus.PENDING, OrderStatus.ASSIGNED, OrderStatus.IN_TRANSIT);
        return deliveryOrderRepository.findByStatusInAndDeliveryWindowEndBefore(nonTerminal, LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DeliveryOrderResponse updateStatus(Long id, DeliveryOrderStatusUpdateRequest request) {
        DeliveryOrder order = findEntity(id);
        order.setStatus(request.status());
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(order);
    }

    @Override
    public DeliveryOrder findEntity(Long id) {
        return deliveryOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery order not found: " + id));
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    private DeliveryOrderResponse toResponse(DeliveryOrder o) {
        return new DeliveryOrderResponse(
                o.getId(), o.getOrderNumber(),
                o.getCustomer().getId(), o.getCustomer().getName(),
                o.getPickupLocation().getId(), o.getPickupLocation().getName(),
                o.getDeliveryLocation().getId(), o.getDeliveryLocation().getName(),
                o.getCargoDescription(), o.getWeightKg(), o.getVolumeM3(), o.getPriority(),
                o.getRequiresRefrigeration(), o.getRequiredTemperatureMinC(), o.getRequiredTemperatureMaxC(),
                o.getDeliveryWindowStart(), o.getDeliveryWindowEnd(), o.getStatus(),
                o.getCreatedAt(), o.getUpdatedAt()
        );
    }
}