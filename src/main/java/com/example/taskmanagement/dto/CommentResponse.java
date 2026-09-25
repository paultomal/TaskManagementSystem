package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.Comment;
import java.time.Instant;

public record CommentResponse(
        String id,
        String taskId,
        String authorId,
        String authorUsername,
        String parentId,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask() != null ? comment.getTask().getId() : null,
                comment.getAuthor() != null ? comment.getAuthor().getId() : null,
                comment.getAuthor() != null ? comment.getAuthor().getUsername() : null,
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
