package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        TaskStatus status,
        @Min(1) @Max(5) Integer priority,
        LocalDate dueDate,
        String assigneeId) {
}
