package com.travel.travelapp.config;

import com.travel.travelapp.entity.AppUser;
import com.travel.travelapp.entity.UserRole;
import com.travel.travelapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalCustomerBootstrapConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner localCustomerBootstrap(
            @Value("${app.customer.bootstrap.enabled:true}") boolean customerBootstrapEnabled,
            @Value("${app.customer.name:Ankit Kumar}") String customerName,
            @Value("${app.customer.email:ankit@narayantravels.local}") String customerEmail,
            @Value("${app.customer.password:Customer123!}") String customerPassword) {
        return args -> {
            if (!customerBootstrapEnabled
                    || customerName == null || customerName.isBlank()
                    || customerEmail == null || customerEmail.isBlank()
                    || customerPassword == null || customerPassword.isBlank()) {
                return;
            }

            String normalizedEmail = customerEmail.trim().toLowerCase();
            AppUser customer = userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .orElseGet(AppUser::new);

            customer.setName(customerName.trim());
            customer.setEmail(normalizedEmail);
            customer.setPasswordHash(passwordEncoder.encode(customerPassword));
            customer.setRole(UserRole.CUSTOMER);
            customer.setActive(true);

            userRepository.save(customer);
        };
    }
}
