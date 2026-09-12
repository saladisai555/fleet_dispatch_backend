package com.logistics.controller;

import com.logistics.dto.request.LoginRequest;
import com.logistics.dto.response.LoginResponse;
import com.logistics.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    // AuthService does not exist yet - it depends on JWT signing/validation
    // infrastructure that is built in Step 12. This controller compiles
    // against the eventual interface shape but is NOT functional until then.

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        throw new UnsupportedOperationException("Pending Step 12 - JWT authentication not yet wired");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser() {
        throw new UnsupportedOperationException("Pending Step 12 - JWT authentication not yet wired");
    }
}