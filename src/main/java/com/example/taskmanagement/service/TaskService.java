package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.TaskCreateRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.dto.TaskUpdateRequest;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.model.Label;
import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.model.TaskStatus;
import com.example.taskmanagement.model.User;
import com.example.taskmanagement.repository.LabelRepository;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.CustomUserDetails;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository,
                       LabelRepository labelRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.labelRepository = labelRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "taskLists", key = "T(java.util.Objects).hash(#status, #assigneeId)")
    public List<TaskResponse> findAll(TaskStatus status, String assigneeId) {
        List<Task> tasks;
        if (status != null) {
            tasks = taskRepository.findByStatus(status);
        } else if (assigneeId != null) {
            tasks = taskRepository.findByAssignees_Id(assigneeId);
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
                .assignees(resolveAssignees(request.assigneeIds()))
                .owner(currentUser())
                .labels(resolveLabels(request.labelIds()))
                .build();
        Task saved = taskRepository.saveAndFlush(task);
        log.info("Task created: [{}] '{}' by '{}'", saved.getId(), saved.getTitle(),
                saved.getOwner() != null ? saved.getOwner().getUsername() : "?");
        return TaskResponse.from(saved);
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
        if (request.assigneeIds() != null) {
            task.setAssignees(resolveAssignees(request.assigneeIds()));
        }
        if (request.labelIds() != null) {
            task.setLabels(resolveLabels(request.labelIds()));
        }

        Task saved = taskRepository.saveAndFlush(task);
        log.info("Task updated: [{}] '{}' status={}", saved.getId(), saved.getTitle(), saved.getStatus());
        return TaskResponse.from(saved);
    }

    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#id"),
            @CacheEvict(value = "taskLists", allEntries = true)
    })
    public void delete(String id) {
        Task task = getTaskOrThrow(id);
        checkOwnership(task);
        taskRepository.delete(task);
        log.info("Task deleted: [{}] '{}'", id, task.getTitle());
    }

    private Task getTaskOrThrow(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private Set<User> resolveAssignees(Set<String> assigneeIds) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<User> assignees = new LinkedHashSet<>();
        for (String assigneeId : assigneeIds) {
            assignees.add(userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + assigneeId)));
        }
        return assignees;
    }

    private Set<Label> resolveLabels(Set<String> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Label> labels = new LinkedHashSet<>();
        for (String labelId : labelIds) {
            labels.add(labelRepository.findById(labelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Label not found with id: " + labelId)));
        }
        return labels;
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
