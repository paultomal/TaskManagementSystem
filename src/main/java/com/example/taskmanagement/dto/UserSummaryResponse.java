package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.User;

/**
 * Lightweight user reference (id + username) for embedding in other responses,
 * e.g. a task's assignee list.
 */
public record UserSummaryResponse(String id, String username) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getUsername());
    }
}
