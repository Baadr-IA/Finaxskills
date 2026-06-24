package com.finaxys.skillsrh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "test_collab",
    uniqueConstraints = @UniqueConstraint(name = "uk_test_collab_evaluation_collaborator", columnNames = {"evaluation_id", "collaborator_id"}),
    indexes = {
        @Index(name = "idx_test_collab_collaborator", columnList = "collaborator_id"),
        @Index(name = "idx_test_collab_assigned_at", columnList = "assigned_at"),
        @Index(name = "idx_test_collab_status", columnList = "status")
    }
)
public class TestCollab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private Collaborator collaborator;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Status status;

    @Column(name = "progress", nullable = false)
    private Integer progress;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TestCollab() {
    }

    public TestCollab(Evaluation evaluation, Collaborator collaborator, Instant assignedAt, Status status, Integer progress, LocalDate dueDate) {
        this.evaluation = evaluation;
        this.collaborator = collaborator;
        this.assignedAt = assignedAt != null ? assignedAt : Instant.now();
        this.status = status;
        this.progress = progress != null ? progress : 0;
        this.dueDate = dueDate;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public Collaborator getCollaborator() {
        return collaborator;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getProgress() {
        return progress;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (assignedAt == null) {
            assignedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (progress == null) {
            progress = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
