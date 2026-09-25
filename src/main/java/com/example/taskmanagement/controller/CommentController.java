package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.CommentCreateRequest;
import com.example.taskmanagement.dto.CommentResponse;
import com.example.taskmanagement.service.CommentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task comments / activity thread. Comments are nested under a task for listing
 * and creation; deletion is by comment id (author or ADMIN only).
 */
@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public List<CommentResponse> getForTask(@PathVariable String taskId) {
        return commentService.findByTask(taskId);
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<CommentResponse> add(@PathVariable String taskId,
                                               @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse created = commentService.add(taskId, request);
        return ResponseEntity.created(URI.create("/api/comments/" + created.id())).body(created);
    }

    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
