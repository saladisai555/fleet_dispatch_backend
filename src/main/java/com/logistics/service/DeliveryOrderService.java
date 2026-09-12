package com.logistics.service;

import com.logistics.dto.request.DeliveryOrderRequest;
import com.logistics.dto.response.DeliveryOrderResponse;
import com.logistics.dto.request.DeliveryOrderStatusUpdateRequest;
import com.logistics.entity.DeliveryOrder;
import com.logistics.entity.enums.OrderPriority;
import com.logistics.entity.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface DeliveryOrderService {
    DeliveryOrderResponse create(DeliveryOrderRequest request);
    DeliveryOrderResponse getById(Long id);
    List<DeliveryOrderResponse> getByStatus(OrderStatus status);
    List<DeliveryOrderResponse> getOverdueOrders();
    DeliveryOrderResponse updateStatus(Long id, DeliveryOrderStatusUpdateRequest request);
    DeliveryOrder findEntity(Long id);
    // Added to DeliveryOrderService interface
    DeliveryOrderResponse update(Long id, DeliveryOrderRequest request);
    List<DeliveryOrderResponse> search(OrderStatus status, OrderPriority priority, Long customerId,
                                       LocalDateTime windowStart, LocalDateTime windowEnd);
}