package com.travel.travelapp.controller;

import com.travel.travelapp.dto.AdminSetupStatusResponse;
import com.travel.travelapp.dto.AuthResponse;
import com.travel.travelapp.dto.FirstAdminSetupRequest;
import com.travel.travelapp.dto.ForgotPasswordRequest;
import com.travel.travelapp.dto.ForgotPasswordResponse;
import com.travel.travelapp.dto.GoogleAuthRequest;
import com.travel.travelapp.dto.LoginRequest;
import com.travel.travelapp.dto.MessageResponse;
import com.travel.travelapp.dto.RegisterRequest;
import com.travel.travelapp.dto.ResetPasswordRequest;
import com.travel.travelapp.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.registerCustomer(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/admin-setup-status")
    public AdminSetupStatusResponse adminSetupStatus() {
        return authService.getAdminSetupStatus();
    }

    @PostMapping("/setup-first-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse setupFirstAdmin(@Valid @RequestBody FirstAdminSetupRequest request) {
        return authService.createFirstAdmin(request);
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleAuthRequest request) {
        return authService.authenticateWithGoogle(request.getCredential());
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
