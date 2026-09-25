package com.example.taskmanagement.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Custom Envers revision entity. Supplies the revision number and timestamp
 * (like the built-in {@code DefaultRevisionEntity}, which is {@code final} and
 * so cannot be extended) and adds the username of whoever made the change, so
 * the audit trail records <em>who</em> changed what and when.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(RevisionListenerImpl.class)
@Getter
@Setter
public class RevisionMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfoSeq")
    @SequenceGenerator(name = "revinfoSeq", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    private int id;

    @RevisionTimestamp
    private long timestamp;

    @Column(name = "username", length = 50)
    private String username;
}
