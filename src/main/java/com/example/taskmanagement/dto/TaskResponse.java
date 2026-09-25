package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.model.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record TaskResponse(
        String id,
        String title,
        String description,
        TaskStatus status,
        int priority,
        LocalDate dueDate,
        Set<UserSummaryResponse> assignees,
        String ownerUsername,
        Set<LabelResponse> labels,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        Set<LabelResponse> labels = task.getLabels() == null ? Set.of()
                : task.getLabels().stream().map(LabelResponse::from)
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<UserSummaryResponse> assignees = task.getAssignees() == null ? Set.of()
                : task.getAssignees().stream().map(UserSummaryResponse::from)
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                assignees,
                task.getOwner() != null ? task.getOwner().getUsername() : null,
                labels,
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
