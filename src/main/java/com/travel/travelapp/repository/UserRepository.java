package com.travel.travelapp.repository;

import com.travel.travelapp.entity.AppUser;
import com.travel.travelapp.entity.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByPasswordResetToken(String passwordResetToken);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);
}
