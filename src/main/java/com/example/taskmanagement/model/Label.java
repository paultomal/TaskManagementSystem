package com.example.taskmanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reusable tag that can be attached to many tasks (many-to-many). Labels are
 * shared across the system, so the same "bug" or "urgent" label is reused
 * rather than duplicated per task.
 */
@Entity
@Table(name = "labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label {

    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** Optional display color, e.g. a hex string like {@code #FF8800}. */
    @Column(length = 20)
    private String color;

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = "LABEL-" + UUID.randomUUID();
        }
    }
}
