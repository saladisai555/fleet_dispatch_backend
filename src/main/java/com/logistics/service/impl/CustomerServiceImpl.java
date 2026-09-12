package com.logistics.service.impl;

import com.logistics.dto.request.CustomerRequest;
import com.logistics.dto.response.CustomerResponse;
import com.logistics.entity.Customer;
import com.logistics.entity.Location;
import com.logistics.exception.DuplicateResourceException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.CustomerRepository;
import com.logistics.service.CustomerService;
import com.logistics.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final LocationService locationService;

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByCustomerCode(request.customerCode())) {
            throw new DuplicateResourceException("Customer code already exists: " + request.customerCode());
        }

        Location location = locationService.findEntity(request.locationId());

        Customer customer = Customer.builder()
                .customerCode(request.customerCode())
                .name(request.name())
                .contactName(request.contactName())
                .phone(request.phone())
                .email(request.email())
                .location(location)
                .priorityLevel(request.priorityLevel())
                .requiresTemperatureControl(request.requiresTemperatureControl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(customerRepository.save(customer));
    }

    // Added to CustomerServiceImpl
    @Override
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findEntity(id);

        // customerCode uniqueness re-checked only if it actually changed
        if (!customer.getCustomerCode().equals(request.customerCode())
                && customerRepository.existsByCustomerCode(request.customerCode())) {
            throw new DuplicateResourceException("Customer code already exists: " + request.customerCode());
        }

        Location location = locationService.findEntity(request.locationId());

        customer.setCustomerCode(request.customerCode());
        customer.setName(request.name());
        customer.setContactName(request.contactName());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setLocation(location);
        customer.setPriorityLevel(request.priorityLevel());
        customer.setRequiresTemperatureControl(request.requiresTemperatureControl());
        customer.setUpdatedAt(LocalDateTime.now());

        return toResponse(customer);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = findEntity(id);
        // customers.location_id is RESTRICT and delivery_orders.customer_id is
        // RESTRICT - deleting a customer with any orders will throw a DB
        // constraint violation, which Step 11's global handler will map to 409
        // CONFLICT rather than a raw 500.
        customerRepository.delete(customer);
    }

    @Override
    public List<CustomerResponse> search(String priority, String search, Pageable pageable) {
        // Kept as a simple in-memory filter over findAll() for now, consistent
        // with the "don't over-engineer" rule - a Specification-based query
        // would only be worth the complexity at a much larger data scale.
        return customerRepository.findAll().stream()
                .filter(c -> priority == null || c.getPriorityLevel().name().equalsIgnoreCase(priority))
                .filter(c -> search == null || c.getName().toLowerCase().contains(search.toLowerCase())
                        || c.getCustomerCode().toLowerCase().contains(search.toLowerCase()))
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CustomerResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public Customer findEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getCustomerCode(), c.getName(), c.getContactName(), c.getPhone(), c.getEmail(),
                c.getLocation().getId(), c.getLocation().getName(), c.getPriorityLevel(),
                c.getRequiresTemperatureControl(), c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}