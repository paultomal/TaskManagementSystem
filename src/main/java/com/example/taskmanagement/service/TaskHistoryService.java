package com.example.taskmanagement.service;

import com.example.taskmanagement.audit.RevisionMetadata;
import com.example.taskmanagement.dto.TaskRevisionResponse;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a task's audit trail from the Envers revision tables.
 */
@Service
@Transactional(readOnly = true)
public class TaskHistoryService {

    private final EntityManager entityManager;
    private final TaskRepository taskRepository;

    public TaskHistoryService(EntityManager entityManager, TaskRepository taskRepository) {
        this.entityManager = entityManager;
        this.taskRepository = taskRepository;
    }

    public List<TaskRevisionResponse> findHistory(String taskId) {
        // A task with no live row may still have history (e.g. it was deleted),
        // so only 404 when there is neither a live row nor any revisions.
        AuditReader reader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Task.class, false, true)
                .add(AuditEntity.id().eq(taskId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        if (rows.isEmpty() && !taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }

        List<TaskRevisionResponse> history = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Task snapshot = (Task) row[0];
            RevisionMetadata meta = (RevisionMetadata) row[1];
            RevisionType type = (RevisionType) row[2];

            history.add(new TaskRevisionResponse(
                    meta.getId(),
                    type.name(),
                    meta.getUsername(),
                    Instant.ofEpochMilli(meta.getTimestamp()),
                    snapshot != null ? snapshot.getTitle() : null,
                    snapshot != null ? snapshot.getDescription() : null,
                    snapshot != null ? snapshot.getStatus() : null,
                    snapshot != null ? snapshot.getPriority() : 0,
                    snapshot != null ? snapshot.getDueDate() : null));
        }
        return history;
    }
}
