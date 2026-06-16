package com.finaxys.skillsrh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private Collaborator collaborator;

    @Column(name = "evaluation_name", length = 255, nullable = false)
    private String evaluationName;

    @Convert(converter = StatusConverter.class)
    @Column(length = 50, nullable = false)
    private Status status; // en attente | en cours | complété

    @Column(name = "date_assigned", nullable = false)
    private LocalDate dateAssigned;

    @Column(name = "date_planned")
    private LocalDate datePlanned;

    @Column(name = "date_validated")
    private LocalDate dateValidated;

    @Column(name = "level_declared", length = 100)
    private String levelDeclared;

    @Column(name = "level_validated", length = 100)
    private String levelValidated;

    @Column(length = 20)
    private String score;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String details = "";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Evaluation() {
    }

    public Evaluation(Collaborator collaborator, String evaluationName, Status status, LocalDate dateAssigned) {
        this.collaborator = collaborator;
        this.evaluationName = evaluationName;
        this.status = status;
        this.dateAssigned = dateAssigned;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Collaborator getCollaborator() {
        return collaborator;
    }

    public String getEvaluationName() {
        return evaluationName;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getDateAssigned() {
        return dateAssigned;
    }

    public LocalDate getDatePlanned() {
        return datePlanned;
    }

    public LocalDate getDateValidated() {
        return dateValidated;
    }

    public String getLevelDeclared() {
        return levelDeclared;
    }

    public String getLevelValidated() {
        return levelValidated;
    }

    public String getScore() {
        return score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setDatePlanned(LocalDate datePlanned) {
        this.datePlanned = datePlanned;
    }

    public void setDateValidated(LocalDate dateValidated) {
        this.dateValidated = dateValidated;
    }

    public void setLevelDeclared(String levelDeclared) {
        this.levelDeclared = levelDeclared;
    }

    public void setLevelValidated(String levelValidated) {
        this.levelValidated = levelValidated;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}

