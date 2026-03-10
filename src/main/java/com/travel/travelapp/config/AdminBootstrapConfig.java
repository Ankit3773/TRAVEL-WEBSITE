package com.travel.travelapp.config;

import com.travel.travelapp.entity.AppUser;
import com.travel.travelapp.entity.UserRole;
import com.travel.travelapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner adminBootstrap(
            @Value("${app.admin.email}") String adminEmail, @Value("${app.admin.password}") String adminPassword) {
        return args -> {
            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                return;
            }

            if (userRepository.existsByEmailIgnoreCase(adminEmail.trim())) {
                return;
            }

            AppUser admin = new AppUser();
            admin.setName("Admin");
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
        };
    }
}
