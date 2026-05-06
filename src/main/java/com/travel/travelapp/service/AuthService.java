package com.travel.travelapp.service;

import com.travel.travelapp.dto.AdminSetupStatusResponse;
import com.travel.travelapp.dto.AuthResponse;
import com.travel.travelapp.dto.FirstAdminSetupRequest;
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
import org.springframework.security.authentication.BadCredentialsException;
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
    private final JwtService jwtService;
    private final GoogleIdentityService googleIdentityService;

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
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        AppUser user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getActive())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    public AdminSetupStatusResponse getAdminSetupStatus() {
        boolean adminExists = userRepository.existsByRole(UserRole.ADMIN);
        return new AdminSetupStatusResponse(adminExists, !adminExists);
    }

    @Transactional
    public AuthResponse createFirstAdmin(FirstAdminSetupRequest request) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            throw new BadRequestException("Admin account already exists");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Email already registered");
        }

        AppUser admin = new AppUser();
        admin.setName(request.getName().trim());
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);

        AppUser saved = userRepository.save(admin);
        return buildAuthResponse(saved);
    }

    @Transactional
    public AuthResponse authenticateWithGoogle(String credential) {
        GoogleIdentityService.GoogleProfile profile = googleIdentityService.verifyCredential(credential);

        AppUser user = userRepository.findByEmailIgnoreCase(profile.email())
                .orElseGet(() -> {
                    AppUser newUser = new AppUser();
                    newUser.setName(resolveGoogleDisplayName(profile));
                    newUser.setEmail(profile.email());
                    newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setRole(UserRole.CUSTOMER);
                    newUser.setActive(true);
                    return userRepository.save(newUser);
                });

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadCredentialsException("Account is inactive");
        }

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

    private String resolveGoogleDisplayName(GoogleIdentityService.GoogleProfile profile) {
        if (profile.name() != null && !profile.name().isBlank()) {
            return profile.name().trim();
        }

        int atIndex = profile.email().indexOf('@');
        if (atIndex > 0) {
            return profile.email().substring(0, atIndex);
        }

        return "Google User";
    }
}
