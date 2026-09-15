package com.logistics.controller;

import com.logistics.dto.response.ManifestItemResponse;
import com.logistics.dto.request.ManifestItemStatusUpdateRequest;
import com.logistics.service.ManifestItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manifest-items")
@RequiredArgsConstructor
public class ManifestItemController {

    private final ManifestItemService manifestItemService;

    @PatchMapping("/{itemId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<ManifestItemResponse> updateStatus(@PathVariable Long itemId,
                                                             @Valid @RequestBody ManifestItemStatusUpdateRequest request) {
        return ResponseEntity.ok(manifestItemService.updateStatus(itemId, request));
    }
}