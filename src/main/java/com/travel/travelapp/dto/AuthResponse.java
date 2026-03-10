package com.travel.travelapp.dto;

import com.travel.travelapp.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String name;
    private String email;
    private UserRole role;
}
