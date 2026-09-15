package com.logistics.service.impl;

import com.logistics.dto.request.LoginRequest;
import com.logistics.dto.response.LoginResponse;
import com.logistics.dto.response.UserResponse;
import com.logistics.entity.User;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.security.CustomUserDetails;
import com.logistics.security.JwtService;
import com.logistics.service.AuthService;
import com.logistics.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return new LoginResponse(
                token, "Bearer", jwtService.getExpirationSeconds(),
                new LoginResponse.UserSummary(
                        userDetails.getId(), userDetails.getName(), userDetails.getEmail(), userDetails.getRole())
        );
    }

    @Override
    public UserResponse getCurrentUser(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userService.findEntity(userDetails.getId());
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}