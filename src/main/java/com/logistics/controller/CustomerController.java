package com.logistics.controller;

import com.logistics.dto.request.CustomerRequest;
import com.logistics.dto.response.CustomerResponse;
import com.logistics.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(customerService.search(priority, search, PageRequest.of(page, size)));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getById(customerId));
    }

    @PostMapping
    // @PreAuthorize annotations are written now to lock in the intended
    // authorization contract, but have no effect until Step 12 enables
    // method security - Spring Security's default auto-config currently
    // protects everything uniformly regardless of these annotations.
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<CustomerResponse> update(@PathVariable Long customerId,
                                                   @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.update(customerId, request));
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long customerId) {
        customerService.delete(customerId);
        return ResponseEntity.noContent().build();
    }
}