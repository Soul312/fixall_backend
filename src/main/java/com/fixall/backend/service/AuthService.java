package com.fixall.backend.service;

import com.fixall.backend.dto.request.*;
import com.fixall.backend.exception.BadRequestException;
import com.fixall.backend.exception.ResourceNotFoundException;
import com.fixall.backend.model.User;
import com.fixall.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already in use");
        }
        User user = User.builder()
            .email(req.getEmail())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .fullName(req.getFullName())
            .phone(req.getPhone())
            .role(req.getRole())
            .build();
        return userRepository.save(user);
    }

    public User authenticate(LoginRequest req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        return userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User updateProfile(String userId, UpdateUserProfileRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        return userRepository.save(user);
    }

    public User updateFcmToken(String userId, String fcmToken) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFcmToken(fcmToken);
        return userRepository.save(user);
    }
}

