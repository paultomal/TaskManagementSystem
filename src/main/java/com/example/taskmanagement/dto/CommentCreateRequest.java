package com.example.taskmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param parentId optional id of the comment being replied to; null for a
 *                 top-level comment.
 */
public record CommentCreateRequest(
        @NotBlank @Size(max = 2000) String content,
        String parentId) {
}
