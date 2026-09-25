package com.example.taskmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 20) String color) {
}
