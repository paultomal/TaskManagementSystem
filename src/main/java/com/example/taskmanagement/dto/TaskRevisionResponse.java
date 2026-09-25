package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One entry in a task's audit trail: the task's state at a given revision, plus
 * who made the change, when, and what kind of change it was.
 *
 * @param revisionType ADD, MOD, or DEL
 */
public record TaskRevisionResponse(
        int revisionNumber,
        String revisionType,
        String changedBy,
        Instant changedAt,
        String title,
        String description,
        TaskStatus status,
        int priority,
        LocalDate dueDate) {
}
