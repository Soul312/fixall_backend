package com.fixall.backend.controller;

import com.fixall.backend.dto.request.*;
import com.fixall.backend.dto.response.AuthResponse;
import com.fixall.backend.model.User;
import com.fixall.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        User user  = authService.register(request);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(
            new AuthResponse(token, user.getId(), user.getRole().name()));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        User user  = authService.authenticate(request);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(
            new AuthResponse(token, user.getId(), user.getRole().name()));
    }

    // GET /api/auth/me   — requires valid JWT
    @GetMapping("/me")
    public ResponseEntity<User> me(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(currentUser);
    }
}
