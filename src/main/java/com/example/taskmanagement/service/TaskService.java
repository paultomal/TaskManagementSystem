package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.TaskCreateRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.dto.TaskUpdateRequest;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.model.TaskStatus;
import com.example.taskmanagement.model.User;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.CustomUserDetails;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "taskLists", key = "T(java.util.Objects).hash(#status, #assigneeId)")
    public List<TaskResponse> findAll(TaskStatus status, String assigneeId) {
        List<Task> tasks;
        if (status != null) {
            tasks = taskRepository.findByStatus(status);
        } else if (assigneeId != null) {
            tasks = taskRepository.findByAssigneeId(assigneeId);
        } else {
            tasks = taskRepository.findAll();
        }
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "#id")
    public TaskResponse findById(String id) {
        return TaskResponse.from(getTaskOrThrow(id));
    }

    @CacheEvict(value = "taskLists", allEntries = true)
    public TaskResponse create(TaskCreateRequest request) {
        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .priority(request.priority() != null ? request.priority() : 3)
                .dueDate(request.dueDate())
                .assignee(resolveAssignee(request.assigneeId()))
                .owner(currentUser())
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#id"),
            @CacheEvict(value = "taskLists", allEntries = true)
    })
    public TaskResponse update(String id, TaskUpdateRequest request) {
        Task task = getTaskOrThrow(id);
        checkOwnership(task);

        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.assigneeId() != null) {
            task.setAssignee(resolveAssignee(request.assigneeId()));
        }

        return TaskResponse.from(taskRepository.save(task));
    }

    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#id"),
            @CacheEvict(value = "taskLists", allEntries = true)
    })
    public void delete(String id) {
        Task task = getTaskOrThrow(id);
        checkOwnership(task);
        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private User resolveAssignee(String assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + assigneeId));
    }

    /**
     * Allows the action only for an ADMIN or the task's owner.
     */
    private void checkOwnership(Task task) {
        CustomUserDetails current = currentUserDetails();
        if (hasAdminRole(current)) {
            return;
        }
        boolean isOwner = task.getOwner() != null
                && task.getOwner().getId() != null
                && task.getOwner().getId().equals(current.getId());
        if (!isOwner) {
            throw new AccessDeniedException("You are not allowed to modify this task");
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
