package com.finaxys.skillsrh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "test_collab_id", nullable = false)
    private TestCollab testCollab;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "position_order", nullable = false)
    private Integer positionOrder;

    protected Question() {
    }

    public Question(TestCollab testCollab, String questionText, Integer positionOrder) {
        this.testCollab = testCollab;
        this.questionText = questionText;
        this.positionOrder = positionOrder;
    }

    public Long getId() {
        return id;
    }

    public TestCollab getTestCollab() {
        return testCollab;
    }

    public String getQuestionText() {
        return questionText;
    }

    public Integer getPositionOrder() {
        return positionOrder;
    }
}

