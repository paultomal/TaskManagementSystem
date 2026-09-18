package com.example.taskmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload. Roles are intentionally NOT accepted here —
 * every self-registered account is created as USER. Elevating a user to ADMIN is
 * done by an admin via {@code PUT /api/users/{id}}.
 */
public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 6, max = 100) String password) {
}
