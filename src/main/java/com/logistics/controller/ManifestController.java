package com.logistics.controller;

import com.logistics.dto.*;
import com.logistics.dto.request.ManifestItemRequest;
import com.logistics.dto.request.ManifestRequest;
import com.logistics.dto.response.ManifestItemResponse;
import com.logistics.dto.response.ManifestResponse;
import com.logistics.entity.enums.ManifestStatus;
import com.logistics.service.ManifestItemService;
import com.logistics.service.ManifestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manifests")
@RequiredArgsConstructor
public class ManifestController {

    private final ManifestService manifestService;
    private final ManifestItemService manifestItemService;

    @GetMapping
    public ResponseEntity<List<ManifestResponse>> list(
            @RequestParam(required = false) ManifestStatus status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId) {
        return ResponseEntity.ok(manifestService.search(status, vehicleId, driverId));
    }

    @GetMapping("/{manifestId}")
    public ResponseEntity<ManifestResponse> getById(@PathVariable Long manifestId) {
        return ResponseEntity.ok(manifestService.getById(manifestId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<ManifestResponse> create(@Valid @RequestBody ManifestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(manifestService.create(request));
    }

    @PutMapping("/{manifestId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<ManifestResponse> update(@PathVariable Long manifestId,
                                                   @Valid @RequestBody ManifestRequest request) {
        return ResponseEntity.ok(manifestService.update(manifestId, request));
    }

    @GetMapping("/{manifestId}/items")
    public ResponseEntity<List<ManifestItemResponse>> items(@PathVariable Long manifestId) {
        return ResponseEntity.ok(manifestItemService.getByManifestId(manifestId));
    }

    @PostMapping("/{manifestId}/items")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<ManifestItemResponse> addItem(@PathVariable Long manifestId,
                                                        @Valid @RequestBody ManifestItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(manifestItemService.addItem(manifestId, request));
    }
}