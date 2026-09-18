package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.model.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        String id,
        String title,
        String description,
        TaskStatus status,
        int priority,
        LocalDate dueDate,
        String assigneeId,
        String assigneeUsername,
        String ownerUsername,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getUsername() : null,
                task.getOwner() != null ? task.getOwner().getUsername() : null,
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
