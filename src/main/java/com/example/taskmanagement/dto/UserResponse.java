package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.Role;
import com.example.taskmanagement.model.User;
import java.time.Instant;
import java.util.Set;

public record UserResponse(
        String id,
        String username,
        String email,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
