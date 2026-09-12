package com.logistics.repository;

import com.logistics.entity.DeliveryOrder;
import com.logistics.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    Optional<DeliveryOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    // Needed for dashboards/queues: e.g. all PENDING orders awaiting assignment
    List<DeliveryOrder> findByStatus(OrderStatus status);

    List<DeliveryOrder> findByCustomerId(Long customerId);

    // Needed for the Diagnostic Agent / delay checks: orders whose delivery
    // window has already passed but are not yet DELIVERED/CANCELLED
    List<DeliveryOrder> findByStatusInAndDeliveryWindowEndBefore(
            List<OrderStatus> statuses, LocalDateTime now);
}