package com.logistics.service.impl;

import com.logistics.dto.request.UserCreateRequest;
import com.logistics.dto.response.UserResponse;
import com.logistics.entity.User;
import com.logistics.entity.enums.UserStatus;
import com.logistics.exception.DuplicateResourceException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.repository.UserRepository;
import com.logistics.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }
}