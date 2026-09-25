package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

/**
 * All fields optional; only non-null values are applied. A non-null (possibly
 * empty) {@code assigneeIds} / {@code labelIds} replaces the corresponding set.
 */
public record TaskUpdateRequest(
        @Size(max = 150) String title,
        @Size(max = 2000) String description,
        TaskStatus status,
        @Min(1) @Max(5) Integer priority,
        LocalDate dueDate,
        Set<String> assigneeIds,
        Set<String> labelIds) {
}
