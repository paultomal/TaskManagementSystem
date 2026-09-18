package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * All fields optional; only non-null values are applied.
 */
public record UserUpdateRequest(
        @Email @Size(max = 120) String email,
        @Size(min = 6, max = 100) String password,
        Set<Role> roles) {
}
