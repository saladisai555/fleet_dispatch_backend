package com.logistics.service;

import com.logistics.dto.request.CustomerRequest;
import com.logistics.dto.response.CustomerResponse;
import com.logistics.entity.Customer;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {
    CustomerResponse create(CustomerRequest request);
    CustomerResponse getById(Long id);
    List<CustomerResponse> getAll();
    Customer findEntity(Long id);
    // Added to CustomerService interface
    CustomerResponse update(Long id, CustomerRequest request);
    void delete(Long id);
    List<CustomerResponse> search(String priority, String search, Pageable pageable); // backs page/size/priority/search
}