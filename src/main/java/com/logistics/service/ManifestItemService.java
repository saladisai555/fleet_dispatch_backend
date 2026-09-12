package com.logistics.service;

import com.logistics.dto.request.ManifestItemRequest;
import com.logistics.dto.response.ManifestItemResponse;
import com.logistics.dto.request.ManifestItemStatusUpdateRequest;
import com.logistics.entity.ManifestItem;

import java.util.List;

public interface ManifestItemService {
    ManifestItemResponse addItem(Long manifestId, ManifestItemRequest request);
    List<ManifestItemResponse> getByManifestId(Long manifestId);
    ManifestItemResponse updateStatus(Long itemId, ManifestItemStatusUpdateRequest request);
    ManifestItem findEntity(Long id);
}