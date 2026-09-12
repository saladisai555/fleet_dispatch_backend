package com.logistics.service.impl;

import com.logistics.dto.request.ManifestItemRequest;
import com.logistics.dto.response.ManifestItemResponse;
import com.logistics.dto.request.ManifestItemStatusUpdateRequest;
import com.logistics.entity.DeliveryOrder;
import com.logistics.entity.Manifest;
import com.logistics.entity.ManifestItem;
import com.logistics.entity.enums.ManifestItemStatus;
import com.logistics.entity.enums.ManifestStatus;
import com.logistics.entity.enums.OrderStatus;
import com.logistics.exception.BusinessRuleViolationException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.ManifestItemRepository;
import com.logistics.service.DeliveryOrderService;
import com.logistics.service.ManifestItemService;
import com.logistics.service.ManifestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManifestItemServiceImpl implements ManifestItemService {

    private static final List<ManifestStatus> ACTIVE_MANIFEST_STATUSES =
            List.of(ManifestStatus.PLANNED, ManifestStatus.DISPATCHED, ManifestStatus.IN_TRANSIT);

    private final ManifestItemRepository manifestItemRepository;
    private final ManifestService manifestService;
    private final DeliveryOrderService deliveryOrderService;

    @Override
    @Transactional
    public ManifestItemResponse addItem(Long manifestId, ManifestItemRequest request) {
        Manifest manifest = manifestService.findEntity(manifestId);
        DeliveryOrder order = deliveryOrderService.findEntity(request.orderId());

        boolean alreadyActiveElsewhere = manifestItemRepository.findByOrderId(order.getId()).stream()
                .anyMatch(item -> ACTIVE_MANIFEST_STATUSES.contains(item.getManifest().getStatus())
                        && !item.getManifest().getId().equals(manifestId));

        if (alreadyActiveElsewhere) {
            throw new BusinessRuleViolationException(
                    "Order " + order.getOrderNumber() + " is already active on another manifest. "
                            + "Use the dispatch/re-dispatch workflow to reassign it.");
        }

        ManifestItem item = ManifestItem.builder()
                .manifest(manifest)
                .order(order)
                .sequenceNumber(request.sequenceNumber())
                .plannedArrivalTime(request.plannedArrivalTime())
                .status(ManifestItemStatus.PLANNED)
                .build();

        ManifestItem saved = manifestItemRepository.save(item);
        order.setStatus(OrderStatus.ASSIGNED);

        return toResponse(saved);
    }

    @Override
    public List<ManifestItemResponse> getByManifestId(Long manifestId) {
        return manifestItemRepository.findByManifestIdOrderBySequenceNumberAsc(manifestId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ManifestItemResponse updateStatus(Long itemId, ManifestItemStatusUpdateRequest request) {
        ManifestItem item = findEntity(itemId);
        item.setStatus(request.status());

        if (request.status() == ManifestItemStatus.DELIVERED) {
            item.setActualArrivalTime(LocalDateTime.now());
            item.getOrder().setStatus(OrderStatus.DELIVERED);
        } else if (request.status() == ManifestItemStatus.FAILED) {
            item.getOrder().setStatus(OrderStatus.DELAYED);
        } else if (request.status() == ManifestItemStatus.IN_TRANSIT) {
            item.getOrder().setStatus(OrderStatus.IN_TRANSIT);
        }

        return toResponse(item);
    }

    @Override
    public ManifestItem findEntity(Long id) {
        return manifestItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manifest item not found: " + id));
    }

    private ManifestItemResponse toResponse(ManifestItem i) {
        return new ManifestItemResponse(
                i.getId(), i.getManifest().getId(), i.getOrder().getId(), i.getOrder().getOrderNumber(),
                i.getSequenceNumber(), i.getPlannedArrivalTime(), i.getActualArrivalTime(), i.getStatus()
        );
    }
}