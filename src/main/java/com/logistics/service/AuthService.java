package com.logistics.service;

import com.logistics.dto.request.LoginRequest;
import com.logistics.dto.response.LoginResponse;
import com.logistics.dto.response.UserResponse;
import org.springframework.security.core.Authentication;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    UserResponse getCurrentUser(Authentication authentication);
}