package com.finaxys.skillsrh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "collaborator_answer")
public class CollaboratorAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private Collaborator collaborator;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(optional = false)
    @JoinColumn(name = "answer_option_id", nullable = false)
    private AnswerOption answerOption;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    protected CollaboratorAnswer() {
    }

    public CollaboratorAnswer(Collaborator collaborator, Question question, AnswerOption answerOption, Instant answeredAt) {
        this.collaborator = collaborator;
        this.question = question;
        this.answerOption = answerOption;
        this.answeredAt = answeredAt;
    }

    public Long getId() {
        return id;
    }

    public Collaborator getCollaborator() {
        return collaborator;
    }

    public Question getQuestion() {
        return question;
    }

    public AnswerOption getAnswerOption() {
        return answerOption;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}

