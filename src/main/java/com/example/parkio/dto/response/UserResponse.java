package com.example.parkio.dto.response;

import com.example.parkio.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean enabled,
        Set<String> roles,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
