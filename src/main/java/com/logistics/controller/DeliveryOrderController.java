package com.logistics.controller;

import com.logistics.dto.*;
import com.logistics.dto.request.DeliveryOrderRequest;
import com.logistics.dto.request.DeliveryOrderStatusUpdateRequest;
import com.logistics.dto.response.DeliveryOrderResponse;
import com.logistics.entity.enums.OrderPriority;
import com.logistics.entity.enums.OrderStatus;
import com.logistics.service.DeliveryOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;

    @GetMapping
    public ResponseEntity<List<DeliveryOrderResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderPriority priority,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) LocalDateTime deliveryWindowStart,
            @RequestParam(required = false) LocalDateTime deliveryWindowEnd) {
        return ResponseEntity.ok(deliveryOrderService.search(status, priority, customerId, deliveryWindowStart, deliveryWindowEnd));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<DeliveryOrderResponse> getById(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryOrderService.getById(orderId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<DeliveryOrderResponse> create(@Valid @RequestBody DeliveryOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryOrderService.create(request));
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<DeliveryOrderResponse> update(@PathVariable Long orderId,
                                                        @Valid @RequestBody DeliveryOrderRequest request) {
        return ResponseEntity.ok(deliveryOrderService.update(orderId, request));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<DeliveryOrderResponse> updateStatus(@PathVariable Long orderId,
                                                              @Valid @RequestBody DeliveryOrderStatusUpdateRequest request) {
        return ResponseEntity.ok(deliveryOrderService.updateStatus(orderId, request));
    }
}