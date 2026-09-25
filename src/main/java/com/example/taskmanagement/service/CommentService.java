package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.CommentCreateRequest;
import com.example.taskmanagement.dto.CommentResponse;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.model.Comment;
import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.model.User;
import com.example.taskmanagement.repository.CommentRepository;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.CustomUserDetails;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, TaskRepository taskRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findByTask(String taskId) {
        requireTask(taskId);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(CommentResponse::from).toList();
    }

    public CommentResponse add(String taskId, CommentCreateRequest request) {
        Task task = requireTask(taskId);

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent comment not found with id: " + request.parentId()));
            if (!parent.getTask().getId().equals(taskId)) {
                throw new IllegalArgumentException("Parent comment belongs to a different task");
            }
        }

        Comment comment = Comment.builder()
                .task(task)
                .author(currentUser())
                .parent(parent)
                .content(request.content())
                .build();
        Comment saved = commentRepository.saveAndFlush(comment);
        log.info("Comment added: [{}] on task [{}] by '{}'", saved.getId(), taskId,
                saved.getAuthor().getUsername());
        return CommentResponse.from(saved);
    }

    public void delete(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        checkAuthorOrAdmin(comment);
        commentRepository.delete(comment);
        log.info("Comment deleted: [{}]", commentId);
    }

    private Task requireTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    /** Allows the action only for an ADMIN or the comment's author. */
    private void checkAuthorOrAdmin(Comment comment) {
        CustomUserDetails current = currentUserDetails();
        if (hasAdminRole(current)) {
            return;
        }
        boolean isAuthor = comment.getAuthor() != null
                && comment.getAuthor().getId() != null
                && comment.getAuthor().getId().equals(current.getId());
        if (!isAuthor) {
            throw new AccessDeniedException("You are not allowed to delete this comment");
        }
    }

    private User currentUser() {
        return userRepository.findById(currentUserDetails().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }

    private CustomUserDetails currentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            throw new AccessDeniedException("No authenticated user");
        }
        return details;
    }

    private boolean hasAdminRole(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
