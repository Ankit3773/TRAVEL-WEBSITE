package com.travel.travelapp.service;

import com.travel.travelapp.dto.AuthResponse;
import com.travel.travelapp.dto.ForgotPasswordRequest;
import com.travel.travelapp.dto.ForgotPasswordResponse;
import com.travel.travelapp.dto.LoginRequest;
import com.travel.travelapp.dto.MessageResponse;
import com.travel.travelapp.dto.RegisterRequest;
import com.travel.travelapp.dto.ResetPasswordRequest;
import com.travel.travelapp.entity.AppUser;
import com.travel.travelapp.entity.UserRole;
import com.travel.travelapp.exception.BadRequestException;
import com.travel.travelapp.exception.ResourceNotFoundException;
import com.travel.travelapp.repository.UserRepository;
import com.travel.travelapp.security.JwtService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        AppUser user = new AppUser();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);

        AppUser saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail().trim().toLowerCase(), request.getPassword()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        AppUser user = userRepository.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String token = null;

        AppUser user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user != null) {
            token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
            userRepository.save(user);
            log.info("Password reset token generated for {}: {}", normalizedEmail, token);
        }

        return ForgotPasswordResponse.builder()
                .message("If that email is registered, password reset instructions have been generated.")
                .resetToken(token)
                .build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        AppUser user = userRepository.findByPasswordResetToken(request.getToken().trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);

        return MessageResponse.builder().message("Password reset successful").build();
    }

    public AppUser getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
