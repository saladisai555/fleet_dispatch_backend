package com.logistics.service;

import com.logistics.dto.request.UserCreateRequest;
import com.logistics.dto.response.UserResponse;
import com.logistics.entity.User;

public interface UserService {
    UserResponse create(UserCreateRequest request);
    UserResponse getById(Long id);
    User findEntity(Long id);
    User findByEmail(String email);
}